package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * type(z, y) :- subClassOf(x, y), type(z, x) .
 */
public class RuleRDFS9 extends Rule<RDFSClosure, TripleID> {

    long rdfTypeID;
    long subClassOfID;
    TripleID triplePattern = new TripleID();

    @Override
    public Stream<TripleID> streamOfInferences(TripleID axiom) {
        Stream.Builder<TripleID> inferences = Stream.builder();
        if (axiom.getPredicate() == rdfTypeID) {
            // given: type(z, x)
            long z = axiom.getSubject();
            long x = axiom.getObject();
            // find: subClassOf(x, y)
            triplePattern.setAll(x, subClassOfID, 0);
            Iterator<TripleID> itID = closure.search(triplePattern);

            // derive: type(z, y)
            itID.forEachRemaining(tID -> {
                inferences.add(new TripleID(z, rdfTypeID, tID.getObject()));
            });
        }

        // given: subClassOf(x, y)
        if (axiom.getPredicate() == subClassOfID) {
            long x = axiom.getSubject();
            long y = axiom.getObject();
            // find: type(z, x)
            triplePattern.setAll(0, rdfTypeID, x);
            Iterator<TripleID> itID = closure.search(triplePattern);
            // derive: type(z, y)
            itID.forEachRemaining(tID -> inferences.add(new TripleID(tID.getSubject(), rdfTypeID, y)));
        }

        return inferences.build();
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.subClassOfID = closure.getSubClassOfID();
        this.rdfTypeID = closure.getRDFTypeID();
    }
}
