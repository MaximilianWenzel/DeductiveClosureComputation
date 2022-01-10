package reasoning.rules;

import data.Closure;
import enums.StatisticsComponent;
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
        if (config.collectWorkerNodeStatistics()) {
            stats.getNumberOfDerivedInferences().incrementAndGet();
            stats.stopStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
            stats.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }

        if (!closure.contains(axiom)) {
            communicationChannel.distributeAxiom(axiom);
        }

        if (config.collectWorkerNodeStatistics()) {
            stats.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }
    }
}
