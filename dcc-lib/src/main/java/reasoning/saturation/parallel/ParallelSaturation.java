package reasoning.saturation.parallel;

import data.Closure;
import data.DefaultClosure;
import data.ParallelToDo;
import enums.SaturationStatusMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.InitialAxiomsDistributor;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class ParallelSaturation<C extends Closure<A>, A extends Serializable> {

    private SaturationInitializationFactory<C, A> factory;

    private final BlockingDeque<SaturationStatusMessage> statusMessages = new LinkedBlockingDeque<>();
    private List<SaturationContext<C, A>> contexts;
    private Collection<WorkerModel<C, A>> workerModels;
    private volatile boolean allPartitionsConverged = false;
    private List<Thread> threadPool;
    private List<A> initialAxioms;
    private WorkloadDistributor workloadDistributor;
    private InitialAxiomsDistributor<A> initialAxiomsDistributor;

    private int convergedPartitions = 0;

    protected ParallelSaturation(SaturationInitializationFactory<C, A> factory) {
        this.factory = factory;
        this.initialAxioms = factory.getInitialAxioms();
        this.workerModels = factory.getWorkerModels();
        this.workloadDistributor = factory.getWorkloadDistributor();
        initContexts();
    }

    public void initContexts() {
        initVariables();
        initAndStartThreads();
    }

    protected SaturationContext<C, A> generateSaturationContext(WorkerModel<C, A> worker) {
        C workerClosure = factory.getNewClosure();
        workerClosure.addAll(initialAxiomsDistributor.getInitialAxioms(worker.getID()));

        ParallelToDo<A> workerToDo = new ParallelToDo<>();

        return new SaturationContext<>(
                this,
                worker.getRules(),
                workerClosure,
                workerToDo,
                new ParallelSaturationInferenceProcessor(workloadDistributor));
    }

    private void initVariables() {
        this.initialAxiomsDistributor = new InitialAxiomsDistributor(initialAxioms, workloadDistributor);

        // init partitions
        this.contexts = new ArrayList<>();
        workerModels.forEach(p -> {
            this.contexts.add(generateSaturationContext(p));
        });
    }

    public C saturate() {
        try {
            while (!allPartitionsConverged) {
                SaturationStatusMessage message = statusMessages.poll(10, TimeUnit.MILLISECONDS);

                if (message != null) {
                    switch (message) {
                        case WORKER_INFO_SATURATION_CONVERGED:
                            convergedPartitions++;
                            break;
                        case WORKER_INFO_SATURATION_RUNNING:
                            convergedPartitions--;
                            break;
                    }
                }

                if (!statusMessages.isEmpty() || convergedPartitions < contexts.size()) {
                    // not all messages processed or not all partitions converged
                    continue;
                }

                // check if all partitions do not process any axioms
                boolean nothingToDo = true;
                for (Thread t : threadPool) {
                    if (t.getState().equals(Thread.State.RUNNABLE)) {
                        nothingToDo = false;
                        break;
                    }
                }
                allPartitionsConverged = nothingToDo;
            }

            // all partitions converged
            for (Thread t : threadPool) {
                t.interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        C closure = factory.getNewClosure();
        for (SaturationContext<C, A> context : contexts) {
            context.getClosure().getClosureResults().forEach(closure::add);
        }
        return closure;
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturationContext<C, A> worker : this.contexts) {
            this.threadPool.add(new Thread(worker));
        }
        this.threadPool.forEach(Thread::start);
    }

    public boolean allPartitionsConverged() {
        return allPartitionsConverged;
    }

    public BlockingDeque<SaturationStatusMessage> getStatusMessages() {
        return statusMessages;
    }
}
