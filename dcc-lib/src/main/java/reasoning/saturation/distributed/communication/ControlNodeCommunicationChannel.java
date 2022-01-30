package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.Closure;
import enums.SaturationStatusMessage;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.NIO2ConnectionModel;
import networking.io.SocketManager;
import networking.messages.*;
import networking.netty.NettyConnectionModel;
import networking.netty.NettySocketManager;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;
import util.ReactorSinkFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ControlNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final Logger log = ConsoleUtils.getLogger();

    protected NIO2NetworkingComponent networkingComponent;
    protected List<DistributedWorkerModel<C, A, T>> workers;
    protected Map<Long, DistributedWorkerModel<C, A, T>> workerIDToWorker;
    protected long controlNodeID = 0L;
    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected Iterator<? extends A> initialAxioms;

    protected BiMap<Long, Long> socketIDToWorkerIDMap = HashBiMap.create();
    protected BiMap<Long, Long> workerIDToSocketIDMap = socketIDToWorkerIDMap.inverse();

    protected ExecutorService threadPool;
    protected Subscriber<Object> receivedMessagesSubscriber;

    protected AcknowledgementEventManager acknowledgementEventManager;

    protected boolean allConnectionsEstablished = false;
    protected AtomicInteger establishedConnections = new AtomicInteger(0);
    protected AtomicInteger initializedWorkers = new AtomicInteger(0);
    protected AtomicInteger receivedClosureResults = new AtomicInteger(0);

    protected AtomicInteger saturationStage = new AtomicInteger(0);
    protected AtomicInteger sumOfAllReceivedAxioms = new AtomicInteger(0);
    protected AtomicInteger sumOfAllSentAxioms = new AtomicInteger(0);

    protected SaturationConfiguration config;

    public ControlNodeCommunicationChannel(ExecutorService threadPool,
                                           Subscriber<Object> receivedMessagesSubscriber,
                                           List<DistributedWorkerModel<C, A, T>> workers,
                                           WorkloadDistributor<C, A, T> workloadDistributor,
                                           Iterator<? extends A> initialAxioms,
                                           SaturationConfiguration config) {
        this.threadPool = threadPool;
        this.receivedMessagesSubscriber = receivedMessagesSubscriber;
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;
        this.config = config;
        init();
    }

    private void init() {
        this.workerIDToWorker = new ConcurrentHashMap<>();
        workers.forEach(p -> workerIDToWorker.put(p.getID(), p));

        acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NIO2NetworkingComponent(threadPool);
        Flux.from(networkingComponent.getReceivedMessagesPublisher())
                .map(MessageEnvelope::getMessage)
                .subscribe(receivedMessagesSubscriber);
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
        this.initialAxioms.forEachRemaining(axiom -> {
            workloadDistributor.getRelevantWorkerIDsForAxiom(axiom).forEach(workerID -> {
                sumOfAllSentAxioms.incrementAndGet();
                send(workerID, axiom);
            });
        });
    }

    public void terminate() {
        networkingComponent.terminate();
    }

    public void broadcast(SaturationStatusMessage statusMessage, Runnable onAcknowledgement) {
        for (Long workerID : this.socketIDToWorkerIDMap.values()) {
            StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, statusMessage);
            send(workerID, stateInfoMessage, onAcknowledgement);
        }
    }

    public void acknowledgeMessage(long receiverWorkerID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(controlNodeID, messageID);
        send(receiverWorkerID, ack);
    }

    public void send(long workerID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, status);
        send(workerID, stateInfoMessage, onAcknowledgement);
    }

    public void send(long workerID, MessageModel<C, A, T> message, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(message.getMessageID(), onAcknowledgement);
        send(workerID, message);
    }

    public void send(long workerID, Serializable message) {
        long socketID = workerIDToSocketIDMap.get(workerID);
        networkingComponent.sendMessage(socketID, message);
    }

    public void requestAxiomCountsFromAllWorkers() {
        this.saturationStage.incrementAndGet();
        for (Long workerID : this.workerIDToSocketIDMap.keySet()) {
            RequestAxiomMessageCount requestAxiomMessageCount = new RequestAxiomMessageCount(controlNodeID,
                    this.saturationStage.get());

            send(workerID, requestAxiomMessageCount);
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
                send(workerModel.getID(), initializeWorkerMessage, () -> initializedWorkers.getAndIncrement());
            }
        };
    }

}
