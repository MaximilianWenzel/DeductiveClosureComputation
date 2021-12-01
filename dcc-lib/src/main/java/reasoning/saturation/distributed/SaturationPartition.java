package reasoning.saturation.distributed;

import data.Closure;
import exceptions.NotImplementedException;
import networking.messages.InitializePartitionMessage;
import reasoning.reasoner.IncrementalReasoner;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.DistributedSaturationInferenceProcessor;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.communication.PartitionNodeCommunicationChannel;
import reasoning.saturation.distributed.states.partitionnode.PartitionState;
import reasoning.saturation.distributed.states.partitionnode.PartitionStateFinished;
import reasoning.saturation.distributed.states.partitionnode.PartitionStateInitializing;

import java.util.Collection;

public class SaturationPartition implements Runnable {

    private final IncrementalReasonerType incrementalReasonerType;
    private final Closure closure;
    private Collection<? extends Rule> rules;
    private PartitionNodeCommunicationChannel communicationChannel;
    private PartitionState state;
    private IncrementalReasoner incrementalReasoner;

    public SaturationPartition(int portToListen,
                               int maxNumberOfAxiomsToBufferBeforeSending,
                               Closure closure,
                               IncrementalReasonerType incrementalReasonerType) {
        this.communicationChannel = new PartitionNodeCommunicationChannel(portToListen, maxNumberOfAxiomsToBufferBeforeSending);
        this.state = new PartitionStateInitializing(this);
        this.closure = closure;
        this.incrementalReasonerType = incrementalReasonerType;
    }


    public void setRules(Collection<? extends Rule> rules) {
        this.rules = rules;
        initializeRules();
    }

    private void initializeRules() {
        DistributedSaturationInferenceProcessor inferenceProcessor = new DistributedSaturationInferenceProcessor(communicationChannel);
        this.rules.forEach(r -> {
            r.setInferenceProcessor(inferenceProcessor);
            r.setClosure(closure);
        });

        switch (incrementalReasonerType) {
            case SINGLE_THREADED:
                this.incrementalReasoner = new IncrementalReasonerImpl(rules, closure);
                break;
            default:
                throw new NotImplementedException();
        }
    }

    @Override
    public void run() {
        try {
            while (!(state instanceof PartitionStateFinished)) {
                state.mainPartitionLoop();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Closure getClosure() {
        return closure;
    }

    public void switchState(PartitionState newState) {
        this.state = newState;
    }

    public PartitionNodeCommunicationChannel getCommunicationChannel() {
        return communicationChannel;
    }

    public IncrementalReasoner getIncrementalReasoner() {
        return incrementalReasoner;
    }

    public void initializePartition(InitializePartitionMessage message) {
        this.communicationChannel.setPartitions(message.getPartitions());
        this.communicationChannel.setPartitionID(message.getPartitionID());
        this.communicationChannel.setWorkloadDistributor(message.getWorkloadDistributor());
        this.communicationChannel.setInitialAxioms(message.getInitialAxioms());
        this.setRules(message.getRules());
    }

    public enum IncrementalReasonerType {
        SINGLE_THREADED,
        PARALLEL
    }

}
