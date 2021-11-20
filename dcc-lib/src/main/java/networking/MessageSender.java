package networking;

import java.util.concurrent.BlockingQueue;

public interface MessageSender {

    BlockingQueue<MessageEnvelope> getMessagesToSend();
}
