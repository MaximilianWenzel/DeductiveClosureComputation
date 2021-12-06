package reasoning.rules;

import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;

public class ParallelSaturationInferenceProcessor implements InferenceProcessor {

    private final WorkloadDistributor distributor;

    public ParallelSaturationInferenceProcessor(WorkloadDistributor distributor) {
        this.distributor = distributor;
    }

    @Override
    public void processInference(Serializable axiom) {
        distributor.getRelevantWorkerIDsForAxiom(axiom);
    }
}
