package reasoning.saturator.distributed;

import data.Closure;
import enums.SaturationStatusMessage;
import networking.messages.MessageModel;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.rules.Rule;
import reasoning.saturator.distributed.state.partitionstates.PartitionState;
import reasoning.saturator.distributed.state.partitionstates.PartitionStateFinished;
import reasoning.saturator.distributed.state.partitionstates.PartitionStateInitialized;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class SaturationPartition implements Runnable {

    private static final AtomicLong partitionIDCounter = new AtomicLong(1L);

    private final long id = partitionIDCounter.getAndIncrement();
    private final Collection<? extends Rule> rules;
    private final Closure closure;
    private final WorkloadDistributor distributor;
    private final SaturationCommunicationChannel communicationChannel;

    private PartitionState state;

    public SaturationPartition(SaturationCommunicationChannel communicationChannel,
                               Collection<? extends Rule> rules,
                               Closure closure,
                               WorkloadDistributor distributor) {
        this.communicationChannel = communicationChannel;
        this.closure = closure;
        this.rules = rules;
        this.distributor = distributor;
        this.state = new PartitionStateInitialized(this);
        initializeRules();
    }

    private void initializeRules() {
        ParallelSaturationInferenceProcessor inferenceProcessor = new ParallelSaturationInferenceProcessor(distributor);
        this.rules.forEach(r -> {
            r.setInferenceProcessor(inferenceProcessor);
            r.setClosure(closure);
        });
    }

    @Override
    public void run() {
        try {
            while (!(state instanceof PartitionStateFinished)) {
                if (!communicationChannel.hasMoreMessages()) {
                    communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_TODO_IS_EMPTY);
                }

                MessageModel messageModel = communicationChannel.read();
                messageModel.accept(state);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void processAxiom(Object axiom) {
        if (closure.add(axiom)) {
            for (Rule rule : rules) {
                rule.apply(axiom);
            }
        }
    }

    public Closure getClosure() {
        return closure;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaturationPartition that = (SaturationPartition) o;
        return id == that.id;
    }

    public boolean isToDoEmpty() {
        return this.communicationChannel.hasMoreMessages();
    }

    public long getPartitionID() {
        return this.id;
    }

    public void switchState(PartitionState newState) {
        this.state = newState;
    }

    public SaturationCommunicationChannel getCommunicationChannel() {
        return communicationChannel;
    }
}
