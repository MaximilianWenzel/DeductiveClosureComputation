package reasoning.saturation.workload;

import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;

public class InitialAxiomsDistributor {

    private Map<Long, RoaringBitmap> partitionIDToInitialAxiomsIndex;
    private List<Object> initialAxioms;
    private WorkloadDistributor workloadDistributor;

    public InitialAxiomsDistributor(List<Object> initialAxioms, WorkloadDistributor workloadDistributor) {
        this.initialAxioms = initialAxioms;
        this.workloadDistributor = workloadDistributor;
        init();
    }

    private void init() {
        // distribute initial axioms across partitions
        this.partitionIDToInitialAxiomsIndex = new HashMap<>();
        for (int i = 0; i < initialAxioms.size(); i++) {
            List<Long> relevantPartitions = workloadDistributor.getRelevantPartitionIDsForAxiom(initialAxioms.get(i));
            for (Long partitionID : relevantPartitions) {
                RoaringBitmap relevantAxiomsForPartition = partitionIDToInitialAxiomsIndex.computeIfAbsent(partitionID, pID -> new RoaringBitmap());
                relevantAxiomsForPartition.add(i);
            }
        }
    }

    public List<Object> getInitialAxioms(Long partitionID) {
        RoaringBitmap initialAxiomsPos = partitionIDToInitialAxiomsIndex.get(partitionID);
        List<Object> partitionAxioms = null;
        if (initialAxiomsPos == null) {
            partitionAxioms = Collections.emptyList();
        } else {
            partitionAxioms = new ArrayList<>(initialAxiomsPos.getCardinality());
            PeekableIntIterator intIt = initialAxiomsPos.getIntIterator();
            while (intIt.hasNext()) {
                int pos = intIt.next();
                partitionAxioms.add(initialAxioms.get(pos));
            }
        }
        return partitionAxioms;
    }
}
