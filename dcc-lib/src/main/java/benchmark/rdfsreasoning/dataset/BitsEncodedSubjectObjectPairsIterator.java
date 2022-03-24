package benchmark.rdfsreasoning.dataset;

import org.rdfhdt.hdt.triples.TripleID;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BitsEncodedSubjectObjectPairsIterator implements Iterator<TripleID> {
    Iterator<Long> predicateIDIt;
    Iterator<Set<Long>> sbjObjPairIt;
    Iterator<Long> currentSbjObjPairsIt = Collections.emptyIterator();
    TripleID current = new TripleID();

    public BitsEncodedSubjectObjectPairsIterator(Map<Long, Set<Long>> predicateIDToSubjectObjectPair) {
        predicateIDIt = predicateIDToSubjectObjectPair.keySet().iterator();
        sbjObjPairIt = predicateIDToSubjectObjectPair.values().iterator();
        initNext();
    }

    private void initNext() {
        if (!currentSbjObjPairsIt.hasNext() && sbjObjPairIt.hasNext()) {
            current.setPredicate(predicateIDIt.next());
            currentSbjObjPairsIt = sbjObjPairIt.next().iterator();
        } else if (!currentSbjObjPairsIt.hasNext() && !sbjObjPairIt.hasNext()) {
            return;
        }
        long sbjObjPair = currentSbjObjPairsIt.next();
        current.setSubject(sbjObjPair >> 32);
        current.setObject((Long.MAX_VALUE >> 32) & sbjObjPair);
    }

    @Override
    public boolean hasNext() {
        return sbjObjPairIt.hasNext() || predicateIDIt.hasNext();
    }

    @Override
    public TripleID next() {
        initNext();
        return current;
    }
}
