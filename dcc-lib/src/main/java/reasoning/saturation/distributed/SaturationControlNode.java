package reasoning.saturation.distributed;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Streams;
import data.Closure;
import enums.SaturationStatusMessage;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.NIO2ConnectionModel;
import networking.io.SocketManager;
import networking.messages.*;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SaturationControlNode<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final Logger log = ConsoleUtils.getLogger();

    private final List<DistributedWorkerModel<C, A, T>> workers;
    protected Map<Long, DistributedWorkerModel<C, A, T>> workerIDToWorker;
    private C resultingClosure;
    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected Iterator<? extends A> initialAxioms;

    private ControlNodeState<C, A, T> state;
    private SaturationConfiguration config;
    private ControlNodeStatistics stats = new ControlNodeStatistics();
    private List<WorkerStatistics> workerStatistics = new ArrayList<>();

    private BlockingQueue<C> closureResultQueue = new ArrayBlockingQueue<>(1);

    private ExecutorService threadPool;

    protected NIO2NetworkingComponent networkingComponent;
    protected long controlNodeID = 0L;

    protected BiMap<Long, Long> socketIDToWorkerIDMap = HashBiMap.create();
    protected BiMap<Long, Long> workerIDToSocketIDMap = socketIDToWorkerIDMap.inverse();

    protected AcknowledgementEventManager acknowledgementEventManager;

    protected boolean allConnectionsEstablished = false;
    protected AtomicInteger establishedConnections = new AtomicInteger(0);
    protected AtomicInteger initializedWorkers = new AtomicInteger(0);
    protected AtomicInteger receivedClosureResults = new AtomicInteger(0);

    protected AtomicInteger saturationStage = new AtomicInteger(0);
    protected AtomicInteger sumOfAllReceivedAxioms = new AtomicInteger(0);
    protected AtomicInteger sumOfAllSentAxioms = new AtomicInteger(0);

    protected Stream.Builder<MessageEnvelope> messagesFromLastIteration = Stream.builder();

    boolean running = true;

    protected Consumer<NIO2NetworkingComponent.ReceivedMessagesPublisher> onNewMessageReceived = publisher -> {
        if (running) {
            Flux.from(networkingComponent.getReceivedMessagesPublisher())
                    .map(msg -> {
                        if (msg.getMessage() != null) {
                            return msg.getMessage();
                        } else {
                            return new StateInfoMessage(controlNodeID, SaturationStatusMessage.TODO_IS_EMPTY_EVENT);
                        }
                    })
                    .flatMap(msg -> {
                        state.processMessage(msg);
                        Stream<MessageEnvelope> messages = messagesFromLastIteration.build();
                        messagesFromLastIteration = Stream.builder();
                        return Flux.fromStream(messages);
                    })
                    .subscribe(networkingComponent.getNewSubscriberForMessagesToSend());
        }
    };


    protected SaturationControlNode(List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    Iterator<? extends A> initialAxioms,
                                    C resultingClosure,
                                    SaturationConfiguration config) {
        this.workers = workers;
        this.resultingClosure = resultingClosure;
        this.config = config;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;

        this.threadPool = Executors.newFixedThreadPool(1);
    }

    public C saturate() {
        init();
        try {
            this.closureResultQueue.take();
            networkingComponent.terminate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this.resultingClosure;
    }

    public void onSaturationFinished() {
        running = false;
        if (config.collectControlNodeStatistics()) {
            stats.collectStopwatchTimes();
        }
        closureResultQueue.add(resultingClosure);
    }

    private void init() {
        networkingComponent = new NIO2NetworkingComponent(threadPool, onNewMessageReceived);
        acknowledgementEventManager = new AcknowledgementEventManager();

        this.state = new CNSInitializing<>(this);
        this.workerIDToWorker = new ConcurrentHashMap<>();
        workers.forEach(p -> workerIDToWorker.put(p.getID(), p));
    }

    public void initializeConnectionToWorkerServers() {
        workers.stream().map(this::getWorkerConnectionModel)
                .forEach(s -> {
                    try {
                        networkingComponent.connectToServer(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void distributeInitialAxioms() {
        // TODO probably not working yet
        threadPool.submit(() -> {
            Flux.fromStream(Streams.stream(this.initialAxioms))
                    .flatMap(axiom -> {
                        Stream.Builder<MessageEnvelope> axioms = Stream.builder();
                        workloadDistributor.getRelevantWorkerIDsForAxiom(axiom).forEach(workerID -> {
                            sumOfAllSentAxioms.incrementAndGet();
                            sendMessage(workerID, axiom);
                        });
                        return Flux.fromStream(axioms.build());
                    })
                    .subscribe(networkingComponent.getNewSubscriberForMessagesToSend());
        });
    }

    public void terminate() {
        networkingComponent.terminate();
    }

    public void broadcast(SaturationStatusMessage statusMessage, Runnable onAcknowledgement) {
        for (Long workerID : this.socketIDToWorkerIDMap.values()) {
            StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, statusMessage);
            sendMessage(workerID, stateInfoMessage, onAcknowledgement);
        }
    }

    public void acknowledgeMessage(long receiverWorkerID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(controlNodeID, messageID);
        sendMessage(receiverWorkerID, ack);
    }

    public void sendMessage(long workerID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, status);
        sendMessage(workerID, stateInfoMessage, onAcknowledgement);
    }

    public void sendMessage(long workerID, MessageModel<C, A, T> message, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(message.getMessageID(), onAcknowledgement);
        sendMessage(workerID, message);
    }

    public void sendMessage(long workerID, Object message) {
        long socketID = workerIDToSocketIDMap.get(workerID);
        this.messagesFromLastIteration.add(new MessageEnvelope(socketID, message));
    }

    public void requestAxiomCountsFromAllWorkers() {
        this.saturationStage.incrementAndGet();
        for (Long workerID : this.workerIDToSocketIDMap.keySet()) {
            RequestAxiomMessageCount requestAxiomMessageCount = new RequestAxiomMessageCount(controlNodeID,
                    this.saturationStage.get());
            sendMessage(workerID, requestAxiomMessageCount);
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

    private NIO2ConnectionModel getWorkerConnectionModel(DistributedWorkerModel<C, A, T> workerModel) {
        ServerData serverData = workerModel.getServerData();
        return new NIO2ConnectionModel(serverData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                log.info("Connection established to worker server " + workerModel.getID() + ".");

                workerIDToSocketIDMap.put(workerModel.getID(), socketManager.getSocketID());

                establishedConnections.incrementAndGet();
                if (establishedConnections.get() == workers.size()) {
                    allConnectionsEstablished = true;
                }

                // send initialization message
                log.info("Sending initialization message to worker " + workerModel.getID() + ".");
                InitializeWorkerMessage<C, A, T> initializeWorkerMessage = new InitializeWorkerMessage<>(
                        controlNodeID,
                        workerModel.getID(),
                        workers,
                        workloadDistributor,
                        workerModel.getClosure(),
                        workerModel.getRules(),
                        config
                );
                long socketID = workerIDToSocketIDMap.get(workerModel.getID());
                MessageEnvelope messageEnvelope = new MessageEnvelope(socketID, initializeWorkerMessage);
                acknowledgementEventManager.messageRequiresAcknowledgment(initializeWorkerMessage.getMessageID(), () -> initializedWorkers.getAndIncrement());
                Flux.just(messageEnvelope).subscribe(networkingComponent.getNewSubscriberForMessagesToSend());
            }
        };
    }

    public SaturationConfiguration getConfig() {
        return this.config;
    }

    public ControlNodeStatistics getControlNodeStatistics() {
        return this.stats;
    }

    public void switchState(ControlNodeState<C, A, T> newState) {
        this.state = newState;
    }

    public List<DistributedWorkerModel<C, A, T>> getWorkers() {
        return this.workers;
    }

    public void addAxiomToClosureResult(A axiom) {
        this.resultingClosure.add(axiom);
    }

    public List<WorkerStatistics> getWorkerStatistics() {
        return this.workerStatistics;
    }
}
