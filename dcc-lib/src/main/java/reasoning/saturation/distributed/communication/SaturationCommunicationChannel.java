package reasoning.saturation.distributed.communication;

public interface SaturationCommunicationChannel {

    Object read() throws InterruptedException;

    boolean hasMoreMessages();

    void terminateNow();

    void terminateAfterAllMessagesHaveBeenSent();
}
