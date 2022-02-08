package reasoning.saturation.distributed.communication;

import networking.NIO2NetworkingComponent;
import networking.messages.MessageEnvelope;
import org.apache.commons.collections4.iterators.IteratorChain;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class NIO2NetworkingLoop implements Runnable {
    private ExecutorService threadPool;
    private AtomicBoolean mainLoopSubmittedToThreadPool = new AtomicBoolean(false);
    private NIO2NetworkingComponent networkingComponent;
    private boolean terminateAfterFinishing;

    private IteratorChain<Object> toDoMessages = new IteratorChain<>();
    private IteratorChain<Object> toDoMessagesIterator = new IteratorChain<>();

    private BlockingQueue<MessageEnvelope> messagesToSend = new LinkedBlockingQueue<>();

    private Object currentElement;

    public NIO2NetworkingLoop(ExecutorService threadPool, boolean terminateAfterFinishing) {
        this.threadPool = threadPool;
        this.terminateAfterFinishing = terminateAfterFinishing;
        init();
    }

    private void init() {
        this.networkingComponent = new NIO2NetworkingComponent(
                threadPool,
                this::onSocketOutboundBufferHasSpace
        );
    }

    @Override
    public void run() {
        do {
            while (runningCondition() || toDoMessagesIterator.hasNext() || toDoMessages.hasNext()) {
                trySendingMessagesWhichCouldNotBeSent();
                if (!messagesToSend.isEmpty()) {
                    // messages could not be sent - stop task
                    mainLoopSubmittedToThreadPool.set(false);
                    return;
                }

                if (!toDoMessagesIterator.hasNext()) {
                    // to-do cannot be modified after "hasNext()" has been called
                    toDoMessagesIterator = toDoMessages;
                    toDoMessages = new IteratorChain<>();
                }

                if (mainLoopSubmittedToThreadPool.compareAndSet(!toDoMessagesIterator.hasNext(), false)) {
                    onNoMoreMessages();
                    return;
                } else {
                    currentElement = toDoMessagesIterator.next();
                    processNextMessage(currentElement);
                }
            }

            this.currentElement = null;
            this.toDoMessagesIterator = new IteratorChain<>();
            onRestart();
        } while (!terminateAfterFinishing);

        onTerminate();
        this.mainLoopSubmittedToThreadPool.set(false);
    }

    public void start() {
        submitThisTaskToThreadPoolIfNotDoneAlready();
    }

    public void stop() {
        this.threadPool.shutdownNow();
    }

    public abstract void onRestart();

    void onNewMessageReceived(Object message) {
        processNextMessage(message);
        submitThisTaskToThreadPoolIfNotDoneAlready();
    }

    void onSocketOutboundBufferHasSpace(Long socketID) {
        if (!messagesToSend.isEmpty()) {
            // messages could not be sent
            submitThisTaskToThreadPoolIfNotDoneAlready();
        }
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public NIO2NetworkingComponent getNetworkingComponent() {
        return networkingComponent;
    }

    public void setTerminateAfterFinishing(boolean terminateAfterFinishing) {
        this.terminateAfterFinishing = terminateAfterFinishing;
    }

    public abstract void onNoMoreMessages();

    public abstract void onTerminate();

    public abstract boolean runningCondition();

    public abstract void processNextMessage(Object nextMessage);

    public void addToToDoQueue(Iterator<?> messages) {
        this.toDoMessages.addIterator(messages);
        submitThisTaskToThreadPoolIfNotDoneAlready();
    }

    public void addToToDoQueue(Object message) {
        this.toDoMessages.addIterator(Collections.singleton(message).iterator());
        submitThisTaskToThreadPoolIfNotDoneAlready();
    }

    public boolean sendMessage(long socketID, Serializable message) {
        if (!messagesToSend.isEmpty()) {
            this.messagesToSend.add(new MessageEnvelope(socketID, message));
            return false;
        } else {
            if (!networkingComponent.sendMessage(socketID, message)) {
                this.messagesToSend.add(new MessageEnvelope(socketID, message));
                return false;
            }
        }
        return true;
    }

    private void submitThisTaskToThreadPoolIfNotDoneAlready() {
        if (mainLoopSubmittedToThreadPool.compareAndSet(false, true) && !threadPool.isShutdown()) {
            this.threadPool.submit(this);
        }
    }

    private void trySendingMessagesWhichCouldNotBeSent() {
        while (!messagesToSend.isEmpty()) {
            MessageEnvelope message = messagesToSend.peek();
            if (!networkingComponent.sendMessage(message.getSocketID(), message.getMessage())) {
                // message could not be sent
                return;
            } else {
                // success
                messagesToSend.remove();
            }
        }
    }
}
