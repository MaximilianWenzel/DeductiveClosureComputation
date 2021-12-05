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

import java.io.Serializable;
import java.util.Collection;

public class SaturationWorker<C extends Closure<A>, A extends Serializable> implements Runnable {

    private final IncrementalReasonerType incrementalReasonerType;
    private final C closure;
    private Collection<? extends Rule<C, A>> rules;
    private WorkerNodeCommunicationChannel<C, A> communicationChannel;
    private WorkerState<C, A> state;
    private IncrementalReasoner<C, A> incrementalReasoner;

    public SaturationWorker(int portToListen,
                            int maxNumberOfAxiomsToBufferBeforeSending,
                            C closure,
                            IncrementalReasonerType incrementalReasonerType) {
        this.communicationChannel = new WorkerNodeCommunicationChannel<>(portToListen, maxNumberOfAxiomsToBufferBeforeSending);
        this.state = new WorkerStateInitializing<>(this);
        this.closure = closure;
        this.incrementalReasonerType = incrementalReasonerType;
    }


    public void setRules(Collection<? extends Rule<C, A>> rules) {
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
                this.incrementalReasoner = new IncrementalReasonerImpl<>(rules, closure);
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

    public C getClosure() {
        return closure;
    }

    public void switchState(WorkerState<C, A> newState) {
        this.state = newState;
    }

    public WorkerNodeCommunicationChannel<C, A> getCommunicationChannel() {
        return communicationChannel;
    }

    public IncrementalReasoner<C, A> getIncrementalReasoner() {
        return incrementalReasoner;
    }

    public void initializePartition(InitializeWorkerMessage<C, A> message) {
        this.communicationChannel.setWorkers(message.getWorkers());
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
