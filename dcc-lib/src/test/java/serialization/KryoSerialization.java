package serialization;

import networking.messages.MessageEnvelope;
import org.junit.jupiter.api.Test;
import util.serialization.KryoSerializer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KryoSerialization {


    @Test
    public void testKryoSerialization() {
        KryoSerializer kryoSerializer = new KryoSerializer();

        String test = "Hello world!";
        byte[] bytes = kryoSerializer.serialize(test);
        String deserializedObject = (String) kryoSerializer.deserialize(bytes);
        System.out.println(deserializedObject);
        assertEquals(test, deserializedObject);
    }

    @Test
    public void testKryoByteBufferOutputStream() {
        KryoSerializer kryoSerializer = new KryoSerializer();

        String test = "Hello world!";
        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);

        int numObjs = 3;
        for (int i = 0; i < numObjs; i++) {
            // serialize object
            kryoSerializer.serializeToByteBuffer(test, buffer);
        }

        buffer.flip();
        for (int i = 0; i < numObjs; i++) {
            // read object
            String deserializedObject = (String) kryoSerializer.deserializeFromByteBuffer(buffer);
            System.out.println(deserializedObject);
            assertEquals(test, deserializedObject);

        }
    }

    @Test
    void testKryoObjectRegistration() {
        KryoSerializer kryoSerializer = new KryoSerializer();
        ByteBuffer messageBuffer = ByteBuffer.allocateDirect(8192);

        ArrayList<Object> a = new ArrayList<>(Arrays.asList(new Object[1]));
        MessageEnvelope envelope = new MessageEnvelope(0, a);

        byte[] bytes = kryoSerializer.serialize(envelope);
        assertTrue(bytes.length > 0);

        kryoSerializer.serializeToByteBuffer(envelope, messageBuffer);
        messageBuffer.flip();
        assertTrue(messageBuffer.hasRemaining());
    }
}
