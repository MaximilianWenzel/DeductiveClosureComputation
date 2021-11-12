package reasoning.saturator.parallel;

import data.Closure;
import data.Dataset;
import data.DefaultClosure;
import data.ParallelToDo;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class ParallelSaturator<P, T> {

    private final Dataset<P, T> dataset;
    private final ParallelToDo<P> toDo = new ParallelToDo<>();
    private final Set<P> consideredAxioms = new UnifiedSet<>();
    private List<SaturatorPartition<P, T>> partitions;
    private volatile boolean saturationFinished = false;
    private List<Thread> threadPool;


    protected ParallelSaturator(Dataset<P, T> dataset) {
        this.dataset = dataset;
    }

    protected SaturatorPartition<P, T> generateSaturatorPartition(PartitionModel<P, T> partition) {
        Closure<P> partitionClosure = new DefaultClosure<>();
        ParallelToDo<P> partitionToDo = new ParallelToDo<>();

        return new SaturatorPartition<>(partition.getRules(), partition.getTermPartition(),
                partition.getDatasetFragment(),
                partitionClosure,
                partitionToDo,
                this);
    }

    public void init() {
        initVariables();
        initAndStartThreads();
    }

    public Set<P> saturate() {
        try {
            while (!saturationFinished) {
                P axiom = null;
                axiom = toDo.poll(10, TimeUnit.MILLISECONDS);

                if (axiom == null) {
                    boolean nothingToDo = true;
                    for (Thread t : threadPool) {
                        if (t.getState().equals(Thread.State.RUNNABLE)) {
                            nothingToDo = false;
                            break;
                        }
                    }
                    saturationFinished = nothingToDo;
                } else {
                    distributeAxiom(axiom);
                }
            }
            for (Thread t : threadPool) {
                t.interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Set<P> closure = new UnifiedSet<>();
        for (SaturatorPartition<P, T> partition : partitions) {
            closure.addAll(partition.getClosure());
        }
        return closure;
    }

    private void initVariables() {
        this.dataset.getInitialAxioms().forEachRemaining(toDo::add);

        // init partitions
        List<PartitionModel<P, T>> partitionModels = initializePartitions();
        this.partitions = new ArrayList<>();
        partitionModels.forEach(p -> {
            this.partitions.add(generateSaturatorPartition(p));
        });
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturatorPartition<P, T> partition : this.partitions) {
            this.threadPool.add(new Thread(partition));
        }
        this.threadPool.forEach(Thread::start);
    }

    private void distributeAxiom(P axiom) {
        if (consideredAxioms.add(axiom)) {
            for (SaturatorPartition<P, T> partition : partitions) {
                if (isRelevantAxiomToPartition(partition, axiom)) {
                    partition.getToDo().add(axiom);
                }
            }
        }
    }

    protected abstract List<PartitionModel<P, T>> initializePartitions();

    public abstract boolean isRelevantAxiomToPartition(SaturatorPartition<P, T> partition, P axiom);

    public abstract boolean checkIfOtherPartitionsRequireAxiom(SaturatorPartition<P, T> partition, P axiom);

    public boolean isSaturationFinished() {
        return saturationFinished;
    }

    public ParallelToDo<P> getToDo() {
        return toDo;
    }

}
