package elsyntax;

import eldlreasoning.expressions.Expression;
import eldlreasoning.saturator.SingleThreadedELSaturator;
import eldlsyntax.*;
import org.junit.jupiter.api.Test;
import util.OWL2ELToExpressionConverter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ELOntologyTest {

    public static final ELOntology elOntology;

    static {
        elOntology = new ELOntology();

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
        ELOntology elOntology = ELOntologyTest.elOntology;

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
        OWL2ELToExpressionConverter converter = new OWL2ELToExpressionConverter();
        Set<Expression> expressions = converter.convert(ELOntologyTest.elOntology.tBox());
        SingleThreadedELSaturator saturator = new SingleThreadedELSaturator(expressions);
        Set<Expression> closure = saturator.saturate();

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
