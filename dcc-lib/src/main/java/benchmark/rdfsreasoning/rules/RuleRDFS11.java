package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * subClassOf(x, z) :- subClassOf(x, y), subClassOf(y, z) .
 */
public class RuleRDFS11 extends Rule<RDFSClosure, TripleID> {
    TripleID triplePattern = new TripleID();
    long subClassOfID;

    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        if (axiom.getPredicate() != subClassOfID) {
            return Stream.empty();
        }

        Stream.Builder<TripleID> conclusions = Stream.builder();

        // given: subClassOf(x, y)
        long y = axiom.getObject();

        triplePattern.setAll(y, subClassOfID, 0);
        Iterator<TripleID> itID = closure.search(triplePattern);
        // find: subClassOf(y, z)
        itID.forEachRemaining(tID -> conclusions.add(new TripleID(tID.getSubject(), subClassOfID, tID.getObject())));

        // given: subClassOf(y, z)
        y = axiom.getSubject();

        triplePattern.setAll(0, subClassOfID, y);
        itID = closure.search(triplePattern);
        // find: subClassOf(x, y)
        itID.forEachRemaining(tID -> conclusions.add(new TripleID(tID.getSubject(), subClassOfID, tID.getObject())));

        return conclusions.build();
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.subClassOfID = closure.getSubClassOfID();
    }
}
