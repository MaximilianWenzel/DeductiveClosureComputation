package networking.io;

import networking.messages.MessageEnvelope;

import java.util.concurrent.BlockingQueue;

public interface MessageSender {

    BlockingQueue<MessageEnvelope> getMessagesToSend();
}
