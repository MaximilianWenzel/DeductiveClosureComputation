package reasoning.saturation.parallel;

import data.Closure;
import data.DefaultClosure;
import data.ParallelToDo;
import enums.SaturationStatusMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.saturation.models.PartitionModel;
import reasoning.saturation.workload.InitialAxiomsDistributor;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public abstract class ParallelSaturation {

    private final BlockingDeque<SaturationStatusMessage> statusMessages = new LinkedBlockingDeque<>();
    private List<SaturationPartition> partitions;
    private Collection<PartitionModel> partitionModels;
    private volatile boolean allPartitionsConverged = false;
    private List<Thread> threadPool;
    private List<Object> initialAxioms;
    private WorkloadDistributor workloadDistributor;
    private InitialAxiomsDistributor initialAxiomsDistributor;

    private int convergedPartitions = 0;

    protected ParallelSaturation(List<Object> initialAxioms,
                                 Collection<PartitionModel> partitionModels,
                                 WorkloadDistributor workloadDistributor) {
        this.initialAxioms = initialAxioms;
        this.partitionModels = partitionModels;
        this.workloadDistributor = workloadDistributor;
        initPartitionThreads();
    }

    public void initPartitionThreads() {
        initVariables();
        initAndStartThreads();
    }

    protected SaturationPartition generateSaturatorPartition(PartitionModel partition) {
        Closure partitionClosure = new DefaultClosure();
        partitionClosure.addAll(initialAxiomsDistributor.getInitialAxioms(partition.getID()));

        ParallelToDo partitionToDo = new ParallelToDo();

        return new SaturationPartition(
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
        partitionModels.forEach(p -> {
            this.partitions.add(generateSaturatorPartition(p));
        });
    }

    public Set<Object> saturate() {
        try {
            while (!allPartitionsConverged) {
                SaturationStatusMessage message = statusMessages.poll(10, TimeUnit.MILLISECONDS);

                if (message != null) {
                    switch (message) {
                        case PARTITION_INFO_TODO_IS_EMPTY:
                            convergedPartitions++;
                            break;
                        case PARTITION_INFO_SATURATION_RUNNING:
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
        for (SaturationPartition partition : partitions) {
            closure.addAll(partition.getClosure());
        }
        return closure;
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturationPartition partition : this.partitions) {
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
