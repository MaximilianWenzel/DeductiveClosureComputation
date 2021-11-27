package reasoning.rules;

import reasoning.rules.InferenceProcessor;
import reasoning.saturation.distributed.communication.PartitionNodeCommunicationChannel;

public class DistributedSaturationInferenceProcessor implements InferenceProcessor {

    private final PartitionNodeCommunicationChannel communicationChannel;

    public DistributedSaturationInferenceProcessor(PartitionNodeCommunicationChannel communicationChannel) {
        this.communicationChannel = communicationChannel;
    }

    @Override
    public void processInference(Object axiom) {
        communicationChannel.distributeAxiom(axiom);
    }
}
