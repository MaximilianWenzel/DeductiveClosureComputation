package reasoning.saturator.distributed;


import data.Dataset;
import data.ParallelToDo;
import networking.ClientComponent;
import networking.messages.DebugMessage;
import networking.messages.InitPartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class SaturationControlNode<P, T> {

    private int state;

    private final Dataset<P, T> dataset;
    private final ParallelToDo<P> toDo = new ParallelToDo<>();
    private final Set<P> consideredAxioms = new UnifiedSet<>();
    private List<DistributedPartitionModel<P, T>> partitionNodes;
    private List<ClientComponent<P, T>> saturationDataChannels = new ArrayList<>();
    private volatile boolean saturationFinished = false;

    protected SaturationControlNode(Dataset<P, T> dataset) {
        this.dataset = dataset;
    }

    protected void initSaturationPartitionNodeConnection(DistributedPartitionModel<P, T> partition) {
        int portNumber = partition.getServerData().getPortNumber();
        String serverName = partition.getServerData().getServerName();
        ClientComponent<P, T> clientComponent = new ClientComponent<>(serverName, portNumber) {
            @Override
            public void processReceivedMessage(SaturationAxiomsMessage<P> message) {
                toDo.addAll(message.getAxioms());
            }

            @Override
            public void processReceivedMessage(StateInfoMessage message) {
                long sequenceNumber = message.getStateSequenceNumber();
                if (sequenceNumber > partition.getCurrentlyLargestStateSequenceNumber().get()) {
                    partition.getCurrentlyLargestStateSequenceNumber().set(sequenceNumber);
                    partition.setState(message.getState());
                }
            }

            @Override
            public void processReceivedMessage(InitPartitionMessage<P, T> message) {
                // TODO implement
            }

            @Override
            public void processReceivedMessage(DebugMessage message) {
                // TODO implement
            }
        };
        partition.initializeConnectionToPartitionNode(clientComponent);
        saturationDataChannels.add(partition.getClientComponent());
    }

    public void init() {
        this.dataset.getInitialAxioms().forEachRemaining(toDo::add);

        // init connection to partition nodes
        this.partitionNodes = initializePartitions();
        this.partitionNodes.forEach(this::initSaturationPartitionNodeConnection);
    }

    public Set<P> saturate() {
        try {
            while (!saturationFinished) {
                P axiom = null;
                axiom = toDo.poll(1000, TimeUnit.MILLISECONDS);

                if (axiom == null) {
                    boolean nothingToDo = true;
                    for (DistributedPartitionModel<P, T> partitionNode : partitionNodes) {
                        switch (partitionNode.getState()) {
                            case DistributedPartitionModel.RUNNING_SATURATION:
                                nothingToDo = false;
                                break;
                            case DistributedPartitionModel.FINISHED_SATURATION_WAITING_ON_CONTROL_NODE:
                                break;
                            default:
                                // TODO partitions might be in state 'INITIALIZED'
                                throw new IllegalStateException();
                        }
                        if (!nothingToDo) {
                            break;
                        }
                    }
                    saturationFinished = nothingToDo;
                } else {
                    distributeAxiom(axiom);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return collectClosureResultsFromPartitions();
    }

    private void distributeAxiom(P axiom) {
        if (consideredAxioms.add(axiom)) {
            for (DistributedPartitionModel<P, T> partition : partitionNodes) {
                if (isRelevantAxiomToPartition(partition, axiom)) {
                    partition.addAxiomToBuffer(axiom);
                }
            }
        }
    }

    private Set<P> collectClosureResultsFromPartitions() {
        // add remaining axioms to closure
        Set<P> closure = new UnifiedSet<>();
        try {
            while (true) {
                P axiom = toDo.poll(1000, TimeUnit.MILLISECONDS);
                if (axiom == null) {
                    // check if all closure results from partitions have been received
                    boolean allClosureResultsReceived = true;
                    for (DistributedPartitionModel<P, T> partitionNode : partitionNodes) {
                        switch (partitionNode.getState()) {
                            case DistributedPartitionModel.FINISHED:
                                // closure result not received
                                allClosureResultsReceived = false;
                                // TODO probably resend message if partition has not received it
                                break;
                            case DistributedPartitionModel.FINISHED_SATURATION_WAITING_ON_CONTROL_NODE:
                                // closure result received
                                break;
                            default:
                                // TODO partitions might be in state 'INITIALIZED'
                                throw new IllegalStateException();
                        }
                        if (!allClosureResultsReceived) {
                            break;
                        }
                    }

                    if (allClosureResultsReceived) {
                        break;
                    }

                } else {
                    closure.add(axiom);
                }
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return closure;
    }

    protected abstract List<DistributedPartitionModel<P, T>> initializePartitions();

    public abstract boolean isRelevantAxiomToPartition(DistributedPartitionModel<P, T> partition, P axiom);

    public boolean isSaturationFinished() {
        return saturationFinished;
    }

    public ParallelToDo<P> getToDo() {
        return toDo;
    }
}
