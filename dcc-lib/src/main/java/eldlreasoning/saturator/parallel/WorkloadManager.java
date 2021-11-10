package eldlreasoning.saturator.parallel;

import eldlsyntax.ELConceptInclusion;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import util.DistributedOWL2ELSaturationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class WorkloadManager {
    private List<SaturatorPartition> partitions;
    private Set<ELConceptInclusion> consideredAxioms = new UnifiedSet<>();
    private BlockingQueue<ELConceptInclusion> toDo;
    private volatile boolean saturationFinished = false;
    private List<Thread> threadPool;

    WorkloadManager() {
    }

    public Set<ELConceptInclusion> startSaturation() {
        initAndStartThreads();
        try {
            while (!saturationFinished) {
                ELConceptInclusion axiom = null;
                axiom = toDo.poll(1000, TimeUnit.MILLISECONDS);

                if (axiom == null) {
                    // queue is empty, check if partitions have finished their work
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

        Set<ELConceptInclusion> closure = new UnifiedSet<>();
        for (SaturatorPartition partition : partitions) {
            closure.addAll(partition.getClosure());
        }
        return closure;
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturatorPartition partition : this.partitions) {
            this.threadPool.add(new Thread(partition));
        }
        this.threadPool.forEach(Thread::start);
    }

    private void distributeAxiom(ELConceptInclusion axiom) {
        if (consideredAxioms.add(axiom)) {
            for (SaturatorPartition partition : partitions) {
                if (DistributedOWL2ELSaturationUtils.isRelevantAxiomToPartition(partition.getConceptPartition(), axiom)) {
                    partition.getToDo().add(axiom);
                }
            }
        }
    }

    public boolean isSaturationFinished() {
        return saturationFinished;
    }

    public void setSaturationFinished(boolean saturationFinished) {
        this.saturationFinished = saturationFinished;
    }

    public BlockingQueue<ELConceptInclusion> getToDo() {
        return toDo;
    }

    void setToDo(BlockingQueue<ELConceptInclusion> toDo) {
        this.toDo = toDo;
    }

    public List<SaturatorPartition> getPartitions() {
        return partitions;
    }

    void setPartitions(List<SaturatorPartition> partitions) {
        this.partitions = partitions;
    }
}
