package reasoning.saturation.distributed;

import data.Closure;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class SaturationControlNode<C extends Closure<A>, A extends Serializable, T extends Serializable> implements
        Subscriber<Object> {

    private static final Logger log = ConsoleUtils.getLogger();

    private final List<DistributedWorkerModel<C, A, T>> workers;
    private C resultingClosure;
    private ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    private ControlNodeState<C, A, T> state;

    private SaturationConfiguration config;
    private ControlNodeStatistics stats = new ControlNodeStatistics();
    private List<WorkerStatistics> workerStatistics = new ArrayList<>();

    private BlockingQueue<C> closureResultQueue = new ArrayBlockingQueue<>(1);

    private ExecutorService threadPool;
    private Subscription receivedMessagesSubscription;

    protected SaturationControlNode(List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    Iterator<? extends A> initialAxioms,
                                    C resultingClosure,
                                    SaturationConfiguration config) {
        this.workers = workers;
        this.resultingClosure = resultingClosure;
        this.config = config;

        this.threadPool = Executors.newFixedThreadPool(1);
        this.communicationChannel = new ControlNodeCommunicationChannel<>(
                threadPool,
                this,
                workers,
                workloadDistributor,
                initialAxioms,
                config
        );
        init();
    }

    private void init() {
        this.state = new CNSInitializing<>(this);
    }

    public C saturate() {
        init();
        try {
            this.closureResultQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this.resultingClosure;
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


    @Override
    public void onSubscribe(Subscription subscription) {
        this.receivedMessagesSubscription = subscription;
        this.receivedMessagesSubscription.request(1);
    }

    @Override
    public void onNext(Object o) {
        state.processMessage(o);
        this.receivedMessagesSubscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        log.warning(throwable.getMessage());
    }

    @Override
    public void onComplete() {
        if (config.collectControlNodeStatistics()) {
            stats.collectStopwatchTimes();
        }
        closureResultQueue.add(resultingClosure);
        communicationChannel.terminate();
        this.notify();
    }
}
