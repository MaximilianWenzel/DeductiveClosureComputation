package reasoning.saturation.distributed;

import data.Closure;
import exceptions.NotImplementedException;
import networking.messages.InitializeWorkerMessage;
import reasoning.reasoner.IncrementalReasoner;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.reasoner.IncrementalReasonerWithStatistics;
import reasoning.rules.DistributedSaturationInferenceProcessor;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.workernode.WorkerState;
import reasoning.saturation.distributed.states.workernode.WorkerStateFinished;
import reasoning.saturation.distributed.states.workernode.WorkerStateInitializing;
import util.NetworkingUtils;

import java.io.Serializable;
import java.util.Collection;

public class SaturationWorker<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements Runnable {

    private final IncrementalReasonerType incrementalReasonerType;
    private C closure;
    private Collection<? extends Rule<C, A>> rules;
    private WorkerNodeCommunicationChannel<C, A, T> communicationChannel;
    private WorkerState<C, A, T> state;
    private IncrementalReasoner<C, A> incrementalReasoner;
    private SaturationConfiguration config;
    private WorkerStatistics stats = new WorkerStatistics();

    public SaturationWorker(int portToListen,
                            IncrementalReasonerType incrementalReasonerType) {
        this.communicationChannel = new WorkerNodeCommunicationChannel<>(portToListen);
        this.state = new WorkerStateInitializing<>(this);
        this.incrementalReasonerType = incrementalReasonerType;
    }

    public static void main(String[] args) {
        // args: <PORT-NUMBER>

        // for port number range
        // args: <PORT-NUMBER-FROM-INCLUSIVE>-<PORT-NUMBER-TO-INCLUSIVE>
        System.out.println("Generating worker...");

        String[] portsStrArr = args[0].split("-");
        int portNumber;
        if (portsStrArr.length == 1) {
            // single port
            portNumber = Integer.parseInt(args[0]);
            System.out.println("Worker port: " + portNumber);
        } else if (portsStrArr.length == 2) {
            // port range
            int fromPortIncl = Integer.parseInt(portsStrArr[0]);
            int toPortIncl = Integer.parseInt(portsStrArr[1]);
            portNumber = NetworkingUtils.getFreePortInPredefinedRange(fromPortIncl, toPortIncl);
        } else {
            throw new IllegalArgumentException("arguments: <PORT-NUMBER-FROM-INCLUSIVE>-<PORT-NUMBER-TO-INCLUSIVE>");
        }
        SaturationWorker<?, ?, ?> saturationWorker = new SaturationWorker<>(
                portNumber,
                IncrementalReasonerType.SINGLE_THREADED
        );
        saturationWorker.run();
    }


    @Override
    public void run() {
        try {
            while (!(state instanceof WorkerStateFinished)) {
                state.mainWorkerLoop();
            }
            terminate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public C getClosure() {
        return closure;
    }

    public void switchState(WorkerState<C, A, T> newState) {
        this.state = newState;
    }

    public WorkerNodeCommunicationChannel<C, A, T> getCommunicationChannel() {
        return communicationChannel;
    }

    public IncrementalReasoner<C, A> getIncrementalReasoner() {
        return incrementalReasoner;
    }

    public void initializeWorker(InitializeWorkerMessage<C, A, T> message) {
        this.communicationChannel.setWorkerID(message.getWorkerID());
        this.communicationChannel.setWorkers(message.getWorkers());
        this.communicationChannel.setWorkloadDistributor(message.getWorkloadDistributor());
        this.communicationChannel.setConfig(message.getConfig());
        this.communicationChannel.setStats(this.stats);
        this.closure = message.getClosure();
        this.config = message.getConfig();

        this.setRules(message.getRules());
    }

    public void setRules(Collection<? extends Rule<C, A>> rules) {
        this.rules = rules;
        initializeRules();
    }

    private void initializeRules() {
        DistributedSaturationInferenceProcessor inferenceProcessor = new DistributedSaturationInferenceProcessor(
                communicationChannel, closure, config, stats);
        this.rules.forEach(r -> {
            r.setInferenceProcessor(inferenceProcessor);
            r.setClosure(closure);
        });

        switch (incrementalReasonerType) {
            case SINGLE_THREADED:
                if (config.collectStatistics()) {
                    this.incrementalReasoner = new IncrementalReasonerWithStatistics<>(rules, closure, config, stats);
                } else {
                    this.incrementalReasoner = new IncrementalReasonerImpl<>(rules, closure);
                }
                break;
            default:
                throw new NotImplementedException();
        }
    }

    public void terminate() {
        communicationChannel.terminate();
    }

    public SaturationConfiguration getConfig() {
        return config;
    }

    public WorkerStatistics getStats() {
        return stats;
    }


    public enum IncrementalReasonerType {
        SINGLE_THREADED,
        PARALLEL;
    }
}
