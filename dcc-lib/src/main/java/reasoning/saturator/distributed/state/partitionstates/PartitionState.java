package reasoning.saturator.distributed.state.partitionstates;

import networking.messages.DebugMessage;
import networking.messages.MessageModelVisitor;
import reasoning.saturator.distributed.SaturationPartition;
import util.ConsoleUtils;

import java.util.logging.Logger;

public abstract class PartitionState implements MessageModelVisitor {

    protected final Logger log = ConsoleUtils.getLogger();
    protected SaturationPartition partition;

    public PartitionState(SaturationPartition partition) {
        this.partition = partition;
    }

    public abstract void onToDoQueueIsEmpty();

    @Override
    public void visit(DebugMessage message) {
        log.info(message.getMessage());
    }
}
