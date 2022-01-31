package reasoning.saturation.distributed;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import exceptions.NotImplementedException;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.NIO2ConnectionModel;
import networking.io.SocketManager;
import networking.messages.*;
import reactor.core.publisher.Flux;
import reasoning.reasoner.IncrementalStreamReasoner;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.workernode.WorkerState;
import reasoning.saturation.distributed.states.workernode.WorkerStateInitializing;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SaturationWorker<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private static final Logger log = ConsoleUtils.getLogger();

    private final SaturationWorker.IncrementalReasonerType incrementalReasonerType = SaturationWorker.IncrementalReasonerType.SINGLE_THREADED;
    private final ServerData serverData;
    private final long controlNodeID = 0L;
    private final AtomicInteger sentAxiomMessages = new AtomicInteger(0);
    private final AtomicInteger receivedAxiomMessages = new AtomicInteger(0);
    private final AtomicInteger saturationStage = new AtomicInteger(0);
    private C closure;
    private Collection<? extends Rule<C, A>> rules;
    private WorkerState<C, A, T> state;
    private IncrementalStreamReasoner<C, A> incrementalReasoner;
    private SaturationConfiguration config = new SaturationConfiguration();
    private WorkerStatistics stats = new WorkerStatistics();
    private List<DistributedWorkerModel<C, A, T>> workers;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private ExecutorService threadPool;
    private long workerID = -1L;

    private NIO2NetworkingComponent networkingComponent;
    private boolean allConnectionsEstablished = false;
    private AcknowledgementEventManager acknowledgementEventManager = new AcknowledgementEventManager();
    private BiMap<Long, Long> socketIDToWorkerIDMap = HashBiMap.create();
    private BiMap<Long, Long> workerIDToSocketIDMap = socketIDToWorkerIDMap.inverse();

    private Flux<MessageEnvelope> processedMessagesFlux;

    private Stream.Builder<MessageEnvelope> messagesFromCurrentIteration = Stream.builder();

    private Consumer<NIO2NetworkingComponent.ReceivedMessagesPublisher> onNewMessagesReceived = publisher -> {
        processedMessagesFlux = Flux.from(networkingComponent.getReceivedMessagesPublisher())
                .doOnNext(new ConsumerForNewConnectionMessages())
                .map(messageEnvelope -> {
                    if (messageEnvelope.getMessage() != null) {
                        return messageEnvelope.getMessage();
                    } else {
                        return new StateInfoMessage(0, SaturationStatusMessage.TODO_IS_EMPTY_EVENT);
                    }
                })
                .flatMap(msg -> {
                    state.processMessage(msg);
                    Stream<MessageEnvelope> messages = messagesFromCurrentIteration.build();
                    messagesFromCurrentIteration = Stream.builder();
                    return Flux.fromStream(messages);
                });
        processedMessagesFlux.subscribe(networkingComponent.getNewSubscriberForMessagesToSend());
    };

    public SaturationWorker(ServerData serverData) {
        this.serverData = serverData;
        init();
    }

    public static void main(String[] args) {
        // args: <HOSTNAME> <PORT-NUMBER>
        log.info("Generating worker...");

        if (args.length != 3) {
            throw new IllegalArgumentException("arguments: <HOSTNAME> <PORT-NUMBER>");
        }

        String hostname = args[0];
        int portNumber = Integer.parseInt(args[1]);

        ServerData serverData = new ServerData(hostname, portNumber);

        SaturationWorker<?, ?, ?> saturationWorker = new SaturationWorker<>(
                serverData
        );
        saturationWorker.start();
    }

    public void initializeWorker(InitializeWorkerMessage<C, A, T> message) {
        this.workerID = message.getWorkerID();
        this.workers = message.getWorkers();
        this.workloadDistributor = message.getWorkloadDistributor();
        this.config = message.getConfig();
        this.closure = message.getClosure();

        this.setRules(message.getRules());
    }

    public void setRules(Collection<? extends Rule<C, A>> rules) {
        this.rules = rules;
        initializeRules();
    }

    public void start() {

    }

    public void stop() {
        networkingComponent.terminate();
    }

    private void initializeRules() {
        this.rules.forEach(r -> {
            r.setClosure(closure);
        });

        switch (incrementalReasonerType) {
            case SINGLE_THREADED:
                this.incrementalReasoner = new IncrementalStreamReasoner<>(rules, closure, config, stats);
                break;
            default:
                throw new NotImplementedException();
        }
    }

    public void switchState(WorkerState<C, A, T> newState) {
        this.state = newState;
    }

    public SaturationConfiguration getConfig() {
        return config;
    }

    public void setConfig(SaturationConfiguration config) {
        this.config = config;
    }

    public WorkerStatistics getStats() {
        return stats;
    }

    public void setStats(WorkerStatistics stats) {
        this.stats = stats;
    }

    public WorkerState<C, A, T> getState() {
        return state;
    }

    private void clearWorkerForNewSaturation() {
        log.info("Restarting worker...");
        reset();
        this.state = new WorkerStateInitializing<>(this);
        this.rules = null;
        this.config = null;
        this.stats = new WorkerStatistics();
        log.info("Worker successfully restarted.");
    }

    private void init() {
        this.threadPool = Executors.newFixedThreadPool(1);
        this.state = new WorkerStateInitializing<>(this);

        this.acknowledgementEventManager = new AcknowledgementEventManager();
        networkingComponent = new NIO2NetworkingComponent(threadPool, onNewMessagesReceived);
        try {
            networkingComponent.listenToPort(getWorkerServerConnectionModel());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void reset() {
        networkingComponent.closeAllSockets();

        socketIDToWorkerIDMap.clear();
        sentAxiomMessages.set(0);
        receivedAxiomMessages.set(0);
        saturationStage.set(0);
        workerID = -1L;
        workers = null;
        workloadDistributor = null;
        allConnectionsEstablished = false;
        acknowledgementEventManager = new AcknowledgementEventManager();
        config = null;
        stats = null;
    }

    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID (and to own worker ID)
        for (DistributedWorkerModel<C, A, T> workerModel : this.workers) {
            if (workerModel.getID() >= this.workerID) {
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
        sendMessage(receiverID, ack);
    }

    public void onSaturationFinished() {
        // TODO probably change for restart
        networkingComponent.terminate();
        //this.reset();
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

            long receiverSocketID = workerIDToSocketIDMap.get(currentReceiverWorkerID);
            streamBuilder.add(new MessageEnvelope(receiverSocketID, inference));
            //toDoProcessor.addElementsToToDo(inference); // TODO maybe add axioms from this worker directly to the queue

            sentAxiomMessages.getAndIncrement();
            if (config.collectWorkerNodeStatistics()) {
                stats.getNumberOfSentAxioms().getAndIncrement();
            }
        }

        if (config.collectWorkerNodeStatistics()) {
            stats.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }

        return streamBuilder.build();
    }

    public void sendMessageToControlNode(SaturationStatusMessage status, Runnable onAcknowledgement) {
        sendMessage(controlNodeID, status, onAcknowledgement);
    }

    public void sendAxiomCountMessageToControlNode() {
        AxiomCount axiomCount = new AxiomCount(
                this.workerID,
                this.saturationStage.get(),
                this.sentAxiomMessages.getAndUpdate(count -> 0),
                this.receivedAxiomMessages.getAndUpdate(count -> 0));
        sendMessage(controlNodeID, axiomCount);
    }

    public void sendClosureToControlNode(C closure, AcknowledgementMessage closureRequestACK) {
        long controlNodeSocketID = this.workerIDToSocketIDMap.get(0L);
        MessageEnvelope ackEnvelope = new MessageEnvelope(controlNodeSocketID, closureRequestACK);
        threadPool.submit(() -> {
            Flux.concat(Flux.fromIterable(closure.getClosureResults())
                            .map(axiom -> new MessageEnvelope(controlNodeSocketID, axiom)),
                    Flux.just(ackEnvelope)
            ).subscribe(networkingComponent.getNewSubscriberForMessagesToSend());
        });
    }

    public void sendMessageToControlNode(WorkerStatistics stats) {
        sendMessage(controlNodeID, new StatisticsMessage(this.workerID, stats));
    }

    public void sendMessage(long workerID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(this.workerID, status);
        sendMessage(workerID, stateInfoMessage, onAcknowledgement);
    }

    public void sendMessage(long workerID, MessageModel messageModel, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(messageModel.getMessageID(), onAcknowledgement);
        sendMessage(workerID, messageModel);
    }

    public void sendMessage(long workerID, Serializable message) {
        long socketID = this.workerIDToSocketIDMap.get(workerID);
        this.messagesFromCurrentIteration.add(new MessageEnvelope(socketID, message));
    }

    public void sendMessage(MessageEnvelope message) {
        this.messagesFromCurrentIteration.add(message);
    }

    public void terminate() {
        this.networkingComponent.terminate();
    }

    public List<DistributedWorkerModel<C, A, T>> getWorkers() {
        return this.workers;
    }

    public void setWorkers(List<DistributedWorkerModel<C, A, T>> workers) {
        this.workers = workers;
    }

    public void setWorkloadDistributor(WorkloadDistributor<C, A, T> workloadDistributor) {
        this.workloadDistributor = workloadDistributor;
    }

    public long getWorkerID() {
        return workerID;
    }

    public void setWorkerID(long workerID) {
        this.workerID = workerID;
    }

    public void addAxiomsToToDoQueue(List<A> axioms) {
        Flux.fromIterable(axioms)
                .flatMap(msg -> {
                    state.processMessage(msg);
                    Stream<MessageEnvelope> messages = messagesFromCurrentIteration.build();
                    messagesFromCurrentIteration = Stream.builder();
                    return Flux.fromStream(messages);
                })
                .subscribe(networkingComponent.getNewSubscriberForMessagesToSend());
    }

    public AcknowledgementEventManager getAcknowledgementEventManager() {
        return acknowledgementEventManager;
    }


    public long getControlNodeID() {
        return 0;
    }

    public boolean allConnectionsEstablished() {
        // # of all other workers
        if (this.workers == null) {
            // worker not yet initialized
            return false;
        }
        return allConnectionsEstablished;
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

    public void closeAllConnections() {
        networkingComponent.closeAllSockets();
    }

    private NIO2ConnectionModel getWorkerClientConnectionModel(DistributedWorkerModel<C, A, T> workerModel) {
        ServerData workerServerData = workerModel.getServerData();

        return new NIO2ConnectionModel(workerServerData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                log.info("Connection to worker server established.");

                workerIDToSocketIDMap.put(workerModel.getID(), socketManager.getSocketID());
                StateInfoMessage stateInfoMessage = new StateInfoMessage(
                        workerID,
                        SaturationStatusMessage.WORKER_CLIENT_HELLO
                );
                Flux.just(new MessageEnvelope(socketManager.getSocketID(), stateInfoMessage))
                        .subscribe(networkingComponent.getNewSubscriberForMessagesToSend());
            }
        };
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

    public C getClosure() {
        return this.closure;
    }

    public IncrementalStreamReasoner<C, A> getIncrementalReasoner() {
        return this.incrementalReasoner;
    }

    public enum IncrementalReasonerType {
        SINGLE_THREADED,
        PARALLEL;
    }

    private class ConsumerForNewConnectionMessages implements Consumer<MessageEnvelope> {
        @Override
        public void accept(MessageEnvelope messageEnvelope) {
            Object message = messageEnvelope.getMessage();
            if (message == null) {
                // to-do is empty event
                return;
            }
            if (!allConnectionsEstablished && message instanceof MessageModel) {
                initializeConnection(messageEnvelope);
            }
            if (!(message instanceof MessageModel)) {
                // must be an axiom
                receivedAxiomMessages.getAndIncrement();
                if (config.collectWorkerNodeStatistics()) {
                    stats.getNumberOfReceivedAxioms().incrementAndGet();
                }
            }
        }

        private void initializeConnection(MessageEnvelope messageEnvelope) {
            MessageModel<C, A, T> messageModel = (MessageModel<C, A, T>) messageEnvelope.getMessage();
            long socketID = messageEnvelope.getSocketID();
            long senderSaturationID = messageModel.getSenderID();

            if (!socketIDToWorkerIDMap.containsValue(senderSaturationID)) {
                socketIDToWorkerIDMap.put(socketID, senderSaturationID);
            }

            if (workers != null && workers.size() + 1 == socketIDToWorkerIDMap.size()) {
                //  if all connections (i.e., # workers + single control node) are established
                allConnectionsEstablished = true;
            }
        }
    }

}
