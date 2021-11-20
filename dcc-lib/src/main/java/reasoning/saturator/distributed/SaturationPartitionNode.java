package reasoning.saturator.distributed;
/*
import networking.ServerComponent;
import networking.messages.DebugMessage;
import networking.messages.InitPartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturator.SingleThreadedSaturation;

public class SaturationPartitionNode<P, T> {

    private ControlNodeModel<P, T> controlNodeModel;

    private ServerComponent<P, T> partitionServer;
    private SingleThreadedSaturation<P, T> singleThreadedSaturation;
    private Thread saturationThread;

    public SaturationPartitionNode() {

        partitionServer = new ServerComponent<P, T>() {
            @Override
            public void processReceivedMessage(SaturationAxiomsMessage<P> message) {
                singleThreadedSaturation.getToDo().addAll(message.getAxioms());
                if (singleThreadedSaturation.isSaturationFinished()) {
                    saturationThread = getSaturationThread();
                    saturationThread.start();
                }
            }

            @Override
            public void processReceivedMessage(StateInfoMessage message) {
                int stateID = message.getState();
                controlNodeModel.setState(stateID);

                switch (stateID) {
                    case ControlNodeModel.WAITING_ON_PARTITION_NODES:

                }
            }

            @Override
            public void processReceivedMessage(InitPartitionMessage<P, T> message) {
                singleThreadedSaturation = new SingleThreadedSaturation<>(
                        message.getDatasetFragment(),
                        message.getRules()
                );

                saturationThread = getSaturationThread();
                saturationThread.start();
            }

            @Override
            public void processReceivedMessage(DebugMessage message) {

            }
        };
    }


    private Thread getSaturationThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                singleThreadedSaturation.saturate();
            }
        });
    }
}


 */