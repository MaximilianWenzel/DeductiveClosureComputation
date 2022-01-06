package benchmark;

import enums.SaturationApproach;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.connectors.ServerConnector;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;

import java.util.*;

public class DockerSaturationBenchmark {

    public DockerSaturationBenchmark() {

    }

    public static void main(String[] args) {

        List<ServerData> serverDataList = new ArrayList<>();

        serverDataList.add(new ServerData("172.17.0.2", 63061));
        serverDataList.add(new ServerData("172.17.0.3", 63062));

        List<ServerConnector> connectors = new ArrayList<>();

        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
                System.out.println(message);
            }
        };

        connectors.add(new ServerConnector(serverDataList.get(0), messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Connected. ");
            }
        });

        connectors.add(new ServerConnector(serverDataList.get(0), messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Connected. ");
            }
        });

        NIO2NetworkingComponent networkingComponent = new NIO2NetworkingComponent(
                Collections.emptyList(),
                connectors
        );


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
