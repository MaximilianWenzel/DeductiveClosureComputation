package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.NIO2ConnectionModel;
import networking.io.SocketManager;
import networking.messages.*;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reasoning.reasoner.IncrementalStreamReasoner;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.workernode.WorkerState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class WorkerNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final Logger log = ConsoleUtils.getLogger();

    private final SaturationWorker.IncrementalReasonerType incrementalReasonerType = SaturationWorker.IncrementalReasonerType.SINGLE_THREADED;
    private final ServerData serverData;
    private final long controlNodeID = 0L;
    private final AtomicInteger sentAxiomMessages = new AtomicInteger(0);
    private final AtomicInteger receivedAxiomMessages = new AtomicInteger(0);
    private final AtomicLong establishedConnections = new AtomicLong(0);
    private final AtomicInteger saturationStage = new AtomicInteger(0);
    private C closure;
    private Collection<? extends Rule<C, A>> rules;
    private WorkerState<C, A, T> state;
    private IncrementalStreamReasoner<C, A> incrementalReasoner;
    private SaturationConfiguration config;
    private WorkerStatistics stats = new WorkerStatistics();
    private List<DistributedWorkerModel<C, A, T>> workers;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private ExecutorService threadPool;
    private long workerID = -1L;

    private NIO2NetworkingComponent networkingComponent;
    private Subscriber<Object> receivedMessagesSubscriber;
    private boolean allConnectionsEstablished = false;
    private long initializationMessageID = -1;
    private AcknowledgementEventManager acknowledgementEventManager;
    private BiMap<Long, Long> socketIDToWorkerIDMap = HashBiMap.create();
    private BiMap<Long, Long> workerIDToSocketIDMap = socketIDToWorkerIDMap.inverse();
    private Flux<Object> receivedMessagesFlux;

    public WorkerNodeCommunicationChannel(ExecutorService threadPool, ServerData serverData,
                                          Subscriber<Object> receivedMessagesSubscriber) {
        this.threadPool = threadPool;
        this.serverData = serverData;
        this.receivedMessagesSubscriber = receivedMessagesSubscriber;
        init();
    }

    private void init() {
        this.acknowledgementEventManager = new AcknowledgementEventManager();
        networkingComponent = new NIO2NetworkingComponent(threadPool);
        try {
            networkingComponent.listenToPort(getWorkerServerConnectionModel());
        } catch (IOException e) {
            e.printStackTrace();
        }
        receivedMessagesFlux = Flux.from(networkingComponent.getReceivedMessagesPublisher())
                .doOnNext(new ConsumerForNewConnectionMessages())
                .map(messageEnvelope -> {
                    if (messageEnvelope.getMessage() != null) {
                        return messageEnvelope.getMessage();
                    } else {
                        return new StateInfoMessage(0, SaturationStatusMessage.TODO_IS_EMPTY_EVENT);
                    }
                })
                .map(msg -> {
                    if (msg instanceof MessageModel) {
                        state.processMessage(msg);
                        // TODO return saturation messages from current iteration
                        return null;
                    } else {
                        return this.incrementalReasoner.getStreamOfInferencesForGivenAxiom((A) msg)
                                .flatMap(this::getAxiomMessagesFromInferenceStream);
                    }
                });
                //  group by axiom and non-axiom objects
                //  non-axioms:
                //  process using state object -> map to MessageModel objects
                // axioms:
                // if state == converged -> change to running
                //         communicationChannel.distributeInferences(incrementalReasoner.getStreamOfInferencesForGivenAxiom(axiom)
                //                .filter(inference -> !this.worker.getClosure().contains(inference)) // only those which are not contained in closure
                //        );
                        receivedMessagesFlux.subscribe(this.receivedMessagesSubscriber);
    }

    public void reset() {
        networkingComponent.closeAllSockets();

        sentAxiomMessages.set(0);
        receivedAxiomMessages.set(0);
        establishedConnections.set(0);
        saturationStage.set(0);
        workerID = -1L;
        workers = null;
        workloadDistributor = null;
        allConnectionsEstablished = false;
        acknowledgementEventManager = new AcknowledgementEventManager();
        initializationMessageID = -1;
        config = null;
        stats = null;
    }

    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID
        for (DistributedWorkerModel<C, A, T> workerModel : this.workers) {
            if (workerModel.getID() > this.workerID) {
                NIO2ConnectionModel workerConModel = getWorkerClientConnectionModel(workerModel);
                try {
                    networkingComponent.connectToServer(workerConModel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void acknowledgeMessage(long receiverID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(this.workerID, messageID);
        send(receiverID, ack);
    }

    public void onSaturationFinished() {
        this.receivedMessagesSubscriber.onComplete();
    }

    public void distributeInferences(Stream<A> inferenceStream) {
        inferenceStream.forEach(inference -> {
            if (config.collectWorkerNodeStatistics()) {
                stats.getNumberOfDerivedInferences().incrementAndGet();
                stats.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            }

            Iterator<Long> receiverWorkerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(inference).iterator();
            long currentReceiverWorkerID;

            while (receiverWorkerIDs.hasNext()) {
                currentReceiverWorkerID = receiverWorkerIDs.next();

                if (currentReceiverWorkerID != this.workerID) {
                    sendAxiom(currentReceiverWorkerID, inference);
                } else {
                    // add axioms from this worker directly to the queue
                    // TODO serialize messages over the network
                    Mono.just(inference).subscribe(receivedMessagesSubscriber);
                }
            }

            if (config.collectWorkerNodeStatistics()) {
                stats.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            }
        });
    }

    public Stream<MessageEnvelope> getAxiomMessagesFromInferenceStream(A inference) {
        Stream.Builder<MessageEnvelope> streamBuilder = Stream.builder();

        if (config.collectWorkerNodeStatistics()) {
            stats.getNumberOfDerivedInferences().incrementAndGet();
            stats.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }

        Iterator<Long> receiverWorkerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(inference).iterator();
        long currentReceiverWorkerID;

        while (receiverWorkerIDs.hasNext()) {
            currentReceiverWorkerID = receiverWorkerIDs.next();

            if (currentReceiverWorkerID != this.workerID) {
                long receiverSocketID = workerIDToSocketIDMap.get(currentReceiverWorkerID);
                streamBuilder.add(new MessageEnvelope(receiverSocketID, inference));
            } else {
                // add axioms from this worker directly to the queue
                // TODO serialize messages over the network
                // TODO remove recursive call
                incrementalReasoner.getStreamOfInferencesForGivenAxiom(inference)
                        .flatMap(this::getAxiomMessagesFromInferenceStream)
                        .forEach(streamBuilder::add);
            }
        }

        if (config.collectWorkerNodeStatistics()) {
            stats.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }

        return streamBuilder.build();
    }

    public void sendToControlNode(SaturationStatusMessage status, Runnable onAcknowledgement) {
        send(controlNodeID, status, onAcknowledgement);
    }

    public void sendAxiomCountToControlNode() {
        AxiomCount axiomCount = new AxiomCount(
                this.workerID,
                this.saturationStage.get(),
                this.sentAxiomMessages.getAndUpdate(count -> 0),
                this.receivedAxiomMessages.getAndUpdate(count -> 0));
        send(controlNodeID, axiomCount);
    }

    public void sendToControlNode(C closure) {
        // generate closure
        Collection<A> closureResults = closure.getClosureResults();
        closureResults.forEach(axiom -> {
            send(controlNodeID, axiom);
        });
    }

    public void sendToControlNode(WorkerStatistics stats) {
        send(controlNodeID, new StatisticsMessage(this.workerID, stats));
    }


    public void send(long workerID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(this.workerID, status);
        send(workerID, stateInfoMessage, onAcknowledgement);
    }

    public void send(long workerID, MessageModel messageModel, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(messageModel.getMessageID(), onAcknowledgement);
        send(workerID, messageModel);
    }

    public void send(long workerID, Serializable message) {
        long socketID = this.workerIDToSocketIDMap.get(workerID);
        networkingComponent.sendMessage(socketID, message);
    }

    public void terminate() {
        this.networkingComponent.terminate();
    }

    public void sendAxiom(long receiverWorkerID, A axiom) {
        sentAxiomMessages.getAndIncrement();
        if (config.collectWorkerNodeStatistics()) {
            stats.getNumberOfSentAxioms().getAndIncrement();
        }

        send(receiverWorkerID, axiom);
    }

    public List<DistributedWorkerModel<C, A, T>> getWorkers() {
        return this.workers;
    }

    public void setWorkers(List<DistributedWorkerModel<C, A, T>> workers) {
        this.workers = workers;
    }

    public void setWorkerID(long workerID) {
        this.workerID = workerID;
    }

    public void setWorkloadDistributor(WorkloadDistributor<C, A, T> workloadDistributor) {
        this.workloadDistributor = workloadDistributor;
    }

    public void addAxiomsToQueue(List<A> axioms) {
        Flux.fromStream(axioms.stream()).subscribe(receivedMessagesSubscriber);
    }

    public AcknowledgementEventManager getAcknowledgementEventManager() {
        return acknowledgementEventManager;
    }

    public void acknowledgeInitializationMessage() {
        acknowledgeMessage(0, initializationMessageID);
    }

    public long getControlNodeID() {
        return 0;
    }

    public boolean allConnectionsEstablished() {
        // # of all other workers
        return this.establishedConnections.get() == this.workers.size() - 1;
    }

    public AtomicInteger getSentAxiomMessages() {
        return sentAxiomMessages;
    }

    public AtomicInteger getReceivedAxiomMessages() {
        return receivedAxiomMessages;
    }

    public AtomicInteger getSaturationStageCounter() {
        return saturationStage;
    }

    public void setConfig(SaturationConfiguration config) {
        this.config = config;
    }

    public void setStats(WorkerStatistics stats) {
        this.stats = stats;
    }

    public void closeAllConnections() {
        networkingComponent.closeAllSockets();
    }


    private NIO2ConnectionModel getWorkerClientConnectionModel(DistributedWorkerModel<C, A, T> workerModel) {
        ServerData workerServerData = workerModel.getServerData();

        return new NIO2ConnectionModel(workerServerData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                log.info("Connection to worker server established.");

                StateInfoMessage stateInfoMessage = new StateInfoMessage(
                        workerID,
                        SaturationStatusMessage.WORKER_CLIENT_HELLO
                );
                send(workerModel.getID(), stateInfoMessage, establishedConnections::getAndIncrement);
            }
        };
    }

    public AtomicLong getEstablishedConnections() {
        return establishedConnections;
    }

    private NIO2ConnectionModel getWorkerServerConnectionModel() {
        return new NIO2ConnectionModel(serverData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                log.info("Client connected to worker.");
                // client will send WORKER_CLIENT_HELLO message
            }
        };
    }

    private class ProcessorTest implements Processor<MessageEnvelope, Object> {

        @Override
        public void subscribe(Subscriber<? super Object> subscriber) {

        }

        @Override
        public void onSubscribe(Subscription subscription) {

        }

        @Override
        public void onNext(MessageEnvelope messageEnvelope) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {

        }
    }

    private class MessagesToSendPublisher implements Publisher<Object>, Subscription {

        @Override
        public void subscribe(Subscriber<? super Object> subscriber) {

        }

        @Override
        public void request(long l) {

        }

        @Override
        public void cancel() {

        }
    }


    private class ConsumerForNewConnectionMessages implements Consumer<MessageEnvelope> {

        @Override
        public void accept(MessageEnvelope messageEnvelope) {
            Object message = messageEnvelope.getMessage();
            if (!(message instanceof MessageModel)) {
                // must be an axiom
                receivedAxiomMessages.getAndIncrement();
                if (config.collectWorkerNodeStatistics()) {
                    stats.getNumberOfReceivedAxioms().incrementAndGet();
                }
                return;
            }
            if (!allConnectionsEstablished) {
                initializeConnection(messageEnvelope);
            }
        }

        private void initializeConnection(MessageEnvelope messageEnvelope) {
            MessageModel<C, A, T> messageModel = (MessageModel<C, A, T>) messageEnvelope.getMessage();
            long socketID = messageEnvelope.getSocketID();
            long senderSaturationID = messageModel.getSenderID();

            boolean newConnection = socketIDToWorkerIDMap.put(socketID, senderSaturationID) == null;
            if (newConnection) {
                establishedConnections.incrementAndGet();
            }

            if (workers != null && workers.size() == establishedConnections.get()) {
                //  if all connections (i.e., # workers - 1 + single control node) are established
                allConnectionsEstablished = true;
            }

            if (messageModel instanceof InitializeWorkerMessage) {
                // first message from control node - get message ID for acknowledgement
                initializationMessageID = messageModel.getMessageID();
            }
        }
    }

}
