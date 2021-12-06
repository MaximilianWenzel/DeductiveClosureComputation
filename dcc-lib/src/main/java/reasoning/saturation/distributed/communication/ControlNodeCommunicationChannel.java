package reasoning.saturation.distributed.communication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import data.Closure;
import enums.SaturationStatusMessage;
import networking.*;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.messages.*;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.InitialAxiomsDistributor;
import reasoning.saturation.workload.WorkloadDistributor;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ControlNodeCommunicationChannel<C extends Closure<A>, A extends Serializable, T extends Serializable> implements SaturationCommunicationChannel {

    private final Logger log = ConsoleUtils.getLogger();

    protected NetworkingComponent networkingComponent;
    protected List<DistributedWorkerModel<C, A, T>> workers;
    protected Map<Long, DistributedWorkerModel<C, A, T>> workerIDToWorker;
    protected BiMap<Long, Long> socketIDToWorkerID;
    protected BiMap<Long, Long> workerIDToSocketID;
    protected long controlNodeID = 0L;
    protected BlockingDeque<MessageModel<C, A, T>> receivedMessages = new LinkedBlockingDeque<>();
    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected List<? extends A> initialAxioms;

    protected InitialAxiomsDistributor<A> initialAxiomsDistributor;
    protected AcknowledgementEventManager acknowledgementEventManager;

    protected boolean allConnectionsEstablished = false;
    protected AtomicInteger initializedWorkers = new AtomicInteger(0);


    public ControlNodeCommunicationChannel(List<DistributedWorkerModel<C, A, T>> workers,
                                           WorkloadDistributor<C, A, T> workloadDistributor,
                                           List<? extends A> initialAxioms) {
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;
        init();
    }

    private void init() {
        this.socketIDToWorkerID = Maps.synchronizedBiMap(HashBiMap.create());
        this.workerIDToSocketID = this.socketIDToWorkerID.inverse();

        this.workerIDToWorker = new HashMap<>();
        workers.forEach(p -> workerIDToWorker.put(p.getID(), p));

        initialAxiomsDistributor = new InitialAxiomsDistributor<>(initialAxioms, workloadDistributor);

        acknowledgementEventManager = new AcknowledgementEventManager();

        networkingComponent = new NetworkingComponent(
                new MessageProcessorImpl(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        networkingComponent.startNIOThread();
    }

    public void initializeConnectionToWorkerServers() {
        workers.stream().map(p -> new WorkerServerConnector(p.getServerData(), p))
                .forEach(s -> {
                    try {
                        networkingComponent.connectToServer(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public Object read() throws InterruptedException {
        return receivedMessages.take();
    }

    @Override
    public boolean hasMoreMessagesToReadWriteOrToBeAcknowledged() {
        return this.receivedMessages.isEmpty();
    }

    @Override
    public void terminate() {
        networkingComponent.terminate();
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

    public void send(long receiverSocketID, SaturationStatusMessage status) {
        StateInfoMessage stateInfoMessage = new StateInfoMessage(controlNodeID, status);
        send(receiverSocketID, stateInfoMessage);
    }

    public void send(long receiverSocketID, MessageModel<C, A, T> message, Runnable onAcknowledgement) {
        acknowledgementEventManager.messageRequiresAcknowledgment(message.getMessageID(), onAcknowledgement);
        MessageEnvelope messageEnvelope = new MessageEnvelope(receiverSocketID, message);
        networkingComponent.sendMessage(messageEnvelope);
    }
    public void send(long receiverSocketID, MessageModel<C, A, T> message) {
        MessageEnvelope messageEnvelope = new MessageEnvelope(receiverSocketID, message);
        networkingComponent.sendMessage(messageEnvelope);
    }

    public AcknowledgementEventManager getAcknowledgementEventManager() {
        return this.acknowledgementEventManager;
    }

    private class MessageProcessorImpl implements MessageProcessor {

        @Override
        public void process(MessageEnvelope message) {
            if (!allConnectionsEstablished) {
                long workerID = ((MessageModel)message.getMessage()).getSenderID();
                ControlNodeCommunicationChannel.this.socketIDToWorkerID.put(message.getSocketID(), workerID);
                if (socketIDToWorkerID.size() == ControlNodeCommunicationChannel.this.workers.size()) {
                    allConnectionsEstablished = true;
                }
            }
            receivedMessages.add((MessageModel) message.getMessage());
        }
    }
    private class WorkerServerConnector extends ServerConnector {

        private final DistributedWorkerModel<C, A, T> workerModel;

        public WorkerServerConnector(ServerData serverData, DistributedWorkerModel<C, A, T> workerModel) {
            super(serverData);
            this.workerModel = workerModel;
        }

        @Override
        public void onConnectionEstablished(SocketManager socketManager) {
            log.info("Connection established to worker server " + workerModel.getID() + ".");

            // get worker ID to socket ID mapping
            socketIDToWorkerID.put(socketManager.getSocketID(), workerModel.getID());

            // send message
            log.info("Sending initialization message to worker " + workerModel.getID() + ".");
            InitializeWorkerMessage<C, A, T> initializeWorkerMessage = new InitializeWorkerMessage<>(
                    ControlNodeCommunicationChannel.this.controlNodeID,
                    workerModel.getID(),
                    ControlNodeCommunicationChannel.this.workers,
                    workloadDistributor,
                    workerModel.getRules(),
                    initialAxiomsDistributor.getInitialAxioms(workerModel.getID())
            );

            send(socketManager.getSocketID(), initializeWorkerMessage, new Runnable() {
                @Override
                public void run() {
                    initializedWorkers.getAndIncrement();
                }
            });
        }

    }
    public boolean allWorkersInitialized() {
        return this.initializedWorkers.get() == this.workers.size();
    }


}
