package serialization;

import benchmark.microbenchmark.TestObject;
import org.junit.jupiter.api.Test;
import util.serialization.JavaSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class JavaSerialization {

    @Test
    void testJavaSerializationWithLength() throws IOException, ClassNotFoundException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);

        JavaSerializer javaSerializer = new JavaSerializer();
        List<TestObject> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new TestObject());
        }

        int numBytesForLength = 4;
        for (TestObject obj : list) {
            buffer.position(buffer.position() + numBytesForLength);
            int start = buffer.position();
            javaSerializer.serializeToByteBuffer(obj, buffer);
            int end = buffer.position();

            int numBytesObject = end - start;
            buffer.position(buffer.position() - numBytesObject - numBytesForLength);
            buffer.putInt(numBytesObject);
            buffer.position(end);
        }

        buffer.flip();

        while (buffer.hasRemaining()) {
            int length = buffer.getInt();
            TestObject obj = (TestObject) javaSerializer.deserializeFromByteBuffer(buffer);
            assert obj != null;
            System.out.println(obj + ", Size: " + length);
        }
    }
}
