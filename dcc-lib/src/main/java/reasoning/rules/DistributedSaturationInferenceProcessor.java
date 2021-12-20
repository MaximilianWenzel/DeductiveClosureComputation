package reasoning.rules;

import data.Closure;
import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;

import java.io.Serializable;

public class DistributedSaturationInferenceProcessor implements InferenceProcessor {

    private final WorkerNodeCommunicationChannel communicationChannel;
    private final Closure closure;
    private final SaturationConfiguration config;
    private final WorkerStatistics stats;

    public DistributedSaturationInferenceProcessor(
            WorkerNodeCommunicationChannel communicationChannel, Closure closure,
            SaturationConfiguration config, WorkerStatistics stats) {
        this.communicationChannel = communicationChannel;
        this.closure = closure;
        this.config = config;
        this.stats = stats;
    }

    @Override
    public void processInference(Serializable axiom) {
        if (config.collectStatistics()) {
            stats.getNumberOfDerivedInferences().incrementAndGet();
        }

        if (!closure.contains(axiom)) {
            communicationChannel.distributeAxiom(axiom);
        }
    }
}
