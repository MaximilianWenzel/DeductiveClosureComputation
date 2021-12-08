package elsyntax;

import benchmark.SaturationWorkerServerGenerator;
import data.Closure;
import data.DefaultClosure;
import data.IndexedELOntology;
import eldlreasoning.OWLELDistributedWorkerFactory;
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
import reasoning.saturation.distributed.communication.BenchmarkConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import util.OWL2ELSaturationUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

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

    public DefaultClosure<ELConceptInclusion> getClosureOfSingleThreadedSaturator() {
        Collection<OWLELRule> owlelRules = OWL2ELSaturationUtils.getOWL2ELRules(elOntology);
        DefaultClosure<ELConceptInclusion> closure = new DefaultClosure<>();
        SingleThreadedSaturation<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> saturation =
                new SingleThreadedSaturation<>(elOntology.getInitialAxioms().iterator(), owlelRules, closure);
        return saturation.saturate();
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


    void testDistributedSaturation() {
        BenchmarkConfiguration benchmarkConfiguration = new BenchmarkConfiguration(10);
        SaturationWorkerServerGenerator<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> workerFactory;
        workerFactory = new SaturationWorkerServerGenerator<>(benchmarkConfiguration, 3, new Callable<DefaultClosure<ELConceptInclusion>>() {
            @Override
            public DefaultClosure<ELConceptInclusion> call() throws Exception {
                return new DefaultClosure<>();
            }
        });

        List<SaturationWorker<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> saturationWorkers;
        saturationWorkers = workerFactory.generateWorkers();
        saturationWorkers.forEach(p -> new Thread(p).start());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<ServerData> workerServerData = workerFactory.getServerDataList();
        OWLELDistributedWorkerFactory owlWorkerFactory = new OWLELDistributedWorkerFactory(elOntology, workerServerData);
        List<DistributedWorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> workerModels =
                owlWorkerFactory.generateDistributedWorkers();
        OWLELWorkloadDistributor workloadDistributor = new OWLELWorkloadDistributor(workerModels);
        DistributedSaturation<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> distributedSaturation = new DistributedSaturation<>(
                benchmarkConfiguration, workerModels,workloadDistributor, elOntology.getInitialAxioms(), new DefaultClosure<>());

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

        Closure<ELConceptInclusion> distributedClosure = distributedSaturation.saturate();
        Closure<ELConceptInclusion> singleThreadedClosure = getClosureOfSingleThreadedSaturator();

        Set<ELConceptInclusion> difference = new UnifiedSet<>();
        singleThreadedClosure.getClosureResults().forEach(difference::add);
        distributedClosure.getClosureResults().forEach(difference::remove);

        System.out.println(difference);
        assertEquals(singleThreadedClosure, distributedClosure);

    }
}
