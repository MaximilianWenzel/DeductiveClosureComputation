package elsyntax;

import eldlreasoning.saturator.SingleThreadedELSaturator;
import eldlreasoning.saturator.parallel.ParallelELSaturator;
import eldlsyntax.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
        SingleThreadedELSaturator saturator = new SingleThreadedELSaturator(elOntology);
        Set<ELConceptInclusion> closure = saturator.saturate();

        ELTBoxAxiom.Visitor tBoxVisitor = new ELTBoxAxiom.Visitor() {
            @Override
            public void visit(ELConceptInclusion axiom) {
                System.out.println(axiom.toString());
            }
        };
        System.out.println("TBox axioms:");
        elOntology.tBox().forEach(eltBoxAxiom -> eltBoxAxiom.accept(tBoxVisitor));

        System.out.println();
        System.out.println("Closure: ");
        closure.forEach(System.out::println);
    }

    @Test
    public void testParallelSaturation() {
        ParallelELSaturator saturator = new ParallelELSaturator(elOntology, 3);
        Set<ELConceptInclusion> closure = saturator.saturate();

        ELTBoxAxiom.Visitor tBoxVisitor = new ELTBoxAxiom.Visitor() {
            @Override
            public void visit(ELConceptInclusion axiom) {
                System.out.println(axiom.toString());
            }
        };
        System.out.println("TBox axioms:");
        elOntology.tBox().forEach(eltBoxAxiom -> eltBoxAxiom.accept(tBoxVisitor));

        System.out.println();
        System.out.println("Closure: ");
        closure.forEach(System.out::println);
    }
}
