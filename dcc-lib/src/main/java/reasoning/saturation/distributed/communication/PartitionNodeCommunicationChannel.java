package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.*;
import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.messages.InitializePartitionMessage;
import networking.messages.MessageModel;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.models.DistributedPartitionModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

public class PartitionNodeCommunicationChannel implements SaturationCommunicationChannel {

    private final Logger log = ConsoleUtils.getLogger();

    private final int portToListen;
    private long partitionID = -1L;
    private List<DistributedPartitionModel> partitions;
    private NetworkingComponent networkingComponent;
    private WorkloadDistributor workloadDistributor;

    private BiMap<Long, Long> socketIDToPartitionID;
    private BiMap<Long, Long> partitionIDToSocketID;
    private BlockingDeque<MessageModel> receivedMessages = new LinkedBlockingDeque<>();

    private int maxNumAxiomsToBufferBeforeSending;
    private Map<Long, List<Serializable>> partitionIDToBufferedAxioms = new HashMap<>();

    private long controlNodeSocketID = -1L;
    private boolean allConnectionsEstablished = false;


    private SaturationAxiomsMessage initialAxioms;

    public PartitionNodeCommunicationChannel(int portToListen,
                                             int maxNumAxiomsToBufferBeforeSending) {
        this.portToListen = portToListen;
        this.maxNumAxiomsToBufferBeforeSending = maxNumAxiomsToBufferBeforeSending;
        init();
    }

    private void init() {
        this.socketIDToPartitionID = HashBiMap.create();
        this.partitionIDToSocketID = this.socketIDToPartitionID.inverse();

        networkingComponent = new NetworkingComponent(
                new MessageProcessorImpl(),
                Collections.singletonList(new PartitionServerPortListener(portToListen)),
                Collections.emptyList());
        networkingComponent.startNIOThread();
    }


