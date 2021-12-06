package reasoning.saturation.distributed.communication;

public interface SaturationCommunicationChannel {

    Object read() throws InterruptedException;

    boolean hasMoreMessagesToReadWriteOrToBeAcknowledged();

    void terminate();
}
