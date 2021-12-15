package reasoning.saturation.parallel;

import data.Closure;
import data.ParallelToDo;
import enums.SaturationStatusMessage;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.InitialAxiomsDistributor;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

public class ParallelSaturation<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private SaturationInitializationFactory<C, A, T> factory;

    private final BlockingQueue<SaturationStatusMessage> statusMessages = new LinkedBlockingQueue<>();
    private List<SaturationContext<C, A, T>> contexts;
    private Collection<WorkerModel<C, A, T>> workerModels;
    private volatile boolean allWorkersConverged = false;
    private List<Thread> threadPool;
    private List<? extends A> initialAxioms;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private InitialAxiomsDistributor<A> initialAxiomsDistributor;
    private Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext = new HashMap<>();

    private int convergedWorkers = 0;

    public ParallelSaturation(SaturationInitializationFactory<C, A, T> factory) {
        this.factory = factory;
        this.initialAxioms = factory.getInitialAxioms();
        this.workerModels = factory.getWorkerModels();
        this.workloadDistributor = factory.getWorkloadDistributor();
        initContexts();
    }

    private void initContexts() {
        initVariables();
    }

    protected SaturationContext<C, A, T> generateSaturationContext(WorkerModel<C, A, T> worker) {
        C workerClosure = factory.getNewClosure();
        ParallelToDo<A> workerToDo = new ParallelToDo<>();
        SaturationContext<C, A, T> saturationContext = new SaturationContext<>(
                this,
                worker.getRules(),
                workerClosure,
                workerToDo);

        this.workerIDToSaturationContext.put(worker.getID(), saturationContext);
        return saturationContext;
    }

    private void initVariables() {
        this.initialAxiomsDistributor = new InitialAxiomsDistributor<>(initialAxioms, workloadDistributor);

        // init workers
        this.contexts = new ArrayList<>();
        workerModels.forEach(p -> {
            this.contexts.add(generateSaturationContext(p));
        });

        ParallelSaturationInferenceProcessor<C, A, T> inferenceProcessor = new ParallelSaturationInferenceProcessor<>(
                workloadDistributor, workerIDToSaturationContext);
        for (SaturationContext<C, A, T> context : this.contexts) {
            context.setInferenceProcessor(inferenceProcessor);
        }

        // distribute initial axioms
        for (A axiom : initialAxioms) {
            List<Long> workerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(axiom);
            for (Long workerID : workerIDs) {
                this.workerIDToSaturationContext.get(workerID).getToDo().add(axiom);
            }
        }
    }

    public C saturate() {
        initAndStartThreads();

        try {
            while (!allWorkersConverged) {
                SaturationStatusMessage message = statusMessages.take();

                switch (message) {
                    case WORKER_INFO_SATURATION_CONVERGED:
                        convergedWorkers++;
                        break;
                    case WORKER_INFO_SATURATION_RUNNING:
                        convergedWorkers--;
                        break;
                }

                if (convergedWorkers == contexts.size()) {
                    allWorkersConverged = true;
                }
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

    public BlockingQueue<SaturationStatusMessage> getStatusMessages() {
        return statusMessages;
    }
}
