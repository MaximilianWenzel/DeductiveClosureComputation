package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.*;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.messages.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class WorkerNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> implements SaturationCommunicationChannel {

    private final Logger log = ConsoleUtils.getLogger();

    private final int portToListen;
    private long workerID = -1L;
    private List<DistributedWorkerModel<C, A, T>> workers;
    private NetworkingComponent networkingComponent;
    private WorkloadDistributor workloadDistributor;

    private BiMap<Long, Long> socketIDToWorkerID;
    private BiMap<Long, Long> workerIDToSocketID;
    private BlockingDeque<Object> toDo = new LinkedBlockingDeque<>();

    private final int maxNumAxiomsToBufferBeforeSending;
    private final Map<Long, List<Serializable>> workerIDToBufferedAxioms = new HashMap<>();

    private long controlNodeSocketID = -1L;
    private boolean allConnectionsEstablished = false;

    private AcknowledgementEventManager acknowledgementEventManager;
    private long initializationMessageID = -1;
    private final AtomicLong distributedAxiomMessages = new AtomicLong(0);
    private final AtomicLong acknowledgedAxiomMessages = new AtomicLong(0);
    private final AtomicLong establishedConnections = new AtomicLong(0);


    private SaturationAxiomsMessage<C, A, T> initialAxioms;

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

        networkingComponent = new NetworkingComponent(
                new MessageProcessorImpl(),
                Collections.singletonList(new WorkerServerPortListener(portToListen)),
                Collections.emptyList());
        networkingComponent.startNIOThread();
    }


    public void connectToWorkerServers() {
        // connect to all worker nodes with a higher worker ID
        for (DistributedWorkerModel workerModel : this.workers) {
            if (workerModel.getID() > this.workerID) {
                try {
                    WorkerServerConnector workerServerConnector = new WorkerServerConnector(workerModel.getServerData());
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

    public void sendToControlNode(C closure) {
        // generate closure
        SaturationAxiomsMessage<C, A, T> saturationAxiomsMessage = new SaturationAxiomsMessage<>(workerID, closure.getClosureResults());
        MessageEnvelope messageEnvelope = new MessageEnvelope(controlNodeSocketID, saturationAxiomsMessage);
        networkingComponent.sendMessage(messageEnvelope);
    }

    public void send(long workerID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        long receiverSocketID = workerIDToSocketID.get(workerID);
        StateInfoMessage stateInfoMessage = new StateInfoMessage(this.workerID, status);
        send(receiverSocketID, stateInfoMessage, onAcknowledgement);
    }

    private void send(long receiverSocketID, MessageModel messageModel, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(messageModel.getMessageID(), onAcknowledgement);
        MessageEnvelope messageEnvelope = new MessageEnvelope(receiverSocketID, messageModel);
        networkingComponent.sendMessage(messageEnvelope);
    }

    private void send(long receiverSocketID, MessageModel messageModel) {
        MessageEnvelope messageEnvelope = new MessageEnvelope(receiverSocketID, messageModel);
        networkingComponent.sendMessage(messageEnvelope);
    }

    @Override
    public Object read() throws InterruptedException {
        return toDo.take();
    }

    @Override
    public boolean hasMoreMessagesToReadWriteOrToBeAcknowledged() {
        return !this.toDo.isEmpty()
                || distributedAxiomMessages.get() != acknowledgedAxiomMessages.get()
                || networkingComponent.socketsCurrentlyReadMessages();
    }

    @Override
    public void terminate() {
        this.networkingComponent.terminate();
    }

    public void distributeAxiom(Serializable axiom) {
        List<Long> workerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(axiom);

        for (Long receiverWorkerID : workerIDs) {
            if (receiverWorkerID != this.workerID) {
                /*
                List<Serializable> bufferedAxioms = this.workerIDToBufferedAxioms.computeIfAbsent(receiverWorkerID, p -> new ArrayList<>());
                bufferedAxioms.add(axiom);
                if (bufferedAxioms.size() == maxNumAxiomsToBufferBeforeSending) {
                    sendAxioms(receiverWorkerID, bufferedAxioms);
                    this.workerIDToBufferedAxioms.remove(receiverWorkerID);
                }

                 */
                sendAxioms(receiverWorkerID, Collections.singletonList(axiom));
            } else {
                // add axioms from this worker directly to the queue
                toDo.add(axiom);
            }
        }
    }

    /**
     * Indicates if at least one axiom has been transmitted.
     */
    /*
    public boolean sendAllBufferedAxioms() {
        boolean axiomTransmitted = false;
        for (Map.Entry<Long, List<Serializable>> workerIDToBufferedAxioms : this.workerIDToBufferedAxioms.entrySet()) {
            sendAxioms(workerIDToBufferedAxioms.getKey(), workerIDToBufferedAxioms.getValue());
            axiomTransmitted = true;
        }
        this.workerIDToBufferedAxioms.clear();
        return axiomTransmitted;
    }

     */

    private void sendAxioms(long receiverWorkerID, List<? extends Serializable> axioms) {
        if (axioms.isEmpty()) {
            return;
        }
        SaturationAxiomsMessage saturationAxiomsMessage = new SaturationAxiomsMessage(workerID, axioms);
        Long socketID = this.workerIDToSocketID.get(receiverWorkerID);

        if (socketID == null) {
            log.warning("Worker " + receiverWorkerID + " has for worker " + this.workerID + " no socket ID assigned.");
        }

        distributedAxiomMessages.getAndIncrement();
        send(socketID, saturationAxiomsMessage, new Runnable() {
            @Override
            public void run() {
                acknowledgedAxiomMessages.getAndIncrement();
            }
        });
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

    private class MessageProcessorImpl implements MessageProcessor {
        @Override
        public void process(MessageEnvelope messageEnvelope) {
            if (!(messageEnvelope.getMessage() instanceof MessageModel)) {
                throw new MessageProtocolViolationException();
            }
            MessageModel messageModel = (MessageModel) messageEnvelope.getMessage();

            if (!allConnectionsEstablished) {
                // get worker ID / control node ID to socket ID mapping
                socketIDToWorkerID.put(messageEnvelope.getSocketID(), messageModel.getSenderID());
                if (workers != null && workers.size() == socketIDToWorkerID.size()) {
                    //  if all connections (i.e., # workers - 1 + single control node) are established
                    allConnectionsEstablished = true;
                }

                if (messageModel instanceof InitializeWorkerMessage) {
                    // first message from control node
                    WorkerNodeCommunicationChannel.this.controlNodeSocketID = messageEnvelope.getSocketID();
                    initializationMessageID = messageModel.getMessageID();
                }
            }

            WorkerNodeCommunicationChannel.this.toDo.add(messageEnvelope.getMessage());
        }
    }

    private class WorkerServerConnector extends ServerConnector {

        public WorkerServerConnector(ServerData serverData) {
            super(serverData);
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            try {
                log.info("Connection to worker server established: " + socketManager.getSocketChannel().getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (WorkerNodeCommunicationChannel.this.workerID == -1) {
                // worker not yet initialized
                return;
            }

            StateInfoMessage stateInfoMessage = new StateInfoMessage(WorkerNodeCommunicationChannel.this.workerID, SaturationStatusMessage.WORKER_CLIENT_HELLO);
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
            super(port);
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            try {
                log.info("Client connected to worker: " + socketManager.getSocketChannel().getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (WorkerNodeCommunicationChannel.this.workerID == -1) {
                // worker not yet initialized
                return;
            }
            StateInfoMessage stateInfoMessage = new StateInfoMessage(WorkerNodeCommunicationChannel.this.workerID, SaturationStatusMessage.WORKER_SERVER_HELLO);
            send(socketManager.getSocketID(), stateInfoMessage,
                    new Runnable() {
                        @Override
                        public void run() {
                            WorkerNodeCommunicationChannel.this.establishedConnections.getAndIncrement();
                        }
                    });
        }
    }

    public boolean allConnectionsEstablished() {
        // # of all other workers
        return this.establishedConnections.get() == this.workers.size() - 1;
    }
}