    public void connectToPartitionServers() {
        // connect to all partition nodes with a higher partition ID
        for (DistributedPartitionModel partitionModel : this.partitions) {
            if (partitionModel.getID() > this.partitionID) {
                try {
                    PartitionServerConnector partitionServerConnector = new PartitionServerConnector(partitionModel.getServerData());
                    networkingComponent.connectToServer(partitionServerConnector);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendToControlNode(SaturationStatusMessage status) {
        send(controlNodeSocketID, status);
    }

    public void sendToControlNode(Closure closure) {
        SaturationAxiomsMessage saturationAxiomsMessage = new SaturationAxiomsMessage(partitionID, closure);
        MessageEnvelope messageEnvelope = new MessageEnvelope(controlNodeSocketID, saturationAxiomsMessage);
        networkingComponent.sendMessage(messageEnvelope);
    }

    public void send(long receiverSocketID, SaturationStatusMessage status) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(partitionID, status);
        MessageEnvelope messageEnvelope = new MessageEnvelope(receiverSocketID, stateInfoMessage);
        networkingComponent.sendMessage(messageEnvelope);
    }

    @Override
    public MessageModel read() throws InterruptedException {
        return this.receivedMessages.take();
    }

    @Override
    public boolean hasMoreMessages() {
        return !this.receivedMessages.isEmpty() || networkingComponent.socketsCurrentlyReadMessages();
    }



    public void distributeAxiom(Serializable axiom) {
        List<Long> partitionIDs = workloadDistributor.getRelevantPartitionIDsForAxiom(axiom);

        for (Long receiverPartitionID : partitionIDs) {
            if (receiverPartitionID != this.partitionID) {
                List<Serializable> bufferedAxioms = this.partitionIDToBufferedAxioms.computeIfAbsent(receiverPartitionID, p -> new ArrayList<>());
                bufferedAxioms.add(axiom);
                if (bufferedAxioms.size() == maxNumAxiomsToBufferBeforeSending) {
                    sendAxioms(receiverPartitionID, bufferedAxioms);
                    this.partitionIDToBufferedAxioms.remove(receiverPartitionID);
                }
            } else {
                // add axioms from this partition directly to the queue
                // TODO inefficient to wrap object around single axiom
                receivedMessages.add(new SaturationAxiomsMessage(this.partitionID, Collections.singleton(axiom)));
            }
        }
    }

    public void sendAllBufferedAxioms() {
        for (Map.Entry<Long, List<Serializable>> partitionIDToBufferedAxioms : this.partitionIDToBufferedAxioms.entrySet()) {
            sendAxioms(partitionIDToBufferedAxioms.getKey(), partitionIDToBufferedAxioms.getValue());
        }
        this.partitionIDToBufferedAxioms.clear();
    }

    private void sendAxioms(long receiverPartitionID, List<? extends Serializable> axioms) {
        if (axioms.isEmpty()) {
            return;
        }
        SaturationAxiomsMessage saturationAxiomsMessage = new SaturationAxiomsMessage(partitionID, axioms);
        Long socketID = this.partitionIDToSocketID.get(receiverPartitionID);

        if (socketID == null) {
            log.warning("Partition " + receiverPartitionID + " has for partition " + this.partitionID + " no socket ID assigned.");
            // TODO ensure ID assignment before this method is called
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            socketID = this.partitionIDToSocketID.get(receiverPartitionID);
        }

        MessageEnvelope messageEnvelope = new MessageEnvelope(socketID, saturationAxiomsMessage);
        networkingComponent.sendMessage(messageEnvelope);
    }

    public List<DistributedPartitionModel> getPartitions() {
        return this.partitions;
    }

    public void setPartitions(List<DistributedPartitionModel> partitions) {
        this.partitions = partitions;
    }

    public void addInitialAxiomsToQueue() {
        if (initialAxioms != null) {
            this.receivedMessages.add(initialAxioms);
            this.initialAxioms = null;
        }
    }

    public void setPartitionID(long partitionID) {
        this.partitionID = partitionID;
    }

    public void setWorkloadDistributor(WorkloadDistributor workloadDistributor) {
        this.workloadDistributor = workloadDistributor;
    }

    public void setInitialAxioms(Collection<? extends Serializable> initialAxioms) {
        this.initialAxioms = new SaturationAxiomsMessage(controlNodeSocketID, initialAxioms);
    }

    public void addAxiomsToQueue(List<SaturationAxiomsMessage> saturationAxiomsMessages) {
        this.receivedMessages.addAll(saturationAxiomsMessages);
    }

    private class MessageProcessorImpl implements MessageProcessor {
        @Override
        public void process(MessageEnvelope messageEnvelope) {
            if (!(messageEnvelope.getMessage() instanceof MessageModel)) {
                throw new MessageProtocolViolationException();
            }
            MessageModel messageModel = (MessageModel) messageEnvelope.getMessage();

            // TODO add condition
            if (true) {
                // get partition ID / control node ID to socket ID mapping
                socketIDToPartitionID.put(messageEnvelope.getSocketID(), messageModel.getSenderID());
                if (partitions != null && partitions.size() == socketIDToPartitionID.size()) {
                    //  if all connections (i.e., # partitions - 1 + single control node) are established
                    allConnectionsEstablished = true;
                }

                if (messageModel instanceof InitializePartitionMessage) {
                    // first message from control node
                    PartitionNodeCommunicationChannel.this.controlNodeSocketID = messageEnvelope.getSocketID();
                }
            }

            PartitionNodeCommunicationChannel.this.receivedMessages.add((MessageModel) messageEnvelope.getMessage());
        }
    }

    private class PartitionServerConnector extends ServerConnector {

        public PartitionServerConnector(ServerData serverData) {
            super(serverData);
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            try {
                log.info("Connection to partition server established: " + socketManager.getSocketChannel().getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
            send(socketManager.getSocketID(), SaturationStatusMessage.PARTITION_CLIENT_HELLO);
        }
    }

    private class PartitionServerPortListener extends PortListener {

        public PartitionServerPortListener(int port) {
            super(port);
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            try {
                log.info("Client connected to partition: " + socketManager.getSocketChannel().getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
            send(socketManager.getSocketID(), SaturationStatusMessage.PARTITION_SERVER_HELLO);
        }
    }
}
