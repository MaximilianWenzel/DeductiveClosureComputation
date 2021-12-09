package serialization;

import org.junit.jupiter.api.Test;
import util.SerializationUtils;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KryoSerialization {

    @Test
    public void testKryoSerialization() {
        String test = "Hello world!";
        byte[] bytes = SerializationUtils.kryoSerialization(test);
        String deserializedObject = (String) SerializationUtils.kryoDeserialize(bytes);
        System.out.println(deserializedObject);
        assertEquals(test, deserializedObject);
    }

    @Test
    public void testKryoByteBufferOutputStream() {
        String test = "Hello world!";
        ByteBuffer writeBuffer = ByteBuffer.allocate(8192);
        ByteBuffer messageSizeBuffer = ByteBuffer.allocate(8192);

        int lastOffset = 0;
        for (int i = 0; i < 3; i++) {
            // prepare buffer for reading
            SerializationUtils.kryoSerializeToByteBuffer(test, writeBuffer);

            // written bytes
            messageSizeBuffer.putInt(writeBuffer.position() - lastOffset);
            lastOffset = writeBuffer.position();
        }
        writeBuffer.flip();
        messageSizeBuffer.flip();

        ByteBuffer readBuffer = ByteBuffer.allocate(8192);
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
            String deserializedObject = (String) SerializationUtils.kryoDeserializeFromByteBuffer(readBuffer, readBuffer.position(), messageSize);
            System.out.println(deserializedObject + ", Size: " + messageSize);
            assertEquals(test, deserializedObject);
        }
    }
}
