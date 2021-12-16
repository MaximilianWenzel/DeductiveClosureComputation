package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.NIONetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.messages.*;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class WorkerNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements SaturationCommunicationChannel {

    private final Logger log = ConsoleUtils.getLogger();

    private final int portToListen;
    private final Map<Long, List<A>> workerIDToBufferedAxioms = new HashMap<>();
    private final AtomicLong establishedConnections = new AtomicLong(0);
    private long workerID = -1L;
    private List<DistributedWorkerModel<C, A, T>> workers;
    private NIONetworkingComponent networkingComponent;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private BiMap<Long, Long> socketIDToWorkerID;
    private BiMap<Long, Long> workerIDToSocketID;
    private BlockingQueue<Object> toDo = new LinkedBlockingQueue<>();
    private long controlNodeSocketID = -1L;
    private boolean allConnectionsEstablished = false;
    private AcknowledgementEventManager acknowledgementEventManager;
    private long initializationMessageID = -1;
    private SaturationAxiomsMessage<C, A, T> initialAxioms;

    // TODO: let user define batch size for sending closure to control node
    private final int closureResultAxiomBatchSize = 1000;
    private final int maxNumAxiomsToBufferBeforeSending;

    private int saturationStage = 0;
    private AtomicInteger sentAxiomMessages = new AtomicInteger(0);
    private AtomicInteger receivedAxiomMessages = new AtomicInteger(0);

    public WorkerNodeCommunicationChannel(int portToListen,
                                          int maxNumAxiomsToBufferBeforeSending) {
        this.portToListen = portToListen;
        this.maxNumAxiomsToBufferBeforeSending = maxNumAxiomsToBufferBeforeSending;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = HashBiMap.create();
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();
        this.acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NIONetworkingComponent(
                Collections.singletonList(new WorkerServerPortListener(portToListen)),
                Collections.emptyList()
        );
        networkingComponent.startNIOThread();
    }


    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID
        for (DistributedWorkerModel workerModel : this.workers) {
            if (workerModel.getID() > this.workerID) {
                try {
                    WorkerServerConnector workerServerConnector = new WorkerServerConnector(
                            workerModel.getServerData());
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
        AxiomCount axiomCount = new AxiomCount(this.workerID, this.saturationStage, this.sentAxiomMessages.get(),
                this.receivedAxiomMessages.get());
        this.sentAxiomMessages.set(0);
        this.receivedAxiomMessages.set(0);
        send(controlNodeSocketID, axiomCount);
    }

    public void sendToControlNode(C closure) {
        // generate closure
        Collection<A> closureResults = closure.getClosureResults();

        if (closureResults.size() < this.maxNumAxiomsToBufferBeforeSending) {
            sendAxioms(getControlNodeID(), new ArrayList<>(closureResults));
        } else {
            // send results in smaller batches
            Iterator<A> closureResultsIt = closureResults.iterator();
            int counter = 0;
            List<A> batch = new ArrayList<>(closureResultAxiomBatchSize);
            while (closureResultsIt.hasNext()) {
                batch.add(closureResultsIt.next());
                counter++;
                if (counter > closureResultAxiomBatchSize) {
                    counter = 0;
                    sendAxioms(getControlNodeID(), batch);
                    batch = new ArrayList<>(closureResultAxiomBatchSize);
                }
            }
            sendAxioms(getControlNodeID(), batch);
        }
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

    private void send(long receiverSocketID, MessageModel messageModel) {
        networkingComponent.sendMessage(receiverSocketID, messageModel);
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
        // TODO: check if axiom has already been distributed
        List<Long> workerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(axiom);

        for (Long receiverWorkerID : workerIDs) {
            if (receiverWorkerID != this.workerID) {
                List<A> bufferedAxioms = this.workerIDToBufferedAxioms.computeIfAbsent(receiverWorkerID,
                        p -> new ArrayList<>(maxNumAxiomsToBufferBeforeSending));
                bufferedAxioms.add(axiom);
                if (bufferedAxioms.size() == maxNumAxiomsToBufferBeforeSending) {
                    sendAxioms(receiverWorkerID, bufferedAxioms);
                    bufferedAxioms.clear();
                }
            } else {
                // add axioms from this worker directly to the queue
                toDo.add(axiom);
            }
        }
    }

    @Override
    public void terminate() {
        this.networkingComponent.terminate();
    }


    /**
     * Indicates if at least one axiom has been transmitted.
     */
    public boolean sendAllBufferedAxioms() {
        boolean axiomTransmitted = false;
        for (Map.Entry<Long, List<A>> workerIDToBufferedAxioms : this.workerIDToBufferedAxioms.entrySet()) {
            List<A> bufferedAxioms = workerIDToBufferedAxioms.getValue();
            if (bufferedAxioms.isEmpty()) {
                continue;
            }
            sendAxioms(workerIDToBufferedAxioms.getKey(), bufferedAxioms);
            bufferedAxioms.clear();
            axiomTransmitted = true;
        }
        return axiomTransmitted;
    }

    private void sendAxioms(long receiverWorkerID, List<A> axioms) {
        if (axioms.isEmpty()) {
            return;
        }
        sentAxiomMessages.getAndIncrement();
        SaturationAxiomsMessage saturationAxiomsMessage = new SaturationAxiomsMessage(workerID, axioms);
        Long socketID = this.workerIDToSocketID.get(receiverWorkerID);

        if (socketID == null) {
            log.warning("Worker " + receiverWorkerID + " has for worker " + this.workerID + " no socket ID assigned.");
        }

        send(socketID, saturationAxiomsMessage);
    }

    public List<DistributedWorkerModel<C, A, T>> getWorkers() {
        return this.workers;
    }

    public void setWorkers(List<DistributedWorkerModel<C, A, T>> workers) {
        this.workers = workers;
    }

    public void addInitialAxiomsToQueue() {
        if (initialAxioms != null) {
            this.toDo.addAll(initialAxioms.getAxioms());
            this.initialAxioms = null;
        }
    }

    public void setWorkerID(long workerID) {
        this.workerID = workerID;
    }

    public void setWorkloadDistributor(WorkloadDistributor workloadDistributor) {
        this.workloadDistributor = workloadDistributor;
    }

    public void setInitialAxioms(Collection<A> initialAxioms) {
        this.initialAxioms = new SaturationAxiomsMessage<>(controlNodeSocketID, initialAxioms);
    }

    public void addAxiomsToQueue(List<SaturationAxiomsMessage<C, A, T>> saturationAxiomsMessages) {
        this.toDo.addAll(saturationAxiomsMessages);
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

    public void setSaturationStage(int saturationStage) {
        this.saturationStage = saturationStage;
    }

    private class MessageProcessorImpl implements MessageProcessor {
        @Override
        public void process(long socketID, Object message) {
            if (!(message instanceof MessageModel)) {
                throw new MessageProtocolViolationException();
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

            WorkerNodeCommunicationChannel.this.toDo.add(message);
        }
    }

    private class WorkerServerConnector extends ServerConnector {

        public WorkerServerConnector(ServerData serverData) {
            super(serverData, new MessageProcessorImpl());
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Connection to worker server established.");
            if (WorkerNodeCommunicationChannel.this.workerID == -1) {
                // worker not yet initialized
                return;
            }

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
            super(port, new MessageProcessorImpl());
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Client connected to worker.");
            if (WorkerNodeCommunicationChannel.this.workerID == -1) {
                // worker not yet initialized
                return;
            }
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
