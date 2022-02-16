package reasoning.saturation.parallel;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.messages.AxiomCount;
import networking.messages.RequestAxiomMessageCount;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;
import util.QueueFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ParallelSaturation<C extends Closure<A>, A extends Serializable> {

    private final BlockingQueue<AxiomCount> statusMessages = QueueFactory.createSaturationToDo();
    private SaturationInitializationFactory<C, A> factory;
    private Logger log = ConsoleUtils.getLogger();
    private List<SaturationContext<C, A>> contexts;
    private Collection<WorkerModel<C, A>> workerModels;
    private volatile boolean allWorkersConverged = false;
    private ExecutorService threadPool;
    private Iterator<? extends A> initialAxioms;
    private WorkloadDistributor<C, A> workloadDistributor;
    private Map<Long, SaturationContext<C, A>> workerIDToSaturationContext = new HashMap<>();

    private AtomicLong sumOfAllReceivedAxioms = new AtomicLong(0);
    private AtomicLong sumOfAllSentAxioms = new AtomicLong(0);
    private AtomicInteger saturationStage = new AtomicInteger(0);
    private AtomicInteger convergedWorkers = new AtomicInteger(0);
    private boolean saturationConvergedVerificationStage = false;

    private SaturationConfiguration config;
    private ControlNodeStatistics controlNodeStatistics = null;

    public ParallelSaturation(SaturationInitializationFactory<C, A> factory, ExecutorService threadPool) {
        this.factory = factory;
        this.initialAxioms = factory.getInitialAxioms();
        this.workerModels = factory.getWorkerModels();
        this.workloadDistributor = factory.getWorkloadDistributor();
        this.config = new SaturationConfiguration();
        this.threadPool = threadPool;
        init();
    }

    public ParallelSaturation(SaturationConfiguration config, SaturationInitializationFactory<C, A> factory, ExecutorService threadPool) {
        this.factory = factory;
        this.initialAxioms = factory.getInitialAxioms();
        this.workerModels = factory.getWorkerModels();
        this.workloadDistributor = factory.getWorkloadDistributor();
        this.config = config;
        if (config.collectControlNodeStatistics()) {
            this.controlNodeStatistics = new ControlNodeStatistics();
        }
        this.threadPool = threadPool;
        init();
    }

    protected SaturationContext<C, A> generateSaturationContext(WorkerModel<C, A> worker) {
        C workerClosure = factory.getNewClosure();
        BlockingQueue<Object> workerToDo = QueueFactory.createSaturationToDo();
        SaturationContext<C, A> saturationContext = new SaturationContext<>(
                config,
                this,
                worker.getRules(),
                workloadDistributor,
                workerClosure,
                workerToDo
        );

        this.workerIDToSaturationContext.put(worker.getID(), saturationContext);
        return saturationContext;
    }

    private void init() {

        // init workers
        if (config.collectControlNodeStatistics()) {
            controlNodeStatistics.startStopwatch(StatisticsComponent.CONTROL_NODE_INITIALIZING_ALL_WORKERS);
        }

        this.contexts = new ArrayList<>();
        workerModels.forEach(p -> {
            this.contexts.add(generateSaturationContext(p));
        });

        for (SaturationContext<C, A> context : this.contexts) {
            ParallelSaturationInferenceProcessor<C, A> inferenceProcessor = new ParallelSaturationInferenceProcessor<>(
                    context.getStatistics(),
                    workloadDistributor,
                    workerIDToSaturationContext,
                    context.getClosure(),
                    context.getSentAxioms()
            );
            context.setInferenceProcessor(inferenceProcessor);
        }
        // distribute initial axioms
        initialAxioms.forEachRemaining(axiom -> {
            Stream<Long> workerIDs = workloadDistributor.getRelevantWorkerIDsForAxiom(axiom);
            workerIDs.forEach(workerID -> {
                sumOfAllSentAxioms.incrementAndGet();
                this.workerIDToSaturationContext.get(workerID).getToDo().add(axiom);
            });
        });

        submitWorkerTasksToThreadPool();

        if (config.collectControlNodeStatistics()) {
            controlNodeStatistics.stopStopwatch(StatisticsComponent.CONTROL_NODE_INITIALIZING_ALL_WORKERS);
        }
    }

    public C saturate() {
        if (config.collectControlNodeStatistics()) {
            controlNodeStatistics.startStopwatch(StatisticsComponent.CONTROL_NODE_SATURATION_TIME);
        }
        try {
            while (!allWorkersConverged) {

                AxiomCount message = statusMessages.take();
                if (config.collectControlNodeStatistics()) {
                    controlNodeStatistics.getNumberOfReceivedAxiomCountMessages().incrementAndGet();
                }
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
                        if (config.collectControlNodeStatistics()) {
                            controlNodeStatistics.getSumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent().incrementAndGet();
                        }
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

            if (config.collectControlNodeStatistics()) {
                controlNodeStatistics.stopStopwatch(StatisticsComponent.CONTROL_NODE_SATURATION_TIME);
            }

            // all workers converged
            for (SaturationContext<C, A> worker : contexts) {
                worker.getToDo().add(SaturationStatusMessage.CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (config.collectControlNodeStatistics()) {
            controlNodeStatistics.startStopwatch(StatisticsComponent.CONTROL_NODE_WAITING_FOR_CLOSURE_RESULTS);
        }

        C closure = factory.getNewClosure();
        for (SaturationContext<C, A> context : contexts) {
            context.getClosure().getClosureResults().forEach(closure::add);
        }

        if (config.collectControlNodeStatistics()) {
            controlNodeStatistics.stopStopwatch(StatisticsComponent.CONTROL_NODE_WAITING_FOR_CLOSURE_RESULTS);
            controlNodeStatistics.collectStopwatchTimes();
        }
        return closure;
    }

    private void requestAxiomCountsFromAllWorkers() {
        this.saturationStage.incrementAndGet();
        for (SaturationContext<C, A> context : contexts) {
            context.getToDo().add(new RequestAxiomMessageCount(0, this.saturationStage.get()));
        }
    }

    private void submitWorkerTasksToThreadPool() {
        for (SaturationContext<C, A> worker : this.contexts) {
            this.threadPool.submit(worker);
        }
    }

    public boolean allWorkersConverged() {
        return allWorkersConverged;
    }

    public BlockingQueue<AxiomCount> getStatusMessages() {
        return statusMessages;
    }

    public List<WorkerStatistics> getWorkerStatistics() {
        List<WorkerStatistics> statisticsList = new ArrayList<>();
        contexts.stream().map(SaturationContext::getStatistics).filter(Objects::nonNull).forEach(statisticsList::add);
        return statisticsList;
    }

    public ControlNodeStatistics getControlNodeStatistics() {
        return controlNodeStatistics;
    }
}
