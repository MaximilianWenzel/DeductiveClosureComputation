package reasoning.saturation.distributed;

import data.Closure;
import networking.messages.MessageEnvelope;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.controlnode.CNSFinished;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SaturationControlNode<C extends Closure<A>, A extends Serializable, T extends Serializable> implements
        Runnable {

    private final List<DistributedWorkerModel<C, A, T>> workers;
    private C resultingClosure;
    private ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    private BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent = new LinkedBlockingQueue<>();
    private ControlNodeState<C, A, T> state;

    private SaturationConfiguration config;
    private ControlNodeStatistics stats = new ControlNodeStatistics();
    private List<WorkerStatistics> workerStatistics = new ArrayList<>();

    private ExecutorService threadPool;
    private int numberOfThreads;

    private AtomicBoolean mainSaturationTaskSubmittedToThreadPool = new AtomicBoolean(false);

    protected SaturationControlNode(List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    Iterator<? extends A> initialAxioms,
                                    C resultingClosure,
                                    SaturationConfiguration config,
                                    int numberOfThreads) {
        this.workers = workers;
        this.resultingClosure = resultingClosure;
        this.config = config;
        this.numberOfThreads = numberOfThreads;
        this.threadPool = Executors.newFixedThreadPool(numberOfThreads);
        this.communicationChannel = new ControlNodeCommunicationChannel<>(
                workers, workloadDistributor, initialAxioms,
                config, threadPool, messagesThatCouldNotBeSent,
                () -> {
                    if (mainSaturationTaskSubmittedToThreadPool.compareAndSet(false, true)) {
                        this.threadPool.submit(this);
                    }
                });
        init();
    }

    private void init() {
        this.state = new CNSInitializing<>(this);
    }

    public C saturate() {
        threadPool.submit(this);
        try {
            threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return resultingClosure;
    }

    @Override
    public void run() {
        mainSaturationTaskSubmittedToThreadPool.set(true);
        while (!(state instanceof CNSFinished)) {
            if (!messagesThatCouldNotBeSent.isEmpty()) {
                trySendingMessagesWhichCouldNotBeSent();
                if (!messagesThatCouldNotBeSent.isEmpty()) {
                    // messages still could not be sent completely - stop this task, rerun later
                    threadPool.submit(this);
                    return;
                }
            }

            if (mainSaturationTaskSubmittedToThreadPool.compareAndSet(!communicationChannel.hasMoreMessages(), false)) {
                return;
            }

            state.mainControlNodeLoop();
        }
        this.mainSaturationTaskSubmittedToThreadPool.set(false);

        if (config.collectControlNodeStatistics()) {
            stats.collectStopwatchTimes();
        }
        communicationChannel.terminateAfterAllMessagesHaveBeenSent();
        this.threadPool.shutdownNow();
    }

    private void trySendingMessagesWhichCouldNotBeSent() {
        // prevent endless loop, since callback method adds messages again if they could not be sent
        int currentQueueSize = messagesThatCouldNotBeSent.size();
        for (int i = 0; i < currentQueueSize; i++) {
            MessageEnvelope message = messagesThatCouldNotBeSent.remove();
            communicationChannel.send(message.getSocketID(), message.getMessage());
        }
    }

    public Collection<DistributedWorkerModel<C, A, T>> getWorkers() {
        return workers;
    }

    public void switchState(ControlNodeState<C, A, T> state) {
        this.state = state;
    }

    public ControlNodeCommunicationChannel<C, A, T> getCommunicationChannel() {
        return communicationChannel;
    }

    public void addAxiomToClosureResult(A axiom) {
        this.resultingClosure.add(axiom);
    }

    public SaturationConfiguration getConfig() {
        return config;
    }

    public ControlNodeStatistics getControlNodeStatistics() {
        return stats;
    }

    public List<WorkerStatistics> getWorkerStatistics() {
        return workerStatistics;
    }


}
