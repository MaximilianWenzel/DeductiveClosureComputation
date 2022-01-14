package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.NIO2NetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.*;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;
import util.QueueFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class WorkerNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements SaturationCommunicationChannel {

    private final Logger log = ConsoleUtils.getLogger();

    private final ServerData serverData;
    private final AtomicInteger sentAxiomMessages = new AtomicInteger(0);
    private final AtomicInteger receivedAxiomMessages = new AtomicInteger(0);
    private final AtomicLong establishedConnections = new AtomicLong(0);
    private BlockingQueue<Object> toDo = QueueFactory.createDistributedSaturationToDo();

    private NetworkingComponent networkingComponent;
    private Runnable oneNewMessageReceived;
    private BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent;
    private ExecutorService threadPool;

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

    public WorkerNodeCommunicationChannel(ServerData serverData, ExecutorService threadPool,
                                          BlockingQueue<MessageEnvelope> messageThatCouldNotBeSent,
                                          Runnable onNewMessageReceived) {
        this.serverData = serverData;
        this.threadPool = threadPool;
        this.messagesThatCouldNotBeSent = messageThatCouldNotBeSent;
        this.oneNewMessageReceived = onNewMessageReceived;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = Maps.synchronizedBiMap(HashBiMap.create());
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();
        this.acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NIO2NetworkingComponent(
                Collections.singletonList(new WorkerServerConnectionEstablishmentListener(serverData)),
                Collections.emptyList(),
                messageEnvelope -> messagesThatCouldNotBeSent.add(messageEnvelope),
                threadPool
        );
    }

    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID
        for (DistributedWorkerModel<?, ?, ?> workerModel : this.workers) {
            if (workerModel.getID() > this.workerID) {
                try {
                    WorkerConnectionEstablishmentListener workerConnectionEstablishmentListener = new WorkerConnectionEstablishmentListener(
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
                    toDo.add(inference);
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

    public void send(long receiverSocketID, MessageModel messageModel, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(messageModel.getMessageID(), onAcknowledgement);
        networkingComponent.sendMessage(receiverSocketID, messageModel);
    }

    public void send(long receiverSocketID, Serializable message) {
        networkingComponent.sendMessage(receiverSocketID, message);
    }

    @Override
    public Object removeNextMessage() {
        return toDo.poll();
    }

    @Override
    public Object pollNextMessage() {
        return toDo.poll();
    }


    @Override
    public boolean hasMoreMessages() {
        return !this.toDo.isEmpty();
    }

    @Override
    public void terminateNow() {
        this.networkingComponent.terminate();
    }

    @Override
    public void terminateAfterAllMessagesHaveBeenSent() {
        networkingComponent.terminateAfterAllMessagesHaveBeenSent();
    }


    public void sendAxiom(long receiverWorkerID, A axiom) {
        sentAxiomMessages.getAndIncrement();
        if (config.collectWorkerNodeStatistics()) {
            stats.getNumberOfSentAxioms().getAndIncrement();
        }

        Long socketID = this.workerIDToSocketID.get(receiverWorkerID);

        if (socketID == null) {
            log.warning("Worker " + receiverWorkerID + " has for worker " + this.workerID + " no socket ID assigned.");
        }

        send(socketID, axiom);
    }

    public BlockingQueue<Object> getToDo() {
        return toDo;
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
                try {
                    toDo.put(a);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        try {
            for (A a : axioms) {
                this.toDo.put(a);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
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


    private class MessageHandlerImpl implements MessageHandler {
        @Override
        public void process(long socketID, Object message) {
            if (!(message instanceof MessageModel)) {
                // must be an axiom
                receivedAxiomMessages.getAndIncrement();
                if (config.collectWorkerNodeStatistics()) {
                    stats.getNumberOfReceivedAxioms().incrementAndGet();
                }
                toDo.add(message);
                oneNewMessageReceived.run();
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
            toDo.add(message);
            oneNewMessageReceived.run();
        }
    }

    private class WorkerConnectionEstablishmentListener extends ConnectionEstablishmentListener {

        long workerID;

        public WorkerConnectionEstablishmentListener(ServerData serverData, long workerID) {
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

    private class WorkerServerConnectionEstablishmentListener extends ConnectionEstablishmentListener {

        public WorkerServerConnectionEstablishmentListener(ServerData serverData) {
            super(serverData, new MessageHandlerImpl());
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
