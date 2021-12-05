package reasoning.saturation.workload;

import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.io.Serializable;
import java.util.*;

public class InitialAxiomsDistributor<A extends Serializable> {

    private Map<Long, RoaringBitmap> workerIDToInitialAxiomsIndex;
    private List<A> initialAxioms;
    private WorkloadDistributor workloadDistributor;

    public InitialAxiomsDistributor(List<A> initialAxioms, WorkloadDistributor workloadDistributor) {
        this.initialAxioms = initialAxioms;
        this.workloadDistributor = workloadDistributor;
        init();
    }

    private void init() {
        // distribute initial axioms across partitions
        this.workerIDToInitialAxiomsIndex = new HashMap<>();
        for (int i = 0; i < initialAxioms.size(); i++) {
            List<Long> relevantPartitions = workloadDistributor.getRelevantPartitionIDsForAxiom(initialAxioms.get(i));
            for (Long partitionID : relevantPartitions) {
                RoaringBitmap relevantAxiomsForPartition = workerIDToInitialAxiomsIndex.computeIfAbsent(partitionID, pID -> new RoaringBitmap());
                relevantAxiomsForPartition.add(i);
            }
        }
    }

    public List<A> getInitialAxioms(Long partitionID) {
        RoaringBitmap initialAxiomsPos = workerIDToInitialAxiomsIndex.get(partitionID);
        List<A> workerAxioms = null;
        if (initialAxiomsPos == null) {
            workerAxioms = Collections.emptyList();
        } else {
            workerAxioms = new ArrayList<>(initialAxiomsPos.getCardinality());
            PeekableIntIterator intIt = initialAxiomsPos.getIntIterator();
            while (intIt.hasNext()) {
                int pos = intIt.next();
                workerAxioms.add(initialAxioms.get(pos));
            }
        }
        return workerAxioms;
    }
}
