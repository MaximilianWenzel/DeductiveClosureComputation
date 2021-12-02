package networking.io;

import networking.messages.MessageEnvelope;

public interface MessageProcessor {

    void process(MessageEnvelope message);
}
