package benchmark.rdfsreasoning;

import benchmark.rdfsreasoning.dataset.RDFDataset;
import benchmark.rdfsreasoning.dataset.RDFSReasoningDictionary;
import benchmark.rdfsreasoning.dataset.RDFSReasoningTriples;
import benchmark.rdfsreasoning.rules.*;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RDFSSaturationInitializationFactory
        extends SaturationInitializationFactory<RDFSClosure, TripleID> {

    private final RDFDataset rdfDataset;
    private RDFSReasoningTriples triples;
    private RDFSReasoningDictionary dictionary;

    private final int numberOfWorkers;
    private List<WorkerModel<RDFSClosure, TripleID>> workerModels = null;

    public RDFSSaturationInitializationFactory(RDFDataset rdfDataset, int numberOfWorkers) {
        this.rdfDataset = rdfDataset;
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public List<WorkerModel<RDFSClosure, TripleID>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }

        List<WorkerModel<RDFSClosure, TripleID>> workers = new ArrayList<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            WorkerModel<RDFSClosure, TripleID> workerModel = new WorkerModel<>(
                    i + 1,
                    getNewClosure(),
                    generateRules()
            );
            workers.add(workerModel);
        }
        workerModels = workers;
        return workerModels;
    }

    @Override
    public Iterator<TripleID> getInitialAxioms() {
        return rdfDataset.getTriples().search(new TripleID(0, 0, 0));
    }

    @Override
    public RDFSClosure getNewClosure() {
        // TODO
        return null;
    }

    @Override
    public WorkloadDistributor<RDFSClosure, TripleID> getWorkloadDistributor() {
        // TODO
        return null;
    }

    @Override
    public List<Rule<RDFSClosure, TripleID>> generateRules() {
        List<Rule<RDFSClosure, TripleID>> rules = new ArrayList<>();
        //rules.add(new RuleGRDFD1()); TODO: remove if not required
        rules.add(new RuleGRDFD2());
        //rules.add(new RuleRDFS1()); TODO: remove if not required
        rules.add(new RuleRDFS2());
        rules.add(new RuleRDFS3());
        rules.add(new RuleRDFS4a());
        rules.add(new RuleRDFS4b());
        rules.add(new RuleRDFS5());
        rules.add(new RuleRDFS6());
        rules.add(new RuleRDFS7());
        rules.add(new RuleRDFS8());
        rules.add(new RuleRDFS9());
        rules.add(new RuleRDFS10());
        rules.add(new RuleRDFS11());
        rules.add(new RuleRDFS12());
        return rules;
    }

    @Override
    public void resetFactory() {
        workerModels = null;
    }
}
