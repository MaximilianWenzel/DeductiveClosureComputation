package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import data.Closure;
import enums.SaturationStatusMessage;
import networking.NIO2NetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.*;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.InitialAxiomsDistributor;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;
import util.QueueFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ControlNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements SaturationCommunicationChannel {

    // TODO: adjust if workers are not running on localhost
    private static final boolean WORKERS_ON_LOCALHOST = false;

    private final Logger log = ConsoleUtils.getLogger();

    protected NetworkingComponent networkingComponent;
    protected List<DistributedWorkerModel<C, A, T>> workers;
    protected Map<Long, DistributedWorkerModel<C, A, T>> workerIDToWorker;
    protected BiMap<Long, Long> socketIDToWorkerID;
    protected BiMap<Long, Long> workerIDToSocketID;
    protected long controlNodeID = 0L;
    protected BlockingQueue<Object> receivedMessages = QueueFactory.createSaturationToDo();
    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected List<? extends A> initialAxioms;

    protected InitialAxiomsDistributor<A> initialAxiomsDistributor;
    protected AcknowledgementEventManager acknowledgementEventManager;

    protected boolean allConnectionsEstablished = false;
    protected AtomicInteger initializedWorkers = new AtomicInteger(0);
    protected AtomicInteger receivedClosureResults = new AtomicInteger(0);

    protected AtomicInteger saturationStage = new AtomicInteger(0);
    protected AtomicInteger sumOfAllReceivedAxioms = new AtomicInteger(0);
    protected AtomicInteger sumOfAllSentAxioms = new AtomicInteger(0);

    protected ExecutorService threadPool;
    protected BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent;
    protected Runnable onNewMessageReceived;

    protected SaturationConfiguration config;


    public ControlNodeCommunicationChannel(List<DistributedWorkerModel<C, A, T>> workers,
                                           WorkloadDistributor<C, A, T> workloadDistributor,
                                           List<? extends A> initialAxioms,
                                           SaturationConfiguration config,
                                           ExecutorService threadPool,
                                           BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent,
                                           Runnable onNewMessageReceived) {
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;
        this.config = config;
        this.threadPool = threadPool;
        this.messagesThatCouldNotBeSent = messagesThatCouldNotBeSent;
        this.onNewMessageReceived = onNewMessageReceived;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = Maps.synchronizedBiMap(HashBiMap.create());
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();

        this.workerIDToWorker = new ConcurrentHashMap<>();
        workers.forEach(p -> workerIDToWorker.put(p.getID(), p));

        initialAxiomsDistributor = new InitialAxiomsDistributor<>(initialAxioms, workloadDistributor);

        acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NIO2NetworkingComponent(
                Collections.emptyList(),
                Collections.emptyList(),
                messageEnvelope -> messagesThatCouldNotBeSent.add(messageEnvelope),
                threadPool
        );

    }

    public void initializeConnectionToWorkerServers() {

        if (WORKERS_ON_LOCALHOST) {
            List<ServerData> serverDataList = workers.stream()
                    .map(DistributedWorkerModel::getServerData)
                    .map(s -> new ServerData("localhost", s.getPortNumber()))
                    .collect(Collectors.toList());

            for (int i = 0; i < workers.size(); i++) {
                WorkerConnectionEstablishmentListener workerConnectionEstablishmentListener = new WorkerConnectionEstablishmentListener(
                        serverDataList.get(i), workers.get(i));
                try {
                    networkingComponent.connectToServer(workerConnectionEstablishmentListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            workers.stream().map(p -> new WorkerConnectionEstablishmentListener(p.getServerData(), p))
                    .forEach(s -> {
                        try {
                            networkingComponent.connectToServer(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    @Override
    public Object removeNextMessage() {
        return receivedMessages.poll();
    }

    @Override
    public Object pollNextMessage() {
        return receivedMessages.poll();
    }

    @Override
    public boolean hasMoreMessages() {
        return !this.receivedMessages.isEmpty();
    }

    @Override
    public void terminateNow() {
        networkingComponent.terminate();
    }

    @Override
    public void terminateAfterAllMessagesHaveBeenSent() {
        networkingComponent.terminateAfterAllMessagesHaveBeenSent();
    }

    public void broadcast(SaturationStatusMessage statusMessage, Runnable onAcknowledgement) {
        for (Long socketID : this.socketIDToWorkerID.keySet()) {
            StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, statusMessage);
            send(socketID, stateInfoMessage, onAcknowledgement);
        }
    }

    public void acknowledgeMessage(long receiverWorkerID, long messageID) {
        AcknowledgementMessage ack = new AcknowledgementMessage(controlNodeID, messageID);
        long receiverSocketID = workerIDToSocketID.get(receiverWorkerID);
        send(receiverSocketID, ack);
    }

    public void send(long receiverSocketID, SaturationStatusMessage status, Runnable onAcknowledgement) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, status);
        send(receiverSocketID, stateInfoMessage, onAcknowledgement);
    }

    public void send(long receiverSocketID, MessageModel<C, A, T> message, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(message.getMessageID(), onAcknowledgement);
        networkingComponent.sendMessage(receiverSocketID, message);
    }

    public void send(long receiverSocketID, Serializable message) {
        networkingComponent.sendMessage(receiverSocketID, message);
    }

    public void requestAxiomCountsFromAllWorkers() {
        this.saturationStage.incrementAndGet();
        for (Long socketID : this.socketIDToWorkerID.keySet()) {
            RequestAxiomMessageCount requestAxiomMessageCount = new RequestAxiomMessageCount(controlNodeID,
                    this.saturationStage.get());
            send(socketID, requestAxiomMessageCount);
        }
    }

    public AcknowledgementEventManager getAcknowledgementEventManager() {
        return this.acknowledgementEventManager;
    }

    public boolean allWorkersInitialized() {
        return this.initializedWorkers.get() == this.workers.size();
    }

    public AtomicInteger getInitializedWorkers() {
        return initializedWorkers;
    }

    public AtomicInteger getReceivedClosureResultsCounter() {
        return this.receivedClosureResults;
    }

    public AtomicInteger getSaturationStage() {
        return saturationStage;
    }

    public AtomicInteger getSumOfAllReceivedAxioms() {
        return sumOfAllReceivedAxioms;
    }

    public AtomicInteger getSumOfAllSentAxioms() {
        return sumOfAllSentAxioms;
    }

    private class WorkerConnectionEstablishmentListener extends ConnectionEstablishmentListener {

        private final DistributedWorkerModel<C, A, T> workerModel;

        public WorkerConnectionEstablishmentListener(ServerData serverData,
                                                     DistributedWorkerModel<C, A, T> workerModel) {
            super(serverData, new MessageHandler() {
                @Override
                public void process(long socketID, Object message) {
                    receivedMessages.add(message);
                    onNewMessageReceived.run();
                }
            });
            this.workerModel = workerModel;
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Connection established to worker server " + workerModel.getID() + ".");

            // get worker ID to socket ID mapping
            socketIDToWorkerID.put(socketManager.getSocketID(), workerModel.getID());
            if (socketIDToWorkerID.size() == ControlNodeCommunicationChannel.this.workers.size()) {
                allConnectionsEstablished = true;
            }

            // send initialization message
            log.info("Sending initialization message to worker " + workerModel.getID() + ".");
            InitializeWorkerMessage<C, A, T> initializeWorkerMessage = new InitializeWorkerMessage<>(
                    ControlNodeCommunicationChannel.this.controlNodeID,
                    workerModel.getID(),
                    ControlNodeCommunicationChannel.this.workers,
                    workloadDistributor,
                    workerModel.getClosure(),
                    workerModel.getRules(),
                    config
            );

            send(socketManager.getSocketID(), initializeWorkerMessage, new Runnable() {
                @Override
                public void run() {
                    initializedWorkers.getAndIncrement();

                    // send initial axioms
                    initialAxiomsDistributor.getInitialAxioms(workerModel.getID())
                            .forEach(axiom -> {
                                ControlNodeCommunicationChannel.this.getSumOfAllSentAxioms().incrementAndGet();
                                send(socketManager.getSocketID(), axiom);
                            });
                }
            });

        }

    }

}
