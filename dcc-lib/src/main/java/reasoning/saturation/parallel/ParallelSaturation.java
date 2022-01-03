package reasoning.saturation.parallel;

import data.Closure;
import data.DefaultToDo;
import data.ParallelToDo;
import networking.messages.AxiomCount;
import networking.messages.RequestAxiomMessageCount;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.InitialAxiomsDistributor;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class ParallelSaturation<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final BlockingQueue<AxiomCount> statusMessages = new DefaultToDo<>();
    private SaturationInitializationFactory<C, A, T> factory;
    private Logger log = ConsoleUtils.getLogger();
    private List<SaturationContext<C, A, T>> contexts;
    private Collection<WorkerModel<C, A, T>> workerModels;
    private volatile boolean allWorkersConverged = false;
    private List<Thread> threadPool;
    private List<? extends A> initialAxioms;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private InitialAxiomsDistributor<A> initialAxiomsDistributor;
    private Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext = new HashMap<>();

    private AtomicLong sumOfAllReceivedAxioms = new AtomicLong(0);
    private AtomicLong sumOfAllSentAxioms = new AtomicLong(0);
    private AtomicInteger saturationStage = new AtomicInteger(0);
    private AtomicInteger convergedWorkers = new AtomicInteger(0);
    private boolean saturationConvergedVerificationStage = false;


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
        ParallelToDo workerToDo = new ParallelToDo();
        SaturationContext<C, A, T> saturationContext = new SaturationContext<>(
                this,
                worker.getRules(),
                workerClosure,
                workerToDo
        );

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

        for (SaturationContext<C, A, T> context : this.contexts) {
            ParallelSaturationInferenceProcessor<C, A, T> inferenceProcessor = new ParallelSaturationInferenceProcessor<>(
                    workloadDistributor, workerIDToSaturationContext, context.getClosure(), context.getSentAxioms());
            context.setInferenceProcessor(inferenceProcessor);
        }

        // distribute initial axioms
        for (A axiom : initialAxioms) {
            List<Long> workerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(axiom);
            for (Long workerID : workerIDs) {
                sumOfAllSentAxioms.incrementAndGet();
                this.workerIDToSaturationContext.get(workerID).getToDo().add(axiom);
            }
        }
    }

    public C saturate() {
        initAndStartThreads();

        try {
            while (!allWorkersConverged) {
                AxiomCount message = statusMessages.take();

                boolean messageFromLatestSaturationStage = message.getStage() == saturationStage.get();

                sumOfAllReceivedAxioms.addAndGet(message.getReceivedAxioms());
                sumOfAllSentAxioms.addAndGet(message.getSentAxioms());

                if (!messageFromLatestSaturationStage) {
                    continue;
                }

                if (saturationConvergedVerificationStage) {
                    if (message.getReceivedAxioms() == 0 || message.getSentAxioms() == 0) {
                        convergedWorkers.getAndIncrement();
                        log.info(
                                "Worker " + message.getSenderID() + " converged (" + convergedWorkers.get() + "/" + workerModels.size() + ")");
                    } else if (message.getReceivedAxioms() > 0 || message.getSentAxioms() > 0) {
                        log.info("Worker " + message.getSenderID() + " is running again.");
                        saturationConvergedVerificationStage = false;
                        convergedWorkers.set(0);
                    }
                }

                if (sumOfAllReceivedAxioms.get() == sumOfAllSentAxioms.get()) {
                    if (!saturationConvergedVerificationStage) {
                        log.info(
                                "Sum of received axioms equals sum of sent axioms. Entering verification stage: requesting all axiom message counts.");
                        saturationConvergedVerificationStage = true;
                        requestAxiomCountsFromAllWorkers();
                    } else if (convergedWorkers.get() == workerModels.size()) {
                        // all workers converged
                        break;
                    }
                } else if (saturationConvergedVerificationStage && sumOfAllReceivedAxioms.get() != sumOfAllSentAxioms.get()) {
                    log.info("Sum of received axioms does not equal sum of sent axioms anymore.");
                    log.info("Received: " + sumOfAllReceivedAxioms.get());
                    log.info("Sent: " + sumOfAllSentAxioms.get());
                    saturationConvergedVerificationStage = false;
                    convergedWorkers.set(0);
                }
            }
            allWorkersConverged = true;

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

    private void requestAxiomCountsFromAllWorkers() {
        this.saturationStage.incrementAndGet();
        for (SaturationContext<C, A, T> context : contexts) {
            context.getToDo().add(new RequestAxiomMessageCount(0, this.saturationStage.get()));
        }
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

    public BlockingQueue<AxiomCount> getStatusMessages() {
        return statusMessages;
    }
}
