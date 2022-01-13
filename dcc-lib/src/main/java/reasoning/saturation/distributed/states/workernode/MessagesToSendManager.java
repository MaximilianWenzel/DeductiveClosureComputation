package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import networking.NIO2NetworkingComponent;
import networking.messages.MessageEnvelope;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class MessagesToSendManager<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    BlockingQueue<MessageEnvelope> messagesToSend = new LinkedBlockingQueue<>();
    MessageEnvelope currentMessage;
    NIO2NetworkingComponent nio2NetworkingComponent;

    /*
    public MessagesToSendManager() {
    }

    public void addMessagesToSend(Stream<MessageEnvelope> messages) {
        messages.forEach();
    }

    public void sendMessages() {
        messagesToSend.
    }

     */


}
