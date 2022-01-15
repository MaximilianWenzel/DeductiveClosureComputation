package benchmark.rdfsreasoning;

import benchmark.rdfsreasoning.dataset.RDFDataset;
import benchmark.rdfsreasoning.dataset.RDFSReasoningDictionary;
import benchmark.rdfsreasoning.dataset.RDFSReasoningTriples;
import benchmark.rdfsreasoning.rules.*;
import benchmark.transitiveclosure.Reachability;
import benchmark.transitiveclosure.ReachabilityClosure;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.triples.TripleID;
import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RDFSSaturationInitializationFactory
        extends SaturationInitializationFactory<RDFSClosure, TripleID, RDFSPartitionCollection> {

    private RDFDataset rdfDataset;
    private RDFSReasoningTriples triples;
    private RDFSReasoningDictionary dictionary;

    private int numberOfWorkers;
    private List<WorkerModel<RDFSClosure, TripleID, RDFSPartitionCollection>> workerModels = null;

    public RDFSSaturationInitializationFactory(RDFDataset rdfDataset, int numberOfWorkers) {
        this.rdfDataset = rdfDataset;
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public List<WorkerModel<RDFSClosure, TripleID, RDFSPartitionCollection>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }

        List<Set<Long>> propertyIDsForWorkers = new ArrayList<>(this.numberOfWorkers);
        List<Set<Long>> rdfClassIDsForWorkers = new ArrayList<>(this.numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            propertyIDsForWorkers.add(new UnifiedSet<>());
            rdfClassIDsForWorkers.add(new UnifiedSet<>());
        }

        int i = 0;
        for (Long rdfClassID : dictionary.getRDFClassIDs()) {
            rdfClassIDsForWorkers.get(i % numberOfWorkers).add(rdfClassID);
            i++;
        }

        for (Long propertyID : dictionary.getPropertyIDs()) {
            propertyIDsForWorkers.get(i % numberOfWorkers).add(propertyID);
            i++;
        }
        List<RDFSPartitionCollection> rdfsPartitionCollections = new ArrayList<>(this.numberOfWorkers);

        List<WorkerModel<RDFSClosure, TripleID, RDFSPartitionCollection>> workers = new ArrayList<>();
        for (RDFSPartitionCollection partitionCollection : rdfsPartitionCollections) {
            WorkerModel<RDFSClosure, TripleID, RDFSPartitionCollection> workerModel = new WorkerModel<>(
                    getNewClosure(),
                    generateRules(),
                    partitionCollection
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
    public WorkloadDistributor<RDFSClosure, TripleID, RDFSPartitionCollection> getWorkloadDistributor() {
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
