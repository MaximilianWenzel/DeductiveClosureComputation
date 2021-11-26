package reasoning.saturation.distributed.communication;

import networking.messages.MessageModel;

public interface SaturationCommunicationChannel {

    MessageModel read() throws InterruptedException;

    boolean hasMoreMessages();
}
