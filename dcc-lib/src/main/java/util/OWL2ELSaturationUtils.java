package util;

import data.IndexedELOntology;
import eldlreasoning.rules.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Set;

public class OWL2ELSaturationUtils {

    public static Set<OWLELRule> getOWL2ELRules(IndexedELOntology elOntology) {
        Set<OWLELRule> rules = new UnifiedSet<>();
        rules.add(new ComposeConjunctionRule(elOntology.getNegativeConcepts()));
        rules.add(new DecomposeConjunctionRule());
        rules.add(new ReflexiveSubsumptionRule());
        rules.add(new SubsumedByTopRule(elOntology.getTop()));
        rules.add(new UnfoldExistentialRule());
        rules.add(new UnfoldSubsumptionRule(elOntology.getOntologyAxioms()));
        return rules;
    }
}
