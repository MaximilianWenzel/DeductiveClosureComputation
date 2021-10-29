package eldlreasoning.premise;

import eldlreasoning.expression.Expression;
import eldlreasoning.expression.InitExpression;
import eldlreasoning.expression.LinkExpression;
import eldlreasoning.expression.SubsumptionExpression;
import eldlreasoning.models.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ELPremiseContext {
    private final IdxConcept top = new IdxAtomicConcept("⊤");
    private final IdxConcept bottom = new IdxAtomicConcept("⊥");

    private final Set<InitExpression> inits = new UnifiedSet<>();
    private final Set<SubsumptionExpression> subs = new UnifiedSet<>();
    private final Set<LinkExpression> links = new UnifiedSet<>();

    private final Set<IdxConcept> usedConceptsInOntology = new UnifiedSet<>();

    // lookup maps
    private final Set<IdxConjunction> negativeConjunctions = new UnifiedSet<>();
    private final Set<IdxExistential> negativeExists = new UnifiedSet<>();
    private final Map<IdxConcept, Set<IdxConcept>> conceptInclusions = new HashMap<>();
    private final Map<IdxAtomicRole, Set<IdxAtomicRole>> roleInclusions = new HashMap<>();

    // objects for lookup
    private final ModifiableIdxConjunction conjForLookup = new ModifiableIdxConjunction(top, top);


    public ELPremiseContext(Iterable<Expression> inputExpressions) {
        inputExpressions.forEach(expr -> {
            expr.streamOfConcepts().forEach(usedConceptsInOntology::add);
        });

        initLookupMaps();
    }

    private void initLookupMaps() {
        subs.forEach(sub -> {
            if (sub.getFirstConcept() instanceof IdxExistential) {
                negativeExists.add((IdxExistential) sub.getFirstConcept());
            } else if (sub.getFirstConcept() instanceof IdxConjunction) {
                negativeConjunctions.add((IdxConjunction) sub.getFirstConcept());
            }
            Set<IdxConcept> supertypeConcepts = conceptInclusions.getOrDefault(sub.getFirstConcept(), new UnifiedSet<>());
            if (supertypeConcepts.isEmpty()) {
                conceptInclusions.put(sub.getFirstConcept(), supertypeConcepts);
            }
            supertypeConcepts.add(sub.getSecondConcept());
        });
    }

    public IdxConcept getTop() {
        return top;
    }

    public IdxConcept getBottom() {
        return bottom;
    }

    /**
     *
     * @return {@code true} if the given expression has not been considered before
     */
    public boolean add(SubsumptionExpression subExpr) {
        boolean isNewExpr = this.subs.add(subExpr);
        if (isNewExpr) {
            // process expression for indices and lookup tables

            // concept inclusions
            Set<IdxConcept> supertypeConcepts = this.conceptInclusions.getOrDefault(subExpr.getFirstConcept(), new UnifiedSet<>());
            if (supertypeConcepts.isEmpty()) {
                this.conceptInclusions.put(subExpr.getFirstConcept(), supertypeConcepts);
            }
            supertypeConcepts.add(subExpr.getSecondConcept());

            // negative existential
            if (subExpr.getFirstConcept() instanceof IdxExistential) {
                this.negativeExists.add((IdxExistential) subExpr.getFirstConcept());
            }

            // negative conjunctions
            if (subExpr.getFirstConcept() instanceof IdxConjunction) {
                this.negativeConjunctions.add((IdxConjunction) subExpr.getFirstConcept());
            }

            // increment positive and negative occurrence counter
            subExpr.getFirstConcept().getNegOccurs().incrementAndGet();
            subExpr.getSecondConcept().getPosOccurs().incrementAndGet();
        }
        return isNewExpr;
    }

    /**
     *
     * @return {@code true} if the given expression has not been considered before
     */
    public boolean add(InitExpression initExpr) {
        return this.inits.add(initExpr);
    }

    /**
     *
     * @return {@code true} if the given expression has not been considered before
     */
    public boolean add(LinkExpression linkExpr) {
        return this.links.add(linkExpr);
    }

    public boolean isNegativeConjunction(IdxConcept concept1, IdxConcept concept2) {
        conjForLookup.setFirstConcept(concept1);
        conjForLookup.setSecondConcept(concept2);
        return this.negativeConjunctions.contains(conjForLookup);
    }


    public Set<IdxConcept> getSupertypeConcepts(IdxConcept concept) {
        return this.conceptInclusions.getOrDefault(concept, Collections.emptySet());
    }

    public boolean isNegativeConjunction(IdxConjunction conjunction) {
        return this.negativeConjunctions.contains(conjunction);
    }

    public boolean isNegativeExistential(IdxExistential existential) {
        return this.negativeExists.contains(existential);
    }

    public boolean checkIfConceptIsUsedInOntology(IdxConcept concept) {
        return this.usedConceptsInOntology.contains(concept);
    }


}
