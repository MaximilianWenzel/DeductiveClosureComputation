package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * rel(x, b, y) :- subPropertyOf(a, b), rel(x, a, y) .
 */
public class RuleRDFS7 extends Rule<RDFSClosure, TripleID> {
    TripleID triplePattern = new TripleID();
    long subPropertyOfID;

    @Override
    public Stream<TripleID> streamOfInferences(TripleID axiom) {
        Stream.Builder<TripleID> inferences = Stream.builder();
        if (axiom.getPredicate() == subPropertyOfID) {
            // given: subPropertyOf(a, b)
            long a = axiom.getSubject();
            long b = axiom.getObject();

            // find: rel(x, a, y)
            triplePattern.setAll(0, a, 0);
            Iterator<TripleID> itID = closure.search(triplePattern);
            itID.forEachRemaining(tID -> inferences.add(new TripleID(tID.getSubject(), b, tID.getObject())));
        }

        // given: rel(x, a, y)
        long x = axiom.getSubject();
        long a = axiom.getPredicate();
        long y = axiom.getObject();
        triplePattern.setAll(a, subPropertyOfID, 0);
        // find: subPropertyOf(a, b)
        Iterator<TripleID> itID = closure.search(triplePattern);
        itID.forEachRemaining(tID -> inferences.add(new TripleID(x, tID.getPredicate(), y)));

        return inferences.build();
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.subPropertyOfID = closure.getSubPropertyOfID();
    }

}
