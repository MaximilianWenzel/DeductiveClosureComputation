package reasoning.saturation.distributed;

import data.Closure;
import data.DefaultClosure;
import networking.messages.MessageModel;
import reasoning.saturation.models.PartitionModel;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.communication.SaturationCommunicationChannel;
import reasoning.saturation.distributed.state.controlnodestates.CNSFinished;
import reasoning.saturation.distributed.state.controlnodestates.CNSInitializing;
import reasoning.saturation.distributed.state.controlnodestates.ControlNodeState;

import java.util.*;

public class SaturationControlNode {

    private final ControlNodeCommunicationChannel communicationChannel;
    private List<PartitionModel> partitions;
    private ControlNodeState state;

    private final Closure closureResult = new DefaultClosure();

    protected SaturationControlNode(List<PartitionModel> partitions) {
        this.communicationChannel = new ControlNodeCommunicationChannel(partitions);
        this.partitions = partitions;
        init();
    }

    private void init() {
        this.state = new CNSInitializing(this);
    }

    public Closure saturate() {
        try {
            while (!(state instanceof CNSFinished)) {
                MessageModel message = communicationChannel.read();
                message.accept(state);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return closureResult;
    }


    public List<PartitionModel> getPartitions() {
        return partitions;
    }

    public void switchState(ControlNodeState state) {
        this.state = state;
    }

    public ControlNodeCommunicationChannel getCommunicationChannel() {
        return communicationChannel;
    }

    public void addAxiomsToClosureResult(Collection<Object> axioms) {
        this.closureResult.add(axioms);
    }
}
