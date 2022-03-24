package networking.messages;

import reasoning.saturation.distributed.metadata.WorkerStatistics;

/**
 * This message type is used by the worker to send all collected statistics back to the control node at the end of the saturation
 * procedure.
 */
public class StatisticsMessage extends MessageModel {

    private WorkerStatistics statistics;

    protected StatisticsMessage() {

    }

    public StatisticsMessage(long senderID, WorkerStatistics statistics) {
        super(senderID);
        this.statistics = statistics;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }

    public WorkerStatistics getStatistics() {
        return statistics;
    }
}
