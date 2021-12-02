package reasoning.saturation.parallel;

import data.Closure;
import data.DefaultClosure;
import data.ParallelToDo;
import enums.SaturationStatusMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.rules.ParallelSaturationInferenceProcessor;
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

public class ParallelSaturation {

    private final BlockingDeque<SaturationStatusMessage> statusMessages = new LinkedBlockingDeque<>();
    private List<SaturationContext> partitions;
    private Collection<WorkerModel> workerModels;
    private volatile boolean allPartitionsConverged = false;
    private List<Thread> threadPool;
    private List<? extends Serializable> initialAxioms;
    private WorkloadDistributor workloadDistributor;
    private InitialAxiomsDistributor initialAxiomsDistributor;

    private int convergedPartitions = 0;

    protected ParallelSaturation(List<? extends Serializable> initialAxioms,
                                 Collection<WorkerModel> workerModels,
                                 WorkloadDistributor workloadDistributor) {
        this.initialAxioms = initialAxioms;
        this.workerModels = workerModels;
        this.workloadDistributor = workloadDistributor;
        initPartitionThreads();
    }

    public void initPartitionThreads() {
        initVariables();
        initAndStartThreads();
    }

    protected SaturationContext generateSaturatorPartition(WorkerModel partition) {
        Closure partitionClosure = new DefaultClosure();
        partitionClosure.addAll(initialAxiomsDistributor.getInitialAxioms(partition.getID()));

        ParallelToDo partitionToDo = new ParallelToDo();

        return new SaturationContext(
                this,
                partition.getRules(),
                partitionClosure,
                partitionToDo,
                new ParallelSaturationInferenceProcessor(workloadDistributor));
    }

    private void initVariables() {
        this.initialAxiomsDistributor = new InitialAxiomsDistributor(initialAxioms, workloadDistributor);

        // init partitions
        this.partitions = new ArrayList<>();
        workerModels.forEach(p -> {
            this.partitions.add(generateSaturatorPartition(p));
        });
    }

    public Set<Object> saturate() {
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

                if (!statusMessages.isEmpty() || convergedPartitions < partitions.size()) {
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

        Set<Object> closure = new UnifiedSet<>();
        for (SaturationContext partition : partitions) {
            closure.addAll(partition.getClosure());
        }
        return closure;
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturationContext partition : this.partitions) {
            this.threadPool.add(new Thread(partition));
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
