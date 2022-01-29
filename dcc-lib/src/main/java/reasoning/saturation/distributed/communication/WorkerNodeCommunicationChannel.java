package reasoning.saturation.distributed.communication;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import networking.netty.NettyConnectionModel;
import networking.netty.NettyNetworkingComponent;
import networking.netty.NettyReactorNetworkingComponent;
import networking.netty.NettySocketManager;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;
import util.ReactorSinkFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class WorkerNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final Logger log = ConsoleUtils.getLogger();

    private final ServerData serverData;
    private final AtomicInteger sentAxiomMessages = new AtomicInteger(0);
    private final AtomicInteger receivedAxiomMessages = new AtomicInteger(0);
    private final AtomicLong establishedConnections = new AtomicLong(0);
    protected Map<Long, Sinks.Many<Object>> connectionIDToOutboundMessages = new HashMap<>();
    protected EmitFailureHandlerImpl emitFailureHandler = new EmitFailureHandlerImpl();
    private Sinks.Many<Object> receivedMessages = ReactorSinkFactory.getSink();
    private Subscriber<Object> receivedMessagesSubscriber;
    private NettyNetworkingComponent networkingComponent;
    private long workerID = -1L;
    private List<DistributedWorkerModel<C, A, T>> workers;
    private WorkloadDistributor<C, A, T> workloadDistributor;

    private long controlNodeID = 0L;

    private boolean allConnectionsEstablished = false;
    private AcknowledgementEventManager acknowledgementEventManager;
    private long initializationMessageID = -1;
    final private AtomicInteger saturationStage = new AtomicInteger(0);

    private SaturationConfiguration config;
    private WorkerStatistics stats;

    public WorkerNodeCommunicationChannel(ServerData serverData, Subscriber<Object> receivedMessagesSubscriber) {
        this.serverData = serverData;
        this.receivedMessagesSubscriber = receivedMessagesSubscriber;
        init();
    }

    private void init() {
        this.acknowledgementEventManager = new AcknowledgementEventManager();
        //networkingComponent = new NIO2NetworkingComponent(Executors.newFixedThreadPool(1)); TODO
        //networkingComponent.listenToPort(getWorkerServerConnectionModel()); TODO
    }

    public void reset() {
        networkingComponent.closeAllSockets();

        sentAxiomMessages.set(0);
        receivedAxiomMessages.set(0);
        establishedConnections.set(0);
        saturationStage.set(0);
        connectionIDToOutboundMessages.clear();
        emitFailureHandler = new EmitFailureHandlerImpl();
        receivedMessages = ReactorSinkFactory.getSink();
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
                NettyConnectionModel workerConModel = getWorkerClientConnectionModel(workerModel);
                //networkingComponent.connectToServer(workerConModel); TODO
            }
        }
        this.receivedMessages.asFlux().subscribe(receivedMessagesSubscriber);
        this.receivedMessages.asFlux()
                .switchIfEmpty(Mono.just(SaturationStatusMessage.TODO_IS_EMPTY_EVENT)); // TODO adjust if flux is empty
    }

    public void acknowledgeMessage(long receiverID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(this.workerID, messageID);
        send(receiverID, ack);
    }

    public void onSaturationFinished() {
        this.receivedMessages.emitComplete(emitFailureHandler);
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
                    receivedMessages.emitNext(inference, emitFailureHandler);
                }
            }

            if (config.collectWorkerNodeStatistics()) {
                stats.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            }
        });
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
        networkingComponent.sendMessage(workerID, messageModel);
    }

    public void send(long workerID, Serializable message) {
        networkingComponent.sendMessage(workerID, message);
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

    public void setWorkloadDistributor(WorkloadDistributor workloadDistributor) {
        this.workloadDistributor = workloadDistributor;
    }

    public void addAxiomsToQueue(List<A> axioms) {
        for (A a : axioms) {
            this.receivedMessages.emitNext(a, emitFailureHandler);
        }
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


    private NettyConnectionModel getWorkerClientConnectionModel(DistributedWorkerModel<C, A, T> workerModel) {
        ServerData serverData = workerModel.getServerData();
        Consumer<NettySocketManager> onConnectionEstablished = socketManager -> {
            log.info("Connection to worker server established.");

            StateInfoMessage stateInfoMessage = new StateInfoMessage(
                    this.workerID,
                    SaturationStatusMessage.WORKER_CLIENT_HELLO
            );
            send(workerModel.getID(), stateInfoMessage, this.establishedConnections::getAndIncrement);
        };

        Sinks.Many<Object> workerOutboundMessages = ReactorSinkFactory.getSink();
        this.connectionIDToOutboundMessages.put(workerModel.getID(), workerOutboundMessages);

        Sinks.Many<Object> receivedMessagesSink = ReactorSinkFactory.getSink();
        ConsumerForNewConnectionMessages consumer = new ConsumerForNewConnectionMessages(workerOutboundMessages, true);
        receivedMessagesSink.asFlux().doOnNext(consumer);

        /* TODO
        NettyConnectionModel conModel = new NettyConnectionModel(
                serverData,
                onConnectionEstablished,
                receivedMessagesSink,
                workerOutboundMessages.asFlux(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        return conModel;

         */
        return null;
    }

    public AtomicLong getEstablishedConnections() {
        return establishedConnections;
    }

    private NettyConnectionModel getWorkerServerConnectionModel() {
        Sinks.Many<Object> workerOutboundMessages = ReactorSinkFactory.getSink();

        Consumer<NettySocketManager> onConnectionEstablished = socketManager -> {
            log.info("Client connected to worker.");
            // client will send WORKER_CLIENT_HELLO message
        };

        Sinks.Many<Object> receivedMessagesSink = ReactorSinkFactory.getSink();
        ConsumerForNewConnectionMessages consumer = new ConsumerForNewConnectionMessages(workerOutboundMessages, false);
        receivedMessagesSink.asFlux().doOnNext(consumer);

        /* TODO
        NettyConnectionModel conModel = new NettyConnectionModel(
                serverData,
                onConnectionEstablished,
                receivedMessagesSink,
                workerOutboundMessages.asFlux(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        return conModel;

         */
        return null;
    }

    private class ConsumerForNewConnectionMessages implements Consumer<Object> {

        private Sinks.Many<Object> outboundMessages;
        private boolean workerIDOfConnectionIsKnown;

        public ConsumerForNewConnectionMessages(Sinks.Many<Object> outboundMessages,
                                                boolean workerIDOfConnectionIsKnown) {
            this.outboundMessages = outboundMessages;
            this.workerIDOfConnectionIsKnown = workerIDOfConnectionIsKnown;
        }

        @Override
        public void accept(Object message) {
            if (!(message instanceof MessageModel)) {
                // must be an axiom
                receivedAxiomMessages.getAndIncrement();
                if (config.collectWorkerNodeStatistics()) {
                    stats.getNumberOfReceivedAxioms().incrementAndGet();
                }
                receivedMessages.emitNext(message, emitFailureHandler);
                return;
            }

            MessageModel messageModel = (MessageModel) message;

            if (!allConnectionsEstablished) {
                initializeConnection(messageModel);
            }
            receivedMessages.emitNext(message, emitFailureHandler);
        }

        private void initializeConnection(MessageModel messageModel) {
            if (!workerIDOfConnectionIsKnown) {
                Object obj = connectionIDToOutboundMessages.put(messageModel.getSenderID(), outboundMessages);
                if (obj == null) {
                    // worker ID of remote connection is now known
                    this.workerIDOfConnectionIsKnown = true;
                    establishedConnections.incrementAndGet();
                }
            }

            if (workers != null && workers.size() == establishedConnections.get()) {
                //  if all connections (i.e., # workers - 1 + single control node) are established
                allConnectionsEstablished = true;
            }

            if (messageModel instanceof InitializeWorkerMessage) {
                // first message from control node
                initializationMessageID = messageModel.getMessageID();
            }
        }
    }

    private class EmitFailureHandlerImpl implements Sinks.EmitFailureHandler {

        @Override
        public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
            System.err.println(signalType);
            System.err.println(emitResult);
            return false;
        }
    }
}
