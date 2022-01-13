package reasoning.saturation.distributed.communication;

public interface SaturationCommunicationChannel {

    Object removeNextMessage() throws InterruptedException;

    Object pollNextMessage();

    boolean hasMoreMessages();

    void terminateNow();

    void terminateAfterAllMessagesHaveBeenSent();
}
