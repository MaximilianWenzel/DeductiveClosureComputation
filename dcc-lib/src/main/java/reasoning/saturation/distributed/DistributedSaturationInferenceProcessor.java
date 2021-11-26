package reasoning.saturation.distributed;

import reasoning.rules.InferenceProcessor;
import reasoning.saturation.distributed.communication.PartitionNodeCommunicationChannel;

public class DistributedSaturationInferenceProcessor implements InferenceProcessor {

    private PartitionNodeCommunicationChannel communicationChannel;

    public DistributedSaturationInferenceProcessor(PartitionNodeCommunicationChannel communicationChannel) {
        this.communicationChannel = communicationChannel;
    }

    @Override
    public void processInference(Object axiom) {
        communicationChannel.distributeAxiom(axiom);
    }
}
