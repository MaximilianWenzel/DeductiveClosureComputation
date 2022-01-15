package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.apache.jena.graph.Triple;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * type(z, x) :- range(a, x), rel(y, a, z) .
 */
public class RuleRDFS3 extends Rule<RDFSClosure, TripleID> {

    long rdfTypeID;
    long rangeID;
    TripleID triplePattern = new TripleID();

    @Override
    public Stream<TripleID> streamOfInferences(TripleID axiom) {
        Stream.Builder<TripleID> inferences = Stream.builder();
        // given: rel(y, a, z)
        long a = axiom.getPredicate();
        long z = axiom.getObject();
        triplePattern.setAll(a, rangeID, 0);
        Iterator<TripleID> rangeRDFClassIDsForA = closure.search(triplePattern);

        rangeRDFClassIDsForA.forEachRemaining(tID -> {
            inferences.add(new TripleID(z, rdfTypeID, tID.getObject()));
        });

        // given: range(a, x)
        if (axiom.getPredicate() == rangeID) {
            long x = axiom.getObject();
            triplePattern.setAll(0, a, 0);
            Iterator<TripleID> itID = closure.search(triplePattern);
            itID.forEachRemaining(tID -> inferences.add(new TripleID(z, rdfTypeID, x)));
        }

        return inferences.build();
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.rangeID = closure.getRangeID();
        this.rdfTypeID = closure.getRDFTypeID();
    }
}
