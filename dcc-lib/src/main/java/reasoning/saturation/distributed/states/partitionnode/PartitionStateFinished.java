package reasoning.saturation.distributed.states.partitionnode;

import exceptions.MessageProtocolViolationException;
import networking.messages.InitializePartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationPartition;

public class PartitionStateFinished extends PartitionState {

    public PartitionStateFinished(SaturationPartition partition) {
        super(partition);
    }


    @Override
    public void visit(InitializePartitionMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        throw new MessageProtocolViolationException();
    }
}
