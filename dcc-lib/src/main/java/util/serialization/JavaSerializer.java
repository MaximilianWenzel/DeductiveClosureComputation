package util.serialization;

import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.esotericsoftware.kryo.io.Input;

import java.io.*;
import java.nio.ByteBuffer;

public class JavaSerializer implements Serializer {
    @Override
    public byte[] serialize(Serializable object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(object);
        return bos.toByteArray();
    }

    @Override
    public void serializeToByteBuffer(Serializable object, ByteBuffer buffer) throws IOException {
        ByteBufferOutputStream bufferOutputStream = new ByteBufferOutputStream(buffer);
        ObjectOutputStream out = new ObjectOutputStream(bufferOutputStream);
        out.writeObject(object);
        out.flush();
    }

    @Override
    public Object deserializeFromByteBuffer(ByteBuffer buffer) throws IOException, ClassNotFoundException {
        ByteBufferInputStream bis = new ByteBufferInputStream(buffer);
        ObjectInputStream in = new ObjectInputStream(bis);
        return in.readObject();
    }

    @Override
    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(bis);
        return in.readObject();
    }
}
