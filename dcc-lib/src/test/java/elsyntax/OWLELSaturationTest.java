package elsyntax;

import data.DefaultClosure;
import data.IndexedELOntology;
import eldlreasoning.OWLELDistributedPartitionFactory;
import eldlreasoning.OWLELWorkloadDistributor;
import eldlreasoning.rules.OWLELRule;
import eldlsyntax.*;
import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.models.DistributedWorkerModel;
import util.OWL2ELSaturationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        getClosureOfSingleThreadedSaturator().forEach(System.out::println);
    }

    public Set<Object> getClosureOfSingleThreadedSaturator() {
        Collection<OWLELRule> owlelRules = OWL2ELSaturationUtils.getOWL2ELRules(elOntology);
        SingleThreadedSaturation saturation = new SingleThreadedSaturation(elOntology.getInitialAxioms().iterator(), owlelRules);
        Set<Object> closure = saturation.saturate();
        return closure;
    }

    /*
    public Set<ELConceptInclusion> getClosureOfParallelSaturator() {
        OWL2ELSaturationControlNode saturator = new OWL2ELSaturationControlNode(elOntology, 3);
        saturator.init();
        return saturator.saturate();
    }

     */

    /*
    @Test
    public void testParallelSaturation() {
        Set<ELConceptInclusion> closure = getClosureOfParallelSaturator();

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

        assertEquals(getClosureOfSingleThreadedSaturator(), closure);
    }

     */

    @Test
    void testDistributedSaturation() {
        List<ServerData> partitionServerData = new ArrayList<>();
        for (int portNumber = 50000; portNumber < 50002; portNumber++) {
            partitionServerData.add(new ServerData("localhost", portNumber));
        }

        List<SaturationWorker> saturationWorkers = new ArrayList<>();
        for (ServerData serverData : partitionServerData) {
            SaturationWorker partition = new SaturationWorker(
                    serverData.getPortNumber(),
                    10,
                    new DefaultClosure(),
                    SaturationWorker.IncrementalReasonerType.SINGLE_THREADED
            );
            saturationWorkers.add(partition);
        }
        saturationWorkers.forEach(p -> new Thread(p).start());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        OWLELDistributedPartitionFactory partitionFactory = new OWLELDistributedPartitionFactory(elOntology, partitionServerData);
        List<DistributedWorkerModel> partitionModels = partitionFactory.generateDistributedPartitions();
        OWLELWorkloadDistributor workloadDistributor = new OWLELWorkloadDistributor(partitionModels);
        DistributedSaturation distributedSaturation = new DistributedSaturation(partitionModels, workloadDistributor, elOntology.getInitialAxioms());

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

        Set<Object> distributedClosure = distributedSaturation.saturate();
        Set<Object> singleThreadedClosure = getClosureOfSingleThreadedSaturator();
        Set<Object> difference = new UnifiedSet<>(singleThreadedClosure);
        difference.removeAll(distributedClosure);
        System.out.println(difference);
        assertEquals(singleThreadedClosure, distributedClosure);

    }
}
