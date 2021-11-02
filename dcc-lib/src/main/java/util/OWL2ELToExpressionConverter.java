package util;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.InitExpression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.*;
import eldlsyntax.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class OWL2ELToExpressionConverter implements ELObject.Visitor {

    private Set<Expression> expressions;
    private Map<ELConcept, IdxConcept> conceptToIndexedConcept;
    private Map<ELRole, IdxRole> roleToIndexedRole;

    public OWL2ELToExpressionConverter() {

    }

    public Set<Expression> convert(Stream<? extends ELObject> owl2ELExpressions) {
        this.expressions = new UnifiedSet<>();
        this.conceptToIndexedConcept = new HashMap<>();
        this.roleToIndexedRole = new HashMap<>();
        owl2ELExpressions.forEach(e -> e.accept(this));
        return expressions;
    }

    @Override
    public void visit(ELConceptAssertion axiom) {
    }

    @Override
    public void visit(ELRoleAssertion axiom) {
    }

    @Override
    public void visit(ELConceptBottom concept) {
        processAndGetIndexedConcept(concept);
    }

    @Override
    public void visit(ELConceptConjunction concept) {
        processAndGetIndexedConcept(concept);
    }

    @Override
    public void visit(ELConceptExistentialRestriction concept) {
        processAndGetIndexedConcept(concept);
    }

    @Override
    public void visit(ELConceptName concept) {
        processAndGetIndexedConcept(concept);
    }

    @Override
    public void visit(ELConceptTop concept) {
        processAndGetIndexedConcept(concept);
    }

    @Override
    public void visit(ELIndividual individual) {
    }

    @Override
    public void visit(ELRoleName role) {
    }

    @Override
    public void visit(ELConceptInclusion axiom) {
        this.expressions.add(new SubsumptionExpression(processAndGetIndexedConcept(axiom.getSubConcept()),
                processAndGetIndexedConcept(axiom.getSuperConcept())));
    }

    public IdxConcept processAndGetIndexedConcept(ELConcept concept) {
        // TODO implement more efficient strategy (without recursion?)
        IdxConcept idxConcept;
        if (concept instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction elExistRestrict = (ELConceptExistentialRestriction) concept;
            IdxExistential idxExist = new IdxExistential(getIdxRoleFromRole(elExistRestrict.getRelation()),
                    processAndGetIndexedConcept(elExistRestrict.getFiller()));
            return this.conceptToIndexedConcept.computeIfAbsent(elExistRestrict, e -> idxExist);

        } else if (concept instanceof ELConceptBottom) {
            idxConcept = this.conceptToIndexedConcept.computeIfAbsent(concept,
                    e -> {
                        IdxConcept c = new IdxAtomicConcept("⊥");
                        this.expressions.add(new InitExpression(c));
                        return c;
                    });
            this.expressions.add(new InitExpression(idxConcept));
        } else if (concept instanceof ELConceptName) {
            idxConcept = this.conceptToIndexedConcept.computeIfAbsent(concept,
                    e -> {
                        IdxConcept c = new IdxAtomicConcept(((ELConceptName) concept).getName());
                        this.expressions.add(new InitExpression(c));
                        return c;
                    });
            this.expressions.add(new InitExpression(idxConcept));

        } else if (concept instanceof ELConceptConjunction) {
            ELConceptConjunction elConceptConjunction = (ELConceptConjunction) concept;
            IdxConjunction idxConj = new IdxConjunction(processAndGetIndexedConcept(elConceptConjunction.getFirstConjunct()),
                    processAndGetIndexedConcept(elConceptConjunction.getSecondConjunct()));
            idxConcept = this.conceptToIndexedConcept.computeIfAbsent(elConceptConjunction, e -> {
                this.expressions.add(new InitExpression(idxConj));
                return idxConj;
            });
        } else if (concept instanceof ELConceptTop) {
            idxConcept = this.conceptToIndexedConcept.computeIfAbsent(concept,
                    e -> {
                        IdxConcept c = new IdxAtomicConcept("⊤");
                        this.expressions.add(new InitExpression(c));
                        return c;
                    });
        } else {
            throw new IllegalArgumentException("OWL EL concept not supported: " + concept.getClass());
        }
        return idxConcept;
    }

    public IdxRole getIdxRoleFromRole(ELRole elRole) {
        if (elRole instanceof ELRoleName) {
            ELRoleName elRoleName = (ELRoleName) elRole;
            return this.roleToIndexedRole.computeIfAbsent(elRole, e -> new IdxAtomicRole(elRoleName.getName()));
        } else {
            throw new IllegalArgumentException("OWL EL role type not supported: " + elRole.getClass());
        }
    }
}
