package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import data.Closure;
import enums.SaturationStatusMessage;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import networking.netty.NettyConnectionModel;
import networking.netty.NettyReactorNetworkingComponent;
import networking.netty.NettySocketManager;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ControlNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    // TODO: adjust if workers are not running on localhost
    private static final boolean WORKERS_ON_LOCALHOST = false;

    private final Logger log = ConsoleUtils.getLogger();

    protected NettyReactorNetworkingComponent networkingComponent;
    protected List<DistributedWorkerModel<C, A, T>> workers;
    protected Map<Long, DistributedWorkerModel<C, A, T>> workerIDToWorker;
    protected BiMap<Long, Long> socketIDToWorkerID;
    protected BiMap<Long, Long> workerIDToSocketID;
    protected long controlNodeID = 0L;
    protected Sinks.Many<Object> receivedMessages = ReactorSinkFactory.getSink(); // TODO: define queue buffer
    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected Iterator<? extends A> initialAxioms;

    protected AcknowledgementEventManager acknowledgementEventManager;

    protected EmitFailureHandlerImpl emitFailureHandler = new EmitFailureHandlerImpl();

    protected boolean allConnectionsEstablished = false;
    protected AtomicInteger initializedWorkers = new AtomicInteger(0);
    protected AtomicInteger receivedClosureResults = new AtomicInteger(0);

    protected AtomicInteger saturationStage = new AtomicInteger(0);
    protected AtomicInteger sumOfAllReceivedAxioms = new AtomicInteger(0);
    protected AtomicInteger sumOfAllSentAxioms = new AtomicInteger(0);

    protected Map<Long, Sinks.Many<Object>> workerIDToOutboundMessages = new HashMap<>();

    protected ExecutorService threadPool;
    protected BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent;
    protected Runnable onNewMessageReceived;

    protected SaturationConfiguration config;

    public ControlNodeCommunicationChannel(List<DistributedWorkerModel<C, A, T>> workers,
                                           WorkloadDistributor<C, A, T> workloadDistributor,
                                           Iterator<? extends A> initialAxioms,
                                           SaturationConfiguration config,
                                           ExecutorService threadPool,
                                           BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent,
                                           Runnable onNewMessageReceived) {
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;
        this.config = config;
        this.threadPool = threadPool;
        this.messagesThatCouldNotBeSent = messagesThatCouldNotBeSent;
        this.onNewMessageReceived = onNewMessageReceived;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = Maps.synchronizedBiMap(HashBiMap.create());
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();

        this.workerIDToWorker = new ConcurrentHashMap<>();
        workers.forEach(p -> workerIDToWorker.put(p.getID(), p));

        acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NettyReactorNetworkingComponent();
    }

    public void initializeConnectionToWorkerServers() {
        workers.stream().map(this::getWorkerConnectionModel)
                .forEach(s -> networkingComponent.connectToServer(s));
    }

    public void distributeInitialAxioms() {
        this.initialAxioms.forEachRemaining(axiom -> {
            workloadDistributor.getRelevantWorkerIDsForAxiom(axiom).forEach(workerID -> {
                sumOfAllSentAxioms.incrementAndGet();
                send(workerIDToSocketID.get(workerID), axiom);
            });
        });
    }

    public void terminate() {
        networkingComponent.terminate();
    }

    public void broadcast(SaturationStatusMessage statusMessage, Runnable onAcknowledgement) {
        for (Long workerID : this.workerIDToOutboundMessages.keySet()) {
            StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, statusMessage);
            send(workerID, stateInfoMessage, onAcknowledgement);
        }
    }

    public void acknowledgeMessage(long receiverWorkerID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(controlNodeID, messageID);
        long workerID = workerIDToSocketID.get(receiverWorkerID);
        send(workerID, ack);
    }

    public void send(long workerID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, status);
        send(workerID, stateInfoMessage, onAcknowledgement);
    }

    public void send(long workerID, MessageModel<C, A, T> message, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(message.getMessageID(), onAcknowledgement);
        networkingComponent.sendMessage(workerID, message);
    }

    public void send(long workerID, Serializable message) {
        workerIDToOutboundMessages.get(workerID).emitNext(message, emitFailureHandler);
    }

    public void requestAxiomCountsFromAllWorkers() {
        this.saturationStage.incrementAndGet();
        for (Sinks.Many<Object> workerOutboundMessages : this.workerIDToOutboundMessages.values()) {
            RequestAxiomMessageCount requestAxiomMessageCount = new RequestAxiomMessageCount(controlNodeID,
                    this.saturationStage.get());
            workerOutboundMessages.emitNext(requestAxiomMessageCount, emitFailureHandler);
        }
    }

    public AcknowledgementEventManager getAcknowledgementEventManager() {
        return this.acknowledgementEventManager;
    }

    public boolean allWorkersInitialized() {
        return this.initializedWorkers.get() == this.workers.size();
    }

    public AtomicInteger getInitializedWorkers() {
        return initializedWorkers;
    }

    public AtomicInteger getReceivedClosureResultsCounter() {
        return this.receivedClosureResults;
    }

    public AtomicInteger getSaturationStage() {
        return saturationStage;
    }

    public AtomicInteger getSumOfAllReceivedAxioms() {
        return sumOfAllReceivedAxioms;
    }

    public AtomicInteger getSumOfAllSentAxioms() {
        return sumOfAllSentAxioms;
    }

    private NettyConnectionModel getWorkerConnectionModel(DistributedWorkerModel<C, A, T> workerModel) {
        ServerData serverData = workerModel.getServerData();
        Consumer<NettySocketManager> onConnectionEstablished = socketManager -> {
            log.info("Connection established to worker server " + workerModel.getID() + ".");

            // get worker ID to socket ID mapping
            socketIDToWorkerID.put(socketManager.getSocketID(), workerModel.getID());
            if (socketIDToWorkerID.size() == ControlNodeCommunicationChannel.this.workers.size()) {
                allConnectionsEstablished = true;
            }

            // send initialization message
            log.info("Sending initialization message to worker " + workerModel.getID() + ".");
            InitializeWorkerMessage<C, A, T> initializeWorkerMessage = new InitializeWorkerMessage<>(
                    ControlNodeCommunicationChannel.this.controlNodeID,
                    workerModel.getID(),
                    ControlNodeCommunicationChannel.this.workers,
                    workloadDistributor,
                    workerModel.getClosure(),
                    workerModel.getRules(),
                    config
            );

            send(socketManager.getSocketID(), initializeWorkerMessage, () -> initializedWorkers.getAndIncrement());
        };

        // TODO define queue buffer for Sink
        Sinks.Many<Object> workerOutboundMessages = ReactorSinkFactory.getSink();
        this.workerIDToOutboundMessages.put(workerModel.getID(), workerOutboundMessages);

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

    private class EmitFailureHandlerImpl implements Sinks.EmitFailureHandler {

        @Override
        public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
            System.err.println(signalType);
            System.err.println(emitResult);
            return false;
        }
    }

}
