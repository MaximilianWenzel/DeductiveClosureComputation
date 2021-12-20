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

    private KryoSerializer kryoSerializer = new KryoSerializer();

    @Test
    public void testKryoSerialization() {
        String test = "Hello world!";
        byte[] bytes = kryoSerializer.serialize(test);
        String deserializedObject = (String) kryoSerializer.deserialize(bytes);
        System.out.println(deserializedObject);
        assertEquals(test, deserializedObject);
    }

    @Test
    public void testKryoByteBufferOutputStream() {
        String test = "Hello world!";
        ByteBuffer writeBuffer = ByteBuffer.allocateDirect(8192);
        ByteBuffer messageSizeBuffer = ByteBuffer.allocateDirect(8192);

        int lastOffset = 0;
        for (int i = 0; i < 3; i++) {
            // prepare buffer for reading
            kryoSerializer.serializeToByteBuffer(test, writeBuffer);

            // written bytes
            messageSizeBuffer.putInt(writeBuffer.position() - lastOffset);
            lastOffset = writeBuffer.position();
        }
        writeBuffer.flip();
        messageSizeBuffer.flip();

        ByteBuffer readBuffer = ByteBuffer.allocateDirect(8192);
        while (writeBuffer.hasRemaining()) {
            int messageSize = messageSizeBuffer.getInt();
            readBuffer.putInt(messageSize);
            for (int i = 0; i < messageSize; i++) {
                readBuffer.put(writeBuffer.get());
            }
        }
        readBuffer.flip();

        while (readBuffer.hasRemaining()) {
            // read size of message
            int messageSize = readBuffer.getInt();

            // read object
            String deserializedObject = (String) kryoSerializer.deserializeFromByteBuffer(readBuffer);
            System.out.println(deserializedObject + ", Size: " + messageSize);
            assertEquals(test, deserializedObject);
        }
    }

    @Test
    void testKryoObjectRegistration() {
        ByteBuffer messageBuffer = ByteBuffer.allocateDirect(8192);

        ArrayList a = new ArrayList(Arrays.asList(new Object[1]));
        MessageEnvelope envelope = new MessageEnvelope(0, a);

        byte[] bytes = kryoSerializer.serialize(envelope);
        assertTrue(bytes.length > 0);

        kryoSerializer.serializeToByteBuffer(envelope, messageBuffer);
        messageBuffer.flip();
        assertTrue(messageBuffer.hasRemaining());
    }
}
