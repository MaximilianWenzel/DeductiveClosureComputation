package util;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;

import java.util.Set;

public class DistributedOWL2ELSaturationUtils {

    public static boolean isRelevantAxiomToPartition(Set<ELConcept> conceptPartition, ELConceptInclusion axiom) {
        if (conceptPartition.contains(axiom.getSubConcept())
                || conceptPartition.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            if (conceptPartition.contains(exist.getFiller())) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIfOtherPartitionsRequireAxiom(Set<ELConcept> conceptPartition, ELConceptInclusion axiom) {
        if (!conceptPartition.contains(axiom.getSubConcept())
                || !conceptPartition.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            return !conceptPartition.contains(exist.getFiller());
        }
        return false;
    }
}
