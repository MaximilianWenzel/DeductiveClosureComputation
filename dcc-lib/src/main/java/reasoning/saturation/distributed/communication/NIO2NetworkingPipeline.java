package reasoning.saturation.distributed.communication;

import networking.NIO2NetworkingComponent;
import networking.messages.MessageEnvelope;
import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.iterators.IteratorChain;
import util.QueueFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class NIO2NetworkingPipeline implements Runnable {
    private ExecutorService threadPool;
    private AtomicBoolean mainLoopSubmittedToThreadPool = new AtomicBoolean(false);
    private NIO2NetworkingComponent networkingComponent;
    private boolean terminateAfterFinishing;

    private Queue<Object> toDoMessages = QueueFactory.createDistributedSaturationToDo();
    private MessageEnvelope currentMessageToSend = null;

    private Object currentElement;

    public NIO2NetworkingPipeline(ExecutorService threadPool, boolean terminateAfterFinishing) {
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
            while (runningCondition() || !toDoMessages.isEmpty()) {
                if (currentMessageToSend != null) {
                    if (!trySendingCurrentMessage()) {
                        // messages could not be sent - stop task
                        mainLoopSubmittedToThreadPool.set(false);
                        return;
                    }
                }

                if (mainLoopSubmittedToThreadPool.compareAndSet(toDoMessages.isEmpty(), false)) {
                    onNoMoreMessages();
                    return;
                } else {
                    currentElement = toDoMessages.poll();
                    if (currentElement instanceof MessageEnvelope) {
                        currentMessageToSend = (MessageEnvelope) currentElement;
                        if (!trySendingCurrentMessage()) {
                            // messages could not be sent - stop task
                            mainLoopSubmittedToThreadPool.set(false);
                            return;
                        }

                    } else {
                        processNextMessage(currentElement);
                    }
                }
            }

            this.currentElement = null;
            this.currentMessageToSend = null;
            onRestart();
        } while (!terminateAfterFinishing);

        onTerminate();
        this.mainLoopSubmittedToThreadPool.set(false);
    }

    /**
     * Returns whether the current message could be sent.
     */
    private boolean trySendingCurrentMessage() {
        if (!networkingComponent.sendMessage(currentMessageToSend.getSocketID(), currentMessageToSend.getMessage())) {
            return false;
        } else {
            currentMessageToSend = null;
            return true;
        }
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
        if (!toDoMessages.isEmpty()) {
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

    public void addToToDoQueue(Object message) {
        this.toDoMessages.add(message);
        submitThisTaskToThreadPoolIfNotDoneAlready();
    }

    public void sendMessage(long socketID, Serializable message) {
        toDoMessages.add(new MessageEnvelope(socketID, message));
        submitThisTaskToThreadPoolIfNotDoneAlready();
    }

    private void submitThisTaskToThreadPoolIfNotDoneAlready() {
        if (mainLoopSubmittedToThreadPool.compareAndSet(false, true) && !threadPool.isShutdown()) {
            this.threadPool.submit(this);
        }
    }

}
