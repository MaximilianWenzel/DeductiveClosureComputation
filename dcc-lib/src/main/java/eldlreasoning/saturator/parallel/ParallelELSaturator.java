package eldlreasoning.saturator.parallel;

import eldlsyntax.ELConceptInclusion;
import eldlsyntax.IndexedELOntology;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ParallelELSaturator {

    private IndexedELOntology elOntology;
    private WorkloadManager workloadManager;
    private int numberOfPartitions;

    private List<SaturatorPartition> partitions;

    private Set<ELConceptInclusion> closure;
    private List<Thread> threadPool;

    public ParallelELSaturator(IndexedELOntology elOntology, int numberOfPartitions) {
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
        init();
    }

    private void init() {
        workloadManager = new WorkloadManager(elOntology, numberOfPartitions);
        this.partitions = workloadManager.getPartitions();
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturatorPartition partition : this.partitions) {
            this.threadPool.add(new Thread(partition));
        }
        this.threadPool.add(new Thread(workloadManager));

        this.threadPool.forEach(Thread::start);
    }


    public Set<ELConceptInclusion> saturate() {
        initAndStartThreads();
        while (!workloadManager.isSaturationFinished()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean nothingToDo = true;
            for (Thread t : threadPool) {
                if (t.getState().equals(Thread.State.RUNNABLE)) {
                    nothingToDo = false;
                    break;
                }
            }
            workloadManager.setSaturationFinished(nothingToDo);
        }

        try {
            Thread.sleep(10);
            for (Thread t : threadPool) {
                t.interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.closure = new UnifiedSet<>();
        for (SaturatorPartition partition : partitions) {
            closure.addAll(partition.getClosure());
        }
        return closure;
    }


}
