package reasoning.saturation.workload;

import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InitialAxiomsDistributor<A extends Serializable> {

    private Map<Long, RoaringBitmap> workerIDToInitialAxiomsIndex;
    private List<? extends A> initialAxioms;
    private WorkloadDistributor<?, A> workloadDistributor;

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
