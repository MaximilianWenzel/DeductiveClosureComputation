package reasoning.saturation.distributed.communication;

import enums.SaturationStatusMessage;
import networking.NetworkingComponent;
import networking.messages.MessageModel;
import reasoning.saturation.models.PartitionModel;

import java.util.List;

public class ControlNodeCommunicationChannel implements SaturationCommunicationChannel {

    protected NetworkingComponent networkingComponent;
    protected List<PartitionModel> partitions;

    public ControlNodeCommunicationChannel(List<PartitionModel> partitions) {


    }

    @Override
    public MessageModel read() throws InterruptedException {
        return null;
    }

    @Override
    public boolean hasMoreMessages() {
        return false;
    }

    public void broadcast(SaturationStatusMessage statusMessage) {

    }

}
