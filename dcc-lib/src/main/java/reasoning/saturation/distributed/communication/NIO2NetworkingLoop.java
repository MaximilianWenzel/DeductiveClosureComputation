package reasoning.saturation.distributed.communication;

import networking.NIO2NetworkingComponent;
import networking.messages.MessageEnvelope;
import util.QueueFactory;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class NIO2NetworkingLoop implements Runnable {
    private ExecutorService threadPool;
    private BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent = new LinkedBlockingQueue<>();
    private AtomicBoolean mainLoopSubmittedToThreadPool = new AtomicBoolean(true);
    private NIO2NetworkingComponent networkingComponent;
    private boolean terminateAfterFinishing;
    private BlockingQueue<Object> receivedMessages = QueueFactory.createSaturationToDo();

    public NIO2NetworkingLoop(ExecutorService threadPool, boolean terminateAfterFinishing) {
        this.threadPool = threadPool;
        this.terminateAfterFinishing = terminateAfterFinishing;
        init();
    }

    private void init() {
        this.networkingComponent = new NIO2NetworkingComponent(
                threadPool,
                this::onMessageCouldNotBeSent,
                this::onSocketOutboundBufferHasSpace
        );
    }

    @Override
    public void run() {
        do {
            while (runningCondition()) {
                if (!messagesThatCouldNotBeSent.isEmpty()) {
                    trySendingMessagesWhichCouldNotBeSent();
                    if (!messagesThatCouldNotBeSent.isEmpty()) {
                        // messages still could not be sent completely - stop this task
                        // rerun when write operation has finished
                        mainLoopSubmittedToThreadPool.set(false);
                        return;
                    }
                }

                if (mainLoopSubmittedToThreadPool.compareAndSet(receivedMessages.isEmpty(), false)) {
                    onNoMoreMessages();
                    return;
                } else {
                    processNextMessage(receivedMessages.poll());
                }
            }

            this.messagesThatCouldNotBeSent.clear();
            this.receivedMessages.clear();
            onRestart();
        } while (!terminateAfterFinishing);

        onTerminate();
        this.mainLoopSubmittedToThreadPool.set(false);
    }

    public void start() {
        this.threadPool.submit(this);
    }

    public void stop() {
        this.threadPool.shutdownNow();
    }

    public abstract void onRestart();

    void onMessageCouldNotBeSent(MessageEnvelope message) {
        this.messagesThatCouldNotBeSent.add(message);
    }

    void onNewMessageReceived(Collection<?> messages) {
        receivedMessages.addAll(messages);
        if (mainLoopSubmittedToThreadPool.compareAndSet(false, true)) {
            this.threadPool.submit(this);
        }
    }

    void onNewMessageReceived(Object message) {
        this.receivedMessages.add(message);

        if (mainLoopSubmittedToThreadPool.compareAndSet(false, true)) {
            this.threadPool.submit(this);
        }
    }

    void onSocketOutboundBufferHasSpace(Long socketID) {
        if (!messagesThatCouldNotBeSent.isEmpty()
                && mainLoopSubmittedToThreadPool.compareAndSet(false, true)) {
            this.threadPool.submit(this);
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

    private void trySendingMessagesWhichCouldNotBeSent() {
        // prevent endless loop, since callback method adds messages again if they could not be sent
        int currentQueueSize = messagesThatCouldNotBeSent.size();
        for (int i = 0; i < currentQueueSize; i++) {
            MessageEnvelope message = messagesThatCouldNotBeSent.remove();
            networkingComponent.sendMessage(message.getSocketID(), message.getMessage());
        }
    }

}
