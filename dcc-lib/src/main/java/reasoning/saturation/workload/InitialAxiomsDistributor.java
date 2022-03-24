package reasoning.saturation.workload;

import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * If the initial axioms are not assigned to appropriate workers using the hash-based partitioning approach, this class can be deployed in
 * order to assign the responsible worker IDs to the initial axioms in a memory efficient way.
 *
 * @param <A> Types of the deployed axioms.
 */
public class InitialAxiomsDistributor<A extends Serializable> {

    private final List<? extends A> initialAxioms;
    private final WorkloadDistributor<?, A> workloadDistributor;
    private Map<Long, RoaringBitmap> workerIDToInitialAxiomsIndex;

    public InitialAxiomsDistributor(List<? extends A> initialAxioms, WorkloadDistributor<?, A> workloadDistributor) {
        this.initialAxioms = initialAxioms;
        this.workloadDistributor = workloadDistributor;
        init();
    }

    private void init() {
        // distribute initial axioms across workers
        this.workerIDToInitialAxiomsIndex = new HashMap<>();
        for (int i = 0; i < initialAxioms.size(); i++) {
            List<Long> relevantWorkers = workloadDistributor.getRelevantWorkerIDsForAxiom(initialAxioms.get(i)).collect(
                    Collectors.toList());
            for (Long workerID : relevantWorkers) {
                RoaringBitmap relevantAxiomsForWorker = workerIDToInitialAxiomsIndex.computeIfAbsent(workerID, pID -> new RoaringBitmap());
                relevantAxiomsForWorker.add(i);
            }
        }
    }

    public List<A> getInitialAxioms(Long workerID) {
        RoaringBitmap initialAxiomsPos = workerIDToInitialAxiomsIndex.get(workerID);
        List<A> workerAxioms;
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
