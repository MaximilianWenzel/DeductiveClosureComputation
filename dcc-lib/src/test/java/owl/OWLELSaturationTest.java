package owl;

import benchmark.eldlreasoning.OWLELSaturationInitializationFactory;
import data.IndexedELOntology;
import eldlsyntax.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.ClosureComputationTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OWLELSaturationTest {

    public IndexedELOntology elOntology;

    @BeforeEach
    public void init() {
        elOntology = new IndexedELOntology();

        ELConcept conceptA = new ELConceptName("A");
        ELConcept conceptB = new ELConceptName("B");
        ELConcept conceptC = new ELConceptName("C");
        ELRoleName roleR = new ELRoleName("R");
        ELConceptExistentialRestriction existsR_B = new ELConceptExistentialRestriction(roleR, conceptB);
        ELConceptExistentialRestriction existsR_C = new ELConceptExistentialRestriction(roleR, conceptC);

        ELConceptInclusion conceptIncl_B_sub_C = new ELConceptInclusion(conceptB, conceptC);
        ELConceptInclusion conceptIncl_A_sub_existsR_B = new ELConceptInclusion(conceptA, existsR_B);
        ELConceptInclusion conceptIncl_existsR_C_sub_C = new ELConceptInclusion(existsR_C, conceptC);
        elOntology.add(conceptIncl_B_sub_C);
        elOntology.add(conceptIncl_A_sub_existsR_B);
        elOntology.add(conceptIncl_existsR_C_sub_C);
    }


    @Test
    public void testELOntology() {
        ELSignature s = elOntology.getSignature();

        // concept names = A, B, C
        assertEquals(3, s.conceptNames().count());

        // role names = R
        assertEquals(1, s.roleNames().count());

        // no individuals
        assertEquals(0, s.individuals().count());

    }

    @Test
    public void testSingleThreadedSaturation() {
        ELTBoxAxiom.Visitor tBoxVisitor = new ELTBoxAxiom.Visitor() {
            @Override
            public void visit(ELConceptInclusion axiom) {
                System.out.println(axiom.toString());
            }
        };
        System.out.println("TBox axioms:");
        elOntology.tBox().forEach(eltBoxAxiom -> eltBoxAxiom.accept(tBoxVisitor));

        System.out.println();
        OWLELSaturationInitializationFactory factory = new OWLELSaturationInitializationFactory(elOntology, 1);
        System.out.println(ClosureComputationTestUtil.singleThreadedClosureComputation(factory));
    }

    @Test
    public void testParallelSaturation() {
        OWLELSaturationInitializationFactory factory = new OWLELSaturationInitializationFactory(elOntology, 2);
        ClosureComputationTestUtil.parallelClosureComputation(factory);

        factory = new OWLELSaturationInitializationFactory(elOntology, 4);
        ClosureComputationTestUtil.parallelClosureComputation(factory);
    }

    @Test
    public void testDistributedSaturation() {
        OWLELSaturationInitializationFactory factory = new OWLELSaturationInitializationFactory(elOntology, 2);
        ClosureComputationTestUtil.distributedClosureComputation(factory, false, 2);

        init();
        factory = new OWLELSaturationInitializationFactory(elOntology, 4);
        ClosureComputationTestUtil.distributedClosureComputation(factory, false, 2);
    }


}
