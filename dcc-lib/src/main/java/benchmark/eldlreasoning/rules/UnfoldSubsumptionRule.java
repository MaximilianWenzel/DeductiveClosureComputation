package benchmark.eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;

import java.util.Set;
import java.util.stream.Stream;

/**
 * derived(C ⊑ E) ⇐ derived(C ⊑ D) : told(D ⊑ E)
 */
public class UnfoldSubsumptionRule extends OWLELRule {

    private Iterable<ELConceptInclusion> ontology;

    public UnfoldSubsumptionRule(Set<ELConceptInclusion> ontology) {
        super();
        this.ontology = ontology;
    }

    public UnfoldSubsumptionRule() {

    }

    @Override
    public Stream<ELConceptInclusion> streamOfConclusions(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> conclusions = Stream.builder();
        ELConcept c = axiom.getSubConcept();
        ELConcept d = axiom.getSuperConcept();

        for (ELConceptInclusion conceptIncl : ontology) {
            if (d.equals(conceptIncl.getSubConcept())) {
                conclusions.add(new ELConceptInclusion(c, conceptIncl.getSuperConcept()));
            }
        }
        return conclusions.build();
    }
}
