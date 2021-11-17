package elsyntax;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;
import networking.ClientComponent;
import networking.ServerComponent;
import networking.messages.DebugMessage;
import networking.messages.InitPartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import org.junit.jupiter.api.Test;

public class NetworkingTest {

    @Test
    public void testClientServerCommunication() {
        int serverPort = 6066;
        ServerComponent<ELConceptInclusion, ELConcept> serverComponent = new ServerComponent<>(serverPort) {
            @Override
            public void processReceivedMessage(SaturationAxiomsMessage<ELConceptInclusion> message) {
            }

            @Override
            public void processReceivedMessage(StateInfoMessage message) {
            }

            @Override
            public void processReceivedMessage(InitPartitionMessage<ELConceptInclusion, ELConcept> message) {
            }

            @Override
            public void processReceivedMessage(DebugMessage message) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Server: " + message.getMessage());
            }
        };
        serverComponent.startListeningOnPort();

        ClientComponent<ELConceptInclusion, ELConcept> clientComponent = new ClientComponent<>("localhost", serverPort) {
            @Override
            public void processReceivedMessage(SaturationAxiomsMessage<ELConceptInclusion> message) {
            }

            @Override
            public void processReceivedMessage(StateInfoMessage message) {
            }

            @Override
            public void processReceivedMessage(InitPartitionMessage<ELConceptInclusion, ELConcept> message) {
            }

            @Override
            public void processReceivedMessage(DebugMessage message) {
            }
        };

        clientComponent.connectToServer();
        clientComponent.sendMessageAsync(new DebugMessage("Hello World!"));

        try {
            Thread.sleep(50);
            System.out.println("Waiting...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
