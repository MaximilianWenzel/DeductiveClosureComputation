package reasoning.saturation.distributed.communication;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import networking.netty.NettyConnectionModel;
import networking.netty.NettyReactorNetworkingComponent;
import networking.netty.NettySocketManager;
import org.reactivestreams.Subscriber;
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
    private Sinks.Many<Object> receivedMessages = ReactorSinkFactory.getSink(); // TODO adjust queue buffer

    private NettyReactorNetworkingComponent networkingComponent;
    protected Map<Long, Sinks.Many<Object>> connectionIDToOutboundMessages = new HashMap<>();
    protected EmitFailureHandlerImpl emitFailureHandler = new EmitFailureHandlerImpl();

    private long workerID = -1L;
    private List<DistributedWorkerModel<C, A, T>> workers;
    private WorkloadDistributor<C, A, T> workloadDistributor;

    private long controlNodeID = 0L;

    private boolean allConnectionsEstablished = false;
    private AcknowledgementEventManager acknowledgementEventManager;
    private long initializationMessageID = -1;
    private AtomicInteger saturationStage = new AtomicInteger(0);

    private SaturationConfiguration config;
    private WorkerStatistics stats;

    public WorkerNodeCommunicationChannel(ServerData serverData, Subscriber<Object> receivedMessagesSubscriber) {
        this.serverData = serverData;
        this.receivedMessages.asFlux().subscribe(receivedMessagesSubscriber);
        this.receivedMessages.asFlux().switchIfEmpty() // TODO adjust if empty
        init();
    }

    private void init() {
        this.acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NettyReactorNetworkingComponent();
    }

    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID
        for (DistributedWorkerModel<C, A, T> workerModel : this.workers) {
            if (workerModel.getID() > this.workerID) {
                NettyConnectionModel workerConModel = getWorkerClientConnectionModel(workerModel);
                networkingComponent.connectToServer(workerConModel);
            }
        }
    }

    public void acknowledgeMessage(long receiverID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(this.workerID, messageID);
        send(receiverID, ack);
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
        send(0, status, onAcknowledgement);
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

    public void terminateNow() {
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

        // TODO define queue buffer for Sink
        Sinks.Many<Object> workerOutboundMessages = ReactorSinkFactory.getSink();
        this.connectionIDToOutboundMessages.put(workerModel.getID(), workerOutboundMessages);

        NettyConnectionModel conModel = new NettyConnectionModel(
                serverData,
                onConnectionEstablished,
                (msg) -> receivedMessages.emitNext(msg, emitFailureHandler),
                workerOutboundMessages.asFlux(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        return conModel;
    }

    private NettyConnectionModel getWorkerServerConnectionModel() {
        Sinks.Many<Object> workerOutboundMessages = ReactorSinkFactory.getSink();

        Consumer<NettySocketManager> onConnectionEstablished = socketManager -> {
            log.info("Client connected to worker.");
            // client will send WORKER_CLIENT_HELLO message
            StateInfoMessage stateInfoMessage = new StateInfoMessage(this.workerID,
                    SaturationStatusMessage.WORKER_SERVER_HELLO);
            send(socketManager.getSocketID(), stateInfoMessage,
                    WorkerNodeCommunicationChannel.this.establishedConnections::getAndIncrement);
        };

        NettyConnectionModel conModel = new NettyConnectionModel(
                serverData,
                onConnectionEstablished,
                (msg) -> new ConsumerForNewConnectionMessages(workerOutboundMessages),
                workerOutboundMessages.asFlux(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        return conModel;
    }

    private class ConsumerForNewConnectionMessages implements Consumer<Object> {

        private Sinks.Many<Object> outboundMessages;

        public ConsumerForNewConnectionMessages(Sinks.Many<Object> outboundMessages) {
            this.outboundMessages = outboundMessages;
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
            connectionIDToOutboundMessages.put(messageModel.getSenderID(), outboundMessages);

            // get worker ID / control node ID to socket ID mapping
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
