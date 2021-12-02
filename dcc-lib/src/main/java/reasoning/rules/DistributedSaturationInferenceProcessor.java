package reasoning.rules;

import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;

import java.io.Serializable;

public class DistributedSaturationInferenceProcessor implements InferenceProcessor {

    private final WorkerNodeCommunicationChannel communicationChannel;

    public DistributedSaturationInferenceProcessor(WorkerNodeCommunicationChannel communicationChannel) {
        this.communicationChannel = communicationChannel;
    }

    @Override
    public void processInference(Serializable axiom) {
        communicationChannel.distributeAxiom(axiom);
    }
}
