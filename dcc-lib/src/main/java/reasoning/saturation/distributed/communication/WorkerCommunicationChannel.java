package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.ConnectionModel;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.*;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class WorkerCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final Logger log = ConsoleUtils.getLogger();

    private final ServerData serverData;
    private final AtomicInteger sentAxiomMessages = new AtomicInteger(0);
    private final AtomicInteger receivedAxiomMessages = new AtomicInteger(0);

    private NetworkingComponent networkingComponent;

    private long workerID = -1L;
    private List<DistributedWorkerModel<C, A, T>> workers;
    private WorkloadDistributor<C, A, T> workloadDistributor;

    private BiMap<Long, Long> socketIDToWorkerID;
    private BiMap<Long, Long> workerIDToSocketID;

    private long controlNodeSocketID = -1L;
    private boolean allConnectionsEstablished = false;
    private AcknowledgementEventManager acknowledgementEventManager;
    private List<A> initialAxioms;
    private AtomicInteger saturationStage = new AtomicInteger(0);

    private SaturationConfiguration config;
    private WorkerStatistics stats;

    private NIO2NetworkingLoop networkingLoop;

    public WorkerCommunicationChannel(ServerData serverData, NIO2NetworkingLoop networkingLoop) {
        this.serverData = serverData;
        this.networkingLoop = networkingLoop;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = Maps.synchronizedBiMap(HashBiMap.create());
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();
        this.acknowledgementEventManager = new AcknowledgementEventManager();
        this.networkingComponent = networkingLoop.getNetworkingComponent();
        try {
            networkingComponent.listenOnPort(new WorkerServerConnectionModel(serverData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        networkingComponent.closeAllSockets();

        socketIDToWorkerID.clear();
        sentAxiomMessages.set(0);
        receivedAxiomMessages.set(0);
        saturationStage.set(0);
        workerID = -1L;
        workers = null;
        workloadDistributor = null;
        allConnectionsEstablished = false;
        acknowledgementEventManager = new AcknowledgementEventManager();
        this.config = null;
        this.stats = new WorkerStatistics();
        try {
            networkingComponent.listenOnPort(new WorkerServerConnectionModel(serverData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID
        for (DistributedWorkerModel<?, ?, ?> workerModel : this.workers) {
            if (workerModel.getID() > this.workerID) {
                try {
                    WorkerConnectionModel workerConnectionEstablishmentListener = new WorkerConnectionModel(
                            workerModel.getServerData(), workerModel.getID());
                    networkingComponent.connectToServer(workerConnectionEstablishmentListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void acknowledgeMessage(long receiverID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(this.workerID, messageID);
        long receiverSocketID = workerIDToSocketID.get(receiverID);
        send(receiverSocketID, ack);
    }

    public void distributeInferences(Stream<A> inferenceStream) {
        if (config.collectWorkerNodeStatistics()) {
            stats.getNumberOfDerivedInferences().incrementAndGet();
            stats.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }
        inferenceStream.forEach(inference -> {
            workloadDistributor.getRelevantWorkerIDsForAxiom(inference)
                    .forEach(receiverWorkerID -> {
                        if (receiverWorkerID != this.workerID) {
                            sentAxiomMessages.incrementAndGet();
                            networkingLoop.sendMessage(workerIDToSocketID.get(receiverWorkerID), inference);
                        } else {
                            // add axioms from this worker directly to the queue
                            networkingLoop.addToToDoQueue(inference);
                        }
                    });
        });
        if (config.collectWorkerNodeStatistics()) {
            stats.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }
    }

    public void sendAxiomCountToControlNode() {
        AxiomCount axiomCount = new AxiomCount(
                this.workerID,
                this.saturationStage.get(),
                this.sentAxiomMessages.getAndUpdate(count -> 0),
                this.receivedAxiomMessages.getAndUpdate(count -> 0));
        send(controlNodeSocketID, axiomCount);
    }

    public void sendToControlNode(C closure) {
        // generate closure
        Collection<A> closureResults = closure.getClosureResults();
        closureResults.iterator().forEachRemaining(axiom -> {
            networkingLoop.sendMessage(controlNodeSocketID, axiom);
        });
    }

    public void sendToControlNode(WorkerStatistics stats) {
        send(controlNodeSocketID, new StatisticsMessage(this.workerID, stats));
    }


    public void send(long workerID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        long receiverSocketID = workerIDToSocketID.get(workerID);
        StateInfoMessage stateInfoMessage = new StateInfoMessage(this.workerID, status);
        send(receiverSocketID, stateInfoMessage, onAcknowledgement);
    }

    public void send(long receiverSocketID, MessageModel messageModel, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(messageModel.getMessageID(), onAcknowledgement);
        send(receiverSocketID, messageModel);
    }

    public void send(long receiverSocketID, Serializable message) {
        networkingLoop.sendMessage(receiverSocketID, message);
    }

    public void terminateNow() {
        this.networkingComponent.terminate();
    }

    private MessageEnvelope getAxiomMessageEnvelope(long receiverWorkerID, A axiom) {
        sentAxiomMessages.getAndIncrement();
        if (config.collectWorkerNodeStatistics()) {
            stats.getNumberOfSentAxioms().getAndIncrement();
        }
        Long socketID = this.workerIDToSocketID.get(receiverWorkerID);
        if (socketID == null) {
            log.warning("Worker " + receiverWorkerID + " has for worker " + this.workerID + " no socket ID assigned.");
        }

        return new MessageEnvelope(socketID, axiom);
    }

    public List<DistributedWorkerModel<C, A, T>> getWorkers() {
        return this.workers;
    }

    public void setWorkers(List<DistributedWorkerModel<C, A, T>> workers) {
        this.workers = workers;
    }

    public void addInitialAxiomsToQueue() {
        if (initialAxioms != null) {
            networkingLoop.addToToDoQueue(initialAxioms.iterator());
            this.initialAxioms = null;
        }
    }

    public void setWorkerID(long workerID) {
        this.workerID = workerID;
    }

    public void setWorkloadDistributor(WorkloadDistributor workloadDistributor) {
        this.workloadDistributor = workloadDistributor;
    }

    public void setInitialAxioms(List<A> initialAxioms) {
        this.initialAxioms = initialAxioms;
    }

    public void addAxiomsToQueue(List<A> axioms) {
        this.networkingLoop.addToToDoQueue(axioms.iterator());
    }

    public AcknowledgementEventManager getAcknowledgementEventManager() {
        return acknowledgementEventManager;
    }

    public long getControlNodeID() {
        return 0;
    }

    public boolean allConnectionsEstablished() {
        return allConnectionsEstablished;
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


    private class MessageHandlerImpl implements MessageHandler {
        @Override
        public void process(long socketID, Object message) {
            if (!(message instanceof MessageModel)) {
                // must be an axiom
                receivedAxiomMessages.getAndIncrement();
                if (config.collectWorkerNodeStatistics()) {
                    stats.getNumberOfReceivedAxioms().incrementAndGet();
                }
                networkingLoop.onNewMessageReceived(message);
                return;
            }
            MessageModel<C, A, T> messageModel = (MessageModel<C, A, T>) message;

            if (!allConnectionsEstablished) {
                // get worker ID / control node ID to socket ID mapping
                socketIDToWorkerID.put(socketID, messageModel.getSenderID());
                if (workers != null && workers.size() == socketIDToWorkerID.size()) {
                    //  if all connections (i.e., # workers - 1 + single control node) are established
                    allConnectionsEstablished = true;

                    // stop server socket
                    networkingComponent.closeServerSockets();
                }

                if (messageModel instanceof InitializeWorkerMessage) {
                    // first message from control node
                    WorkerCommunicationChannel.this.controlNodeSocketID = socketID;
                }
            }
            networkingLoop.onNewMessageReceived(message);
        }
    }

    private class WorkerConnectionModel extends ConnectionModel {

        long workerID;

        public WorkerConnectionModel(ServerData serverData, long workerID) {
            super(serverData, new MessageHandlerImpl());
            this.workerID = workerID;
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Connection to worker server established.");
            socketIDToWorkerID.put(socketManager.getSocketID(), workerID);

            StateInfoMessage stateInfoMessage = new StateInfoMessage(WorkerCommunicationChannel.this.workerID,
                    SaturationStatusMessage.WORKER_CLIENT_HELLO);
            send(socketManager.getSocketID(), stateInfoMessage);
        }
    }

    private class WorkerServerConnectionModel extends ConnectionModel {

        public WorkerServerConnectionModel(ServerData serverData) {
            super(serverData, new MessageHandlerImpl());
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Client connected to worker.");
            // client will send WORKER_CLIENT_HELLO message
            StateInfoMessage stateInfoMessage = new StateInfoMessage(WorkerCommunicationChannel.this.workerID,
                    SaturationStatusMessage.WORKER_SERVER_HELLO);
            send(socketManager.getSocketID(), stateInfoMessage);
        }
    }
}
