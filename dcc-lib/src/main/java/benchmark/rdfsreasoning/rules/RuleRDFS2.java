package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * type(y, x) :- domain(a, x), rel(y, a, z) .
 */
public class RuleRDFS2 extends Rule<RDFSClosure, TripleID> {

    long rdfTypeID;
    long domainID;
    TripleID triplePattern = new TripleID();

    @Override
    public Stream<TripleID> streamOfInferences(TripleID axiom) {
        Stream.Builder<TripleID> inferences = Stream.builder();

        // given: rel(y, a, z)
        long a = axiom.getPredicate();
        long y = axiom.getSubject();
        triplePattern.setAll(a, domainID, 0);
        Iterator<TripleID> domainRDFClassIDsForA = closure.search(triplePattern);

        domainRDFClassIDsForA.forEachRemaining(tID -> {
            inferences.add(new TripleID(y, rdfTypeID, tID.getObject()));
        });

        // given: domain(a, x)
        if (axiom.getPredicate() == domainID) {
            long x = axiom.getObject();
            triplePattern.setAll(0, a, 0);
            Iterator<TripleID> itID = closure.search(triplePattern);
            itID.forEachRemaining(tID -> inferences.add(new TripleID(y, rdfTypeID, x)));
        }

        return inferences.build();
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.domainID = closure.getDomainID();
        this.rdfTypeID = closure.getRDFTypeID();
    }
}
