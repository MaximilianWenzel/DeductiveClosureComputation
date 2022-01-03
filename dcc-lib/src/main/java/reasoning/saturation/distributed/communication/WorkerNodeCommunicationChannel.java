package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import data.Closure;
import data.DefaultToDo;
import enums.SaturationStatusMessage;
import networking.NIO2NetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class WorkerNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements SaturationCommunicationChannel {

    private final Logger log = ConsoleUtils.getLogger();

    private final int portToListen;
    private final BlockingQueue<Object> toDo = new DefaultToDo<>();
    private final AtomicInteger sentAxiomMessages = new AtomicInteger(0);
    private final AtomicInteger receivedAxiomMessages = new AtomicInteger(0);

    private final AtomicLong establishedConnections = new AtomicLong(0);
    private NetworkingComponent networkingComponent;
    private long workerID = -1L;
    private List<DistributedWorkerModel<C, A, T>> workers;
    private WorkloadDistributor<C, A, T> workloadDistributor;

    private BiMap<Long, Long> socketIDToWorkerID;
    private BiMap<Long, Long> workerIDToSocketID;

    private long controlNodeSocketID = -1L;
    private boolean allConnectionsEstablished = false;
    private AcknowledgementEventManager acknowledgementEventManager;
    private long initializationMessageID = -1;
    private List<A> initialAxioms;
    private AtomicInteger saturationStage = new AtomicInteger(0);

    private SaturationConfiguration config;
    private WorkerStatistics stats;

    public WorkerNodeCommunicationChannel(int portToListen) {
        this.portToListen = portToListen;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = Maps.synchronizedBiMap(HashBiMap.create());
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();
        this.acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NIO2NetworkingComponent(
                Collections.singletonList(new WorkerServerPortListener(portToListen)),
                Collections.emptyList()
        );
    }


    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID
        for (DistributedWorkerModel workerModel : this.workers) {
            if (workerModel.getID() > this.workerID) {
                try {
                    WorkerServerConnector workerServerConnector = new WorkerServerConnector(
                            workerModel.getServerData(), workerModel.getID());
                    networkingComponent.connectToServer(workerServerConnector);
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

    public void sendToControlNode(SaturationStatusMessage status, Runnable onAcknowledgement) {
        send(0, status, onAcknowledgement);
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
        closureResults.forEach(axiom -> {
            send(controlNodeSocketID, axiom);
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

    private void send(long receiverSocketID, MessageModel messageModel, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(messageModel.getMessageID(), onAcknowledgement);
        networkingComponent.sendMessage(receiverSocketID, messageModel);
    }

    private void send(long receiverSocketID, Serializable message) {
        networkingComponent.sendMessage(receiverSocketID, message);
    }

    @Override
    public Object read() throws InterruptedException {
        return toDo.take();
    }

    @Override
    public boolean hasMoreMessages() {
        return !this.toDo.isEmpty()
                || networkingComponent.socketsCurrentlyReadMessages();
    }

    public void distributeAxiom(A axiom) {
        List<Long> workerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(axiom);

        for (Long receiverWorkerID : workerIDs) {
            if (receiverWorkerID != this.workerID) {
                sentAxiomMessages.getAndIncrement();
                sendAxiom(receiverWorkerID, axiom);
            } else {
                // add axioms from this worker directly to the queue
                toDo.offer(axiom);
            }
        }
    }

    @Override
    public void terminate() {
        this.networkingComponent.terminate();
    }


    private void sendAxiom(long receiverWorkerID, A axiom) {
        if (config.collectStatistics()) {
            stats.getNumberOfSentAxioms().getAndIncrement();
        }

        Long socketID = this.workerIDToSocketID.get(receiverWorkerID);

        if (socketID == null) {
            log.warning("Worker " + receiverWorkerID + " has for worker " + this.workerID + " no socket ID assigned.");
        }

        send(socketID, axiom);
    }

    public List<DistributedWorkerModel<C, A, T>> getWorkers() {
        return this.workers;
    }

    public void setWorkers(List<DistributedWorkerModel<C, A, T>> workers) {
        this.workers = workers;
    }

    public void addInitialAxiomsToQueue() {
        if (initialAxioms != null) {
            initialAxioms.forEach(a -> {
                if (!toDo.offer(a)) {
                    throw new IllegalStateException(
                            "To-Do capacity is too small for initial axioms. Capacity: " + toDo.size());
                }
            });

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
        for (A a : axioms) {
            this.toDo.offer(a);
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

    private class MessageHandlerImpl implements MessageHandler {
        @Override
        public void process(long socketID, Object message) {
            if (!(message instanceof MessageModel)) {
                // must be an axiom
                receivedAxiomMessages.getAndIncrement();
                if (config.collectStatistics()) {
                    stats.getNumberOfReceivedAxioms().incrementAndGet();
                }
                toDo.offer(message);
                return;
            }
            MessageModel messageModel = (MessageModel) message;

            if (!allConnectionsEstablished) {
                // get worker ID / control node ID to socket ID mapping
                socketIDToWorkerID.put(socketID, messageModel.getSenderID());
                if (workers != null && workers.size() == socketIDToWorkerID.size()) {
                    //  if all connections (i.e., # workers - 1 + single control node) are established
                    allConnectionsEstablished = true;
                }

                if (messageModel instanceof InitializeWorkerMessage) {
                    // first message from control node
                    WorkerNodeCommunicationChannel.this.controlNodeSocketID = socketID;
                    initializationMessageID = messageModel.getMessageID();
                }
            }
            toDo.offer(message);
        }
    }

    private class WorkerServerConnector extends ServerConnector {

        long workerID;

        public WorkerServerConnector(ServerData serverData, long workerID) {
            super(serverData, new MessageHandlerImpl());
            this.workerID = workerID;
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Connection to worker server established.");
            socketIDToWorkerID.put(socketManager.getSocketID(), workerID);

            StateInfoMessage stateInfoMessage = new StateInfoMessage(WorkerNodeCommunicationChannel.this.workerID,
                    SaturationStatusMessage.WORKER_CLIENT_HELLO);
            send(socketManager.getSocketID(), stateInfoMessage,
                    new Runnable() {
                        @Override
                        public void run() {
                            WorkerNodeCommunicationChannel.this.establishedConnections.getAndIncrement();
                        }
                    });
        }
    }

    private class WorkerServerPortListener extends PortListener {

        public WorkerServerPortListener(int port) {
            super(port, new MessageHandlerImpl());
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Client connected to worker.");
            // client will send WORKER_CLIENT_HELLO message
            StateInfoMessage stateInfoMessage = new StateInfoMessage(WorkerNodeCommunicationChannel.this.workerID,
                    SaturationStatusMessage.WORKER_SERVER_HELLO);
            send(socketManager.getSocketID(), stateInfoMessage,
                    new Runnable() {
                        @Override
                        public void run() {
                            WorkerNodeCommunicationChannel.this.establishedConnections.getAndIncrement();
                        }
                    });
        }
    }
}
