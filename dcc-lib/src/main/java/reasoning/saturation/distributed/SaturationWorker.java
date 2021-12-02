package reasoning.saturation.distributed;

import data.Closure;
import exceptions.NotImplementedException;
import networking.messages.InitializeWorkerMessage;
import reasoning.reasoner.IncrementalReasoner;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.DistributedSaturationInferenceProcessor;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;
import reasoning.saturation.distributed.states.workernode.WorkerState;
import reasoning.saturation.distributed.states.workernode.WorkerStateFinished;
import reasoning.saturation.distributed.states.workernode.WorkerStateInitializing;

import java.util.Collection;

public class SaturationWorker implements Runnable {

    private final IncrementalReasonerType incrementalReasonerType;
    private final Closure closure;
    private Collection<? extends Rule> rules;
    private WorkerNodeCommunicationChannel communicationChannel;
    private WorkerState state;
    private IncrementalReasoner incrementalReasoner;

    public SaturationWorker(int portToListen,
                            int maxNumberOfAxiomsToBufferBeforeSending,
                            Closure closure,
                            IncrementalReasonerType incrementalReasonerType) {
        this.communicationChannel = new WorkerNodeCommunicationChannel(portToListen, maxNumberOfAxiomsToBufferBeforeSending);
        this.state = new WorkerStateInitializing(this);
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
            while (!(state instanceof WorkerStateFinished)) {
                state.mainPartitionLoop();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Closure getClosure() {
        return closure;
    }

    public void switchState(WorkerState newState) {
        this.state = newState;
    }

    public WorkerNodeCommunicationChannel getCommunicationChannel() {
        return communicationChannel;
    }

    public IncrementalReasoner getIncrementalReasoner() {
        return incrementalReasoner;
    }

    public void initializePartition(InitializeWorkerMessage message) {
        this.communicationChannel.setWorkers(message.getPartitions());
        this.communicationChannel.setWorkerID(message.getWorkerID());
        this.communicationChannel.setWorkloadDistributor(message.getWorkloadDistributor());
        this.communicationChannel.setInitialAxioms(message.getInitialAxioms());
        this.setRules(message.getRules());
    }

    public enum IncrementalReasonerType {
        SINGLE_THREADED,
        PARALLEL
    }

}
