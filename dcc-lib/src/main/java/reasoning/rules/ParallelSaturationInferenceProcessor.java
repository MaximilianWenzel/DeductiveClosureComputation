package reasoning.rules;

import reasoning.saturator.distributed.WorkloadDistributor;

public class ParallelSaturationInferenceProcessor implements InferenceProcessor {

    private final WorkloadDistributor distributor;

    public ParallelSaturationInferenceProcessor(WorkloadDistributor distributor) {
        this.distributor = distributor;
    }

    @Override
    public void processInference(Object axiom) {
        distributor.distributeToPartitions(axiom);
    }
}
