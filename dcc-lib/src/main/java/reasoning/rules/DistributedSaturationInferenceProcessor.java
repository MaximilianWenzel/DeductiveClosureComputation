package reasoning.rules;

import reasoning.rules.InferenceProcessor;
import reasoning.saturation.distributed.communication.PartitionNodeCommunicationChannel;

import java.io.Serializable;

public class DistributedSaturationInferenceProcessor implements InferenceProcessor {

    private final PartitionNodeCommunicationChannel communicationChannel;

    public DistributedSaturationInferenceProcessor(PartitionNodeCommunicationChannel communicationChannel) {
        this.communicationChannel = communicationChannel;
    }

    @Override
    public void processInference(Serializable axiom) {
        communicationChannel.distributeAxiom(axiom);
    }
}
