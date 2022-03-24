package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * subPropertyOf(x, rdfs:member) :- type(x, rdfs:ContainerMembershipProperty) .
 */
public class RuleRDFS13 extends Rule<RDFSClosure, TripleID> {
    long subPropertyOfID;
    long rdfTypeID;
    long rdfsContainerMembershipPropertyID;
    long rdfsMemberID;

    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        if (axiom.getPredicate() != rdfTypeID || axiom.getObject() != rdfsContainerMembershipPropertyID) {
            return Stream.empty();
        }
        // given: type(x, rdfs:Datatype)
        long x = axiom.getSubject();
        return Stream.of(new TripleID(x, subPropertyOfID, rdfsMemberID));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.subPropertyOfID = closure.getSubPropertyOfID();
        this.rdfTypeID = closure.getRDFTypeID();
        this.rdfsContainerMembershipPropertyID = closure.getRDFSContainerMembershipPropertyID();
        this.rdfsMemberID = closure.geRDFSMemberID();
    }
}
