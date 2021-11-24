package reasoning.saturator.distributed;

import data.Closure;
import data.DefaultClosure;
import networking.messages.MessageModel;
import reasoning.saturator.distributed.state.controlnodestates.CNSFinished;
import reasoning.saturator.distributed.state.controlnodestates.CNSInitializing;
import reasoning.saturator.distributed.state.controlnodestates.ControlNodeState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaturationControlNode {

    private final SaturationCommunicationChannel communicationChannel;
    private final Closure closureResult = new DefaultClosure();
    private List<SaturationPartition> partitions;
    private List<Thread> threadPool;
    private Map<Long, SaturationPartition> partitionIDToPartition;

    private ControlNodeState state;


    protected SaturationControlNode(SaturationCommunicationChannel communicationChannel,
                                    List<SaturationPartition> partitions) {
        this.communicationChannel = communicationChannel;
        this.partitions = partitions;
        init();
    }

    private void init() {
        this.partitionIDToPartition = new HashMap<>();
        this.partitions.forEach(p -> {
            partitionIDToPartition.put(p.getPartitionID(), p);
        });
        this.state = new CNSInitializing(this);
    }


    public Closure saturate() {
        initAndStartThreads();

        try {
            while (!(state instanceof CNSFinished)) {
                MessageModel message = communicationChannel.read();
                message.accept(state);
            }
            for (Thread t : threadPool) {
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return closureResult;
    }

    private void initAndStartThreads() {
        this.threadPool = new ArrayList<>();
        for (SaturationPartition partition : this.partitions) {
            this.threadPool.add(new Thread(partition));
        }
        this.threadPool.forEach(Thread::start);
    }

    /*
    private void processStatusMessage(StateInfoMessage partitionMessage) {
        SaturationStatusMessage message = partitionMessage.getStatusMessage();
        SaturationPartition partition = this.partitionIDToPartition.get(partitionMessage.getSenderID());
        switch (message) {
            case PARTITION_INFO_TODO_IS_EMPTY:
                this.partitionsWithEmptyToDo.add(partition);
                if (partitionsWithEmptyToDo.size() == partitions.size()) {
                    // ensure that all partitions have no work
                    this.partitionsWithEmptyToDo.removeIf(saturationPartition -> !saturationPartition.isToDoEmpty());
                    if (partitionsWithEmptyToDo.size() != partitions.size()) {
                        // still work to do
                        return;
                    }
                    // all partitions have an empty queue
                    partitionsWithEmptyToDo.clear();
                    state = ControlNodeState.INTERRUPTING_PARTITIONS;
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_INTERRUPT);
                }
                break;
            case PARTITION_INFO_SATURATION_INTERRUPTED:
                interruptedPartitions.add(partition);
                if (interruptedPartitions.size() == partitions.size()) {
                    // all partitions are interrupted
                    interruptedPartitions.clear();
                    state = ControlNodeState.CHECK_IF_PARTITIONS_FINISHED;
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_STATUS);
                }
                break;
            case PARTITION_INFO_SATURATION_CONVERGED:
                convergedPartitions.add(partition);
                if (convergedPartitions.size() == partitions.size()) {
                    // all partitions converged - request closure from partitions
                    state = ControlNodeState.WAITING_FOR_CLOSURE_RESULTS;
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_INFO_ALL_PARTITIONS_CONVERGED);
                }
                break;
        }
    }

    private void processClosureMessage(SaturationAxiomsMessage message) {
        SaturationPartition partition = this.partitionIDToPartition.get(message.getSenderID());

        if (!state.equals(ControlNodeState.WAITING_FOR_CLOSURE_RESULTS)) {
            throw new IllegalStateException("Control node must be in state: " + ControlNodeState.WAITING_FOR_CLOSURE_RESULTS);
        }
        this.closureResult.addAll(message.getAxioms());
        this.partitionsWhichSentClosure.add(partition);

        // check if all closure results have been received
        if (this.partitionsWhichSentClosure.size() == this.partitions.size()) {
            this.state = ControlNodeState.FINISHED;
        }
    }

     */

    public SaturationPartition getPartition(long partitionID) {
        return this.partitionIDToPartition.get(partitionID);
    }

    public List<SaturationPartition> getPartitions() {
        return partitions;
    }

    public void switchState(ControlNodeState state) {
        this.state = state;
    }

    public SaturationCommunicationChannel getCommunicationChannel() {
        return communicationChannel;
    }
}
