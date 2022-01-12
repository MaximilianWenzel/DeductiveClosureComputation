package reasoning.saturation.distributed.communication;

public interface SaturationCommunicationChannel {

    Object takeNextMessage() throws InterruptedException;

    Object pollNextMessage();

    boolean hasMoreMessages();

    void terminateNow();

    void terminateAfterAllMessagesHaveBeenSent();
}
