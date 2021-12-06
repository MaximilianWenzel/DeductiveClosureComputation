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

public class ParallelSaturation<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private SaturationInitializationFactory<C, A, T> factory;

    private final BlockingDeque<SaturationStatusMessage> statusMessages = new LinkedBlockingDeque<>();
    private List<SaturationContext<C, A, T>> contexts;
    private Collection<WorkerModel<C, A, T>> workerModels;
    private volatile boolean allWorkersConverged = false;
    private List<Thread> threadPool;
    private List<A> initialAxioms;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private InitialAxiomsDistributor<A> initialAxiomsDistributor;

    private int convergedWorkers = 0;

    protected ParallelSaturation(SaturationInitializationFactory<C, A, T> factory) {
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

    protected SaturationContext<C, A, T> generateSaturationContext(WorkerModel<C, A, T> worker) {
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

        // init workers
        this.contexts = new ArrayList<>();
        workerModels.forEach(p -> {
            this.contexts.add(generateSaturationContext(p));
        });
    }

    public C saturate() {
        try {
            while (!allWorkersConverged) {
                SaturationStatusMessage message = statusMessages.poll(10, TimeUnit.MILLISECONDS);

                if (message != null) {
                    switch (message) {
                        case WORKER_INFO_SATURATION_CONVERGED:
                            convergedWorkers++;
                            break;
                        case WORKER_INFO_SATURATION_RUNNING:
                            convergedWorkers--;
                            break;
                    }
                }

                if (!statusMessages.isEmpty() || convergedWorkers < contexts.size()) {
                    // not all messages processed or not all workers converged
                    continue;
                }

                // check if all workers do not process any axioms
                boolean nothingToDo = true;
                for (Thread t : threadPool) {
                    if (t.getState().equals(Thread.State.RUNNABLE)) {
                        nothingToDo = false;
                        break;
                    }
                }
                allWorkersConverged = nothingToDo;
            }

            // all workers converged
            for (Thread t : threadPool) {
                t.interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        C closure = factory.getNewClosure();
        for (SaturationContext<C, A, T> context : contexts) {
            context.getClosure().getClosureResults().forEach(closure::add);
        }
        return closure;
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturationContext<C, A, T> worker : this.contexts) {
            this.threadPool.add(new Thread(worker));
        }
        this.threadPool.forEach(Thread::start);
    }

    public boolean allWorkersConverged() {
        return allWorkersConverged;
    }

    public BlockingDeque<SaturationStatusMessage> getStatusMessages() {
        return statusMessages;
    }
}
