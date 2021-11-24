package reasoning.saturator.distributed;

public class PartitionMessage {
    private SaturationPartition partition;
    private Object message;

    public PartitionMessage(SaturationPartition partition, Object message) {
        this.partition = partition;
        this.message = message;
    }

    public SaturationPartition getPartition() {
        return partition;
    }

    public Object getMessage() {
        return message;
    }
}
