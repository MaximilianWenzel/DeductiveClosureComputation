package eldlreasoning.models;

import eldlsyntax.ELOntology;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class ELOntologyIndex {

    private static AtomicLong conceptIDCounter = new AtomicLong(1);
    private static AtomicLong roleIDCounter = new AtomicLong(1);

    Set<IdxConjunction> negativeConjunctions;
    Set<IdxExistential> negativeExists;
    Map<IdxConcept, IdxConcept> conceptInclusions;
    Map<IdxAtomicRole, IdxAtomicRole> roleInclusions;
    //Map<IdxRoleComposition, IdxAtomicRole> roleInclusions;


    public ELOntologyIndex(ELOntology elOntology) {

    }

}
