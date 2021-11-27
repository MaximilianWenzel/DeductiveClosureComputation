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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class PartitionNodeCommunicationChannel implements SaturationCommunicationChannel {

    private final int portToListen;
    private long partitionID = -1L;
    private List<DistributedPartitionModel> partitions;
    private NetworkingComponent networkingComponent;
    private WorkloadDistributor workloadDistributor;

    private BiMap<Long, Long> socketIDToPartitionID;
    private BiMap<Long, Long> partitionIDToSocketID;
    private BlockingDeque<MessageModel> receivedMessages = new LinkedBlockingDeque<>();

    private int maxNumAxiomsToBufferBeforeSending;
    private Map<Long, List<Object>> partitionIDToBufferedAxioms = new HashMap<>();

    private long controlNodeSocketID = -1L;
    private boolean allConnectionsEstablished = false;

    public PartitionNodeCommunicationChannel(int portToListen,
                                             int maxNumAxiomsToBufferBeforeSending) {
        init();
        this.portToListen = portToListen;
        this.maxNumAxiomsToBufferBeforeSending = maxNumAxiomsToBufferBeforeSending;
    }

    private void init() {
        networkingComponent = new NetworkingComponent(
                new MessageProcessorImpl(),
                Collections.singletonList(new PartitionServerPortListener(portToListen)),
                Collections.emptyList());
        this.socketIDToPartitionID = HashBiMap.create();
        this.partitionIDToSocketID = this.socketIDToPartitionID.inverse();
    }

    public void initializePartition(InitializePartitionMessage message) {
        this.partitionID = message.getPartitionID();
        this.workloadDistributor = message.getWorkloadDistributor();
        this.partitions = message.getPartitions();
        this.receivedMessages.add(new SaturationAxiomsMessage(message.getSenderID(), message.getInitialAxioms()));

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
        return this.receivedMessages.isEmpty();
    }

    public void distributeAxiom(Object axiom) {
        List<Long> partitionIDs = workloadDistributor.getRelevantPartitionIDsForAxiom(axiom);

        for (Long receiverPartitionID : partitionIDs) {
            List<Object> bufferedAxioms = this.partitionIDToBufferedAxioms.computeIfAbsent(receiverPartitionID, p -> new ArrayList<>());
            bufferedAxioms.add(axiom);

            if (bufferedAxioms.size() == maxNumAxiomsToBufferBeforeSending) {
                if (receiverPartitionID != this.partitionID) {
                    sendAxioms(receiverPartitionID, bufferedAxioms);
                }
                this.partitionIDToBufferedAxioms.remove(receiverPartitionID);
            }
        }
        // add axioms from this partition directly to the queue
        List<Object> bufferedAxioms = this.partitionIDToBufferedAxioms.getOrDefault(this.partitionID, Collections.emptyList());
        if (!bufferedAxioms.isEmpty()) {
            receivedMessages.add(new SaturationAxiomsMessage(this.partitionID, bufferedAxioms));
        }
    }

    public void sendAllBufferedAxioms() {
        for (Map.Entry<Long, List<Object>> partitionIDToBufferedAxioms : this.partitionIDToBufferedAxioms.entrySet()) {
            sendAxioms(partitionIDToBufferedAxioms.getKey(), partitionIDToBufferedAxioms.getValue());
        }
        this.partitionIDToBufferedAxioms.clear();
    }

    private void sendAxioms(long receiverPartitionID, List<Object> axioms) {
        SaturationAxiomsMessage saturationAxiomsMessage = new SaturationAxiomsMessage(partitionID, axioms);
        MessageEnvelope messageEnvelope = new MessageEnvelope(receiverPartitionID, saturationAxiomsMessage);
        networkingComponent.sendMessage(messageEnvelope);
    }

    public List<DistributedPartitionModel> getPartitions() {
        return this.partitions;
    }

    private class MessageProcessorImpl implements MessageProcessor {
        @Override
        public void process(MessageEnvelope messageEnvelope) {
            if (!(messageEnvelope.getMessage() instanceof MessageModel)) {
                throw new MessageProtocolViolationException();
            }
            MessageModel messageModel = (MessageModel) messageEnvelope.getMessage();

            if (!allConnectionsEstablished) {
                // get partition ID / control node ID to socket ID mapping
                socketIDToPartitionID.put(messageEnvelope.getSocketID(), messageModel.getSenderID());
                if (partitions != null && partitions.size() == socketIDToPartitionID.size()) {
                    //  if all connections (i.e., # partitions - 1 + single control node) are established
                    allConnectionsEstablished = true;
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
            send(socketManager.getSocketID(), SaturationStatusMessage.PARTITION_CLIENT_HELLO);
        }
    }

    private class PartitionServerPortListener extends PortListener {

        public PartitionServerPortListener(int port) {
            super(port);
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            send(socketManager.getSocketID(), SaturationStatusMessage.PARTITION_SERVER_HELLO);
        }
    }

}
