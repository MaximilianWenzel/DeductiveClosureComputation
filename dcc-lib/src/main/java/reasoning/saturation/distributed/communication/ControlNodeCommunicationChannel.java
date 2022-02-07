package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import data.Closure;
import enums.SaturationStatusMessage;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.ConnectionModel;
import networking.io.SocketManager;
import networking.messages.*;
import reasoning.saturation.distributed.metadata.DistributedSaturationConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ControlNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final Logger log = ConsoleUtils.getLogger();

    protected NetworkingComponent networkingComponent;
    protected List<DistributedWorkerModel<C, A, T>> workers;
    protected Map<Long, DistributedWorkerModel<C, A, T>> workerIDToWorker;
    protected BiMap<Long, Long> socketIDToWorkerID;
    protected BiMap<Long, Long> workerIDToSocketID;
    protected long controlNodeID = 0L;
    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected Iterator<? extends A> initialAxioms;

    protected AcknowledgementEventManager acknowledgementEventManager;

    protected boolean allConnectionsEstablished = false;
    protected AtomicInteger initializedWorkers = new AtomicInteger(0);
    protected AtomicInteger receivedClosureResults = new AtomicInteger(0);

    protected AtomicInteger saturationStage = new AtomicInteger(0);
    protected AtomicInteger sumOfAllReceivedAxioms = new AtomicInteger(0);
    protected AtomicInteger sumOfAllSentAxioms = new AtomicInteger(0);

    protected DistributedSaturationConfiguration config;

    protected NIO2NetworkingLoop networkingLoop;

    public ControlNodeCommunicationChannel(List<DistributedWorkerModel<C, A, T>> workers,
                                           WorkloadDistributor<C, A, T> workloadDistributor,
                                           Iterator<? extends A> initialAxioms,
                                           DistributedSaturationConfiguration config,
                                           NIO2NetworkingLoop networkingLoop) {
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;
        this.config = config;
        this.networkingLoop = networkingLoop;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = Maps.synchronizedBiMap(HashBiMap.create());
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();

        this.workerIDToWorker = new ConcurrentHashMap<>();
        workers.forEach(p -> workerIDToWorker.put(p.getID(), p));
        acknowledgementEventManager = new AcknowledgementEventManager();
        networkingComponent = networkingLoop.getNetworkingComponent();
    }

    public void initializeConnectionToWorkerServers() {
        workers.stream().map(p -> new WorkerConnectionModel(p.getServerData(), p))
                .forEach(s -> {
                    try {
                        networkingComponent.connectToServer(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void addInitialAxiomsToToDoQueue() {
        networkingLoop.addToToDoQueue(initialAxioms);
    }

    public void distributeAxiom(A axiom) {
        workloadDistributor.getRelevantWorkerIDsForAxiom(axiom)
                .forEach(receiverWorkerID -> {
                    sumOfAllSentAxioms.incrementAndGet();
                    networkingLoop.sendMessage(workerIDToSocketID.get(receiverWorkerID), axiom);
                });
    }

    public void terminate() {
        networkingComponent.terminate();
    }

    public void broadcast(SaturationStatusMessage statusMessage, Runnable onAcknowledgement) {
        for (Long socketID : this.socketIDToWorkerID.keySet()) {
            StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, statusMessage);
            send(socketID, stateInfoMessage, onAcknowledgement);
        }
    }

    public void acknowledgeMessage(long receiverWorkerID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(controlNodeID, messageID);
        long receiverSocketID = workerIDToSocketID.get(receiverWorkerID);
        send(receiverSocketID, ack);
    }

    public void send(long receiverSocketID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, status);
        send(receiverSocketID, stateInfoMessage, onAcknowledgement);
    }

    public void send(long receiverSocketID, MessageModel<C, A, T> message, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(message.getMessageID(), onAcknowledgement);
        send(receiverSocketID, message);
    }

    public void send(long receiverSocketID, Serializable message) {
        networkingLoop.sendMessage(receiverSocketID, message);
    }

    public void requestAxiomCountsFromAllWorkers() {
        this.saturationStage.incrementAndGet();
        for (Long socketID : this.socketIDToWorkerID.keySet()) {
            RequestAxiomMessageCount requestAxiomMessageCount = new RequestAxiomMessageCount(controlNodeID,
                    this.saturationStage.get());
            send(socketID, requestAxiomMessageCount);
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

    private class WorkerConnectionModel extends ConnectionModel {

        private final DistributedWorkerModel<C, A, T> workerModel;

        public WorkerConnectionModel(ServerData serverData,
                                     DistributedWorkerModel<C, A, T> workerModel) {
            super(serverData, (socketID, message) -> {
                networkingLoop.onNewMessageReceived(message);
            });
            this.workerModel = workerModel;
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Connection established to worker server " + workerModel.getID() + ".");

            // get worker ID to socket ID mapping
            socketIDToWorkerID.put(socketManager.getSocketID(), workerModel.getID());
            if (socketIDToWorkerID.size() == ControlNodeCommunicationChannel.this.workers.size()) {
                allConnectionsEstablished = true;
                networkingComponent.closeServerSockets();
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
        }


    }

}
