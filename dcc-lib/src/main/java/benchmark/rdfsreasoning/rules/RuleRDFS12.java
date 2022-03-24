package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * subClassOf(x, rdfs:Literal) :- type(x, rdfs:Datatype) .
 */
public class RuleRDFS12 extends Rule<RDFSClosure, TripleID> {
    long subClassOfID;
    long rdfTypeID;
    long rdfsDatatypeID;
    long rdfsLiteralID;

    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        if (axiom.getPredicate() != rdfTypeID || axiom.getObject() != rdfsDatatypeID) {
            return Stream.empty();
        }
        // given: type(x, rdfs:Datatype)
        long x = axiom.getSubject();
        return Stream.of(new TripleID(x, subClassOfID, rdfsLiteralID));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.subClassOfID = closure.getSubClassOfID();
        this.rdfTypeID = closure.getRDFTypeID();
        this.rdfsDatatypeID = closure.getRDFSDatatypeID();
        this.rdfsLiteralID = closure.getRDFSLiteralID();
    }
}
