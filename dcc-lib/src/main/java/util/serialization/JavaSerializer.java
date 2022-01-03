package util.serialization;

import com.esotericsoftware.kryo.io.*;

import java.io.*;
import java.nio.ByteBuffer;

public class JavaSerializer implements Serializer {

    private ByteBufferInput bbi = new ByteBufferInput();
    private ByteBufferOutput bbo = new ByteBufferOutput();

    @Override
    public byte[] serialize(Serializable object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(object);
        return bos.toByteArray();
    }

    @Override
    public void serializeToByteBuffer(Serializable object, ByteBuffer buffer) throws IOException {
        ByteBufferOutput bbo = new ByteBufferOutput(buffer);
        ObjectOutputStream out = new ObjectOutputStream(bbo);
        out.writeObject(object);
        out.flush();
    }

    @Override
    public Object deserializeFromByteBuffer(ByteBuffer buffer) throws IOException, ClassNotFoundException {
        ByteBufferInput bbi = new ByteBufferInput(buffer);
        ObjectInputStream in = new ObjectInputStream(bbi);
        return in.readObject();
    }

    @Override
    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(bis);
        return in.readObject();
    }
}
