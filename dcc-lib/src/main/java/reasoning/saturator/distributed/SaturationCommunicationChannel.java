package reasoning.saturator.distributed;

import data.Closure;
import enums.SaturationStatusMessage;
import networking.messages.MessageModel;

public interface SaturationCommunicationChannel {

    MessageModel read() throws InterruptedException;

    MessageModel poll(long timeoutMilliseconds) throws InterruptedException;

    void sendToControlNode(SaturationStatusMessage statusMessage);

    void sendToControlNode(Closure closure);

    void broadcast(SaturationStatusMessage statusMessage);

    boolean hasMoreMessages();
}
