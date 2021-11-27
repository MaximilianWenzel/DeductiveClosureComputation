package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import enums.SaturationStatusMessage;
import networking.*;
import networking.connectors.ServerConnector;
import networking.messages.InitializePartitionMessage;
import networking.messages.MessageModel;
import networking.messages.StateInfoMessage;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.models.DistributedPartitionModel;
import reasoning.saturation.workload.InitialAxiomsDistributor;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

public class ControlNodeCommunicationChannel implements SaturationCommunicationChannel {

    protected NetworkingComponent networkingComponent;
    protected List<DistributedPartitionModel> partitions;
    protected Map<Long, DistributedPartitionModel> partitionIDToPartition;
    protected BiMap<Long, Long> socketIDToPartitionID;
    protected BiMap<Long, Long> partitionIDToSocketID;
    protected long controlNodeID = 0L;
    protected BlockingDeque<MessageModel> receivedMessages = new LinkedBlockingDeque<>();
    protected WorkloadDistributor workloadDistributor;
    protected List<Object> initialAxioms;

    protected InitialAxiomsDistributor initialAxiomsDistributor;

    public ControlNodeCommunicationChannel(List<DistributedPartitionModel> partitions,
                                           WorkloadDistributor workloadDistributor,
                                           List<Object> initialAxioms) {
        this.partitions = partitions;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;
        init();
    }

    private void init() {
        this.socketIDToPartitionID = Maps.synchronizedBiMap(HashBiMap.create());
        this.partitionIDToSocketID = this.socketIDToPartitionID.inverse();

        this.partitionIDToPartition = new HashMap<>();
        partitions.forEach(p -> partitionIDToPartition.put(p.getID(), p));

        networkingComponent = new NetworkingComponent(
                new MessageProcessorImpl(),
                Collections.emptyList(),
                partitions.stream().map(p -> new PartitionServerConnector(p.getServerData(), p))
                        .collect(Collectors.toList())
        );

        initialAxiomsDistributor = new InitialAxiomsDistributor(initialAxioms, workloadDistributor);
    }

    @Override
    public MessageModel read() throws InterruptedException {
        return receivedMessages.take();
    }

    @Override
    public boolean hasMoreMessages() {
        return this.receivedMessages.isEmpty();
    }

    public void broadcast(SaturationStatusMessage statusMessage) {
        for (Long socketID : this.socketIDToPartitionID.keySet()) {
            send(socketID, statusMessage);
        }
    }

    public void send(long receiverSocketID, SaturationStatusMessage status) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, status);
        send(receiverSocketID, stateInfoMessage);
    }

    public void send(long receiverSocketID, MessageModel message) {
        MessageEnvelope messageEnvelope = new MessageEnvelope(receiverSocketID, message);
        networkingComponent.sendMessage(messageEnvelope);
    }

    private class MessageProcessorImpl implements MessageProcessor {
        @Override
        public void process(MessageEnvelope message) {
            receivedMessages.add((MessageModel) message.getMessage());
        }
    }

    private class PartitionServerConnector extends ServerConnector {

        private final DistributedPartitionModel partitionModel;

        public PartitionServerConnector(ServerData serverData, DistributedPartitionModel partitionModel) {
            super(serverData);
            this.partitionModel = partitionModel;
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            // get partition ID to socket ID mapping
            socketIDToPartitionID.put(socketManager.getSocketID(), partitionModel.getID());

            // send message
            InitializePartitionMessage initializePartitionMessage = new InitializePartitionMessage(
                    ControlNodeCommunicationChannel.this.controlNodeID,
                    partitionModel.getID(),
                    ControlNodeCommunicationChannel.this.partitions,
                    workloadDistributor,
                    initialAxiomsDistributor.getInitialAxioms(partitionModel.getID())
            );
            send(socketManager.getSocketID(), initializePartitionMessage);
        }
    }

}
