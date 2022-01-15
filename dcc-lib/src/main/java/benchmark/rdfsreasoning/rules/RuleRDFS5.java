package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * subPropertyOf(x, z) :- subPropertyOf(x, y), subPropertyOf(y, z) .
 */
public class RuleRDFS5 extends Rule<RDFSClosure, TripleID> {
    TripleID triplePattern = new TripleID();
    long subPropertyOfID;

    @Override
    public Stream<TripleID> streamOfInferences(TripleID axiom) {
        if (axiom.getPredicate() != subPropertyOfID) {
            return Stream.empty();
        }

        Stream.Builder<TripleID> inferences = Stream.builder();

        // given: subPropertyOf(x, y)
        long y = axiom.getObject();

        triplePattern.setAll(y, subPropertyOfID, 0);
        Iterator<TripleID> itID = closure.search(triplePattern);
        // find: subPropertyOf(y, z)
        itID.forEachRemaining(tID -> inferences.add(new TripleID(tID.getSubject(), subPropertyOfID, tID.getObject())));

        // given: subPropertyOf(y, z)
        y = axiom.getSubject();

        triplePattern.setAll(0, subPropertyOfID, y);
        itID = closure.search(triplePattern);
        // find: subPropertyOf(x, y)
        itID.forEachRemaining(tID -> inferences.add(new TripleID(tID.getSubject(), subPropertyOfID, tID.getObject())));

        return inferences.build();
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.subPropertyOfID = closure.getSubPropertyOfID();
    }
}
