package eldlreasoning.saturator.parallel;

import eldlsyntax.ELConceptInclusion;
import eldlsyntax.IndexedELOntology;

import java.util.Set;

public class ParallelELSaturator {

    private IndexedELOntology elOntology;
    private WorkloadManager workloadManager;
    private int numberOfPartitions;


    public ParallelELSaturator(IndexedELOntology elOntology, int numberOfPartitions) {
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
        init();
    }

    private void init() {
        WorkloadManagerFactory factory = new WorkloadManagerFactory(elOntology, numberOfPartitions);
        workloadManager = factory.generate();
    }

    public Set<ELConceptInclusion> saturate() {
        return workloadManager.startSaturation();
    }


}
