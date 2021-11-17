package networking;

import networking.messages.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class NetworkingComponent<P, T> {

    protected Socket socket;
    protected InputStream in;
    protected OutputStream out;
    protected ObjectInputStream objIn;
    protected ObjectOutputStream objOut;
    protected BlockingQueue<MessageModel> messagesToSend;

    protected Thread messageTransmitter;
    protected Thread messageProcessor;

    protected void init(Socket socket) {
        try {
            this.socket = socket;
            this.out = this.socket.getOutputStream();
            this.objOut = new ObjectOutputStream(out);
            this.objOut.flush();

            this.in = this.socket.getInputStream();
            this.objIn = new ObjectInputStream(in);

            // TODO adjust capacity of message buffer
            this.messagesToSend = new ArrayBlockingQueue<>(1000);

            this.messageTransmitter = new Thread(new MessageTransmitter());
            this.messageTransmitter.start();

            this.messageProcessor = new Thread(new MessageProcessor());
            this.messageProcessor.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageAsync(MessageModel message) {
        this.messagesToSend.add(message);
    }

    public abstract void processReceivedMessage(SaturationAxiomsMessage<P> message);

    public abstract void processReceivedMessage(StateInfoMessage message);

    public abstract void processReceivedMessage(InitPartitionMessage<P, T> message);

    public abstract void processReceivedMessage(DebugMessage message);

    class MessageTransmitter implements Runnable {
        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    MessageModel message = messagesToSend.take();
                    objOut.writeInt(message.getMessageID());
                    objOut.writeObject(message);
                    objOut.flush();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class MessageProcessor implements Runnable {
        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    int messageType = objIn.readInt();
                    switch (messageType) {
                        case MessageType.SATURATION_DATA:
                            SaturationAxiomsMessage<P> saturationAxiomsMessage = (SaturationAxiomsMessage<P>) objIn.readObject();
                            processReceivedMessage(saturationAxiomsMessage);
                            break;
                        case MessageType.CONTROL_DATA:
                            StateInfoMessage stateInfoMessage = (StateInfoMessage) objIn.readObject();
                            processReceivedMessage(stateInfoMessage);
                            break;
                        case MessageType.INITIALIZE_PARTITION:
                            InitPartitionMessage<P, T> initPartitionMessage = (InitPartitionMessage<P, T>) objIn.readObject();
                            processReceivedMessage(initPartitionMessage);
                            break;
                        case MessageType.DEBUG_MESSAGE:
                            DebugMessage debugMessage = (DebugMessage) objIn.readObject();
                            processReceivedMessage(debugMessage);
                            break;
                        default:
                            throw new IllegalStateException("Unknown message type: " + messageType);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
