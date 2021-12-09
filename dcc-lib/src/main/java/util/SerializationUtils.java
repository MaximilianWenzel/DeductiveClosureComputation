package util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.*;
import java.nio.ByteBuffer;

public class SerializationUtils {

    public static final Kryo kryo = new Kryo();

    public static byte[] kryoSerialization(Object object) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeClassAndObject(output, object);
        output.flush();
        return bos.toByteArray();
    }

    public static void kryoSerializeToByteBuffer(Object object, ByteBuffer buffer) {
        ByteBufferOutputStream bos = new ByteBufferOutputStream(buffer);
        Output output = new Output(bos);
        kryo.writeClassAndObject(output, object);
        output.flush();
    }

    public static Object kryoDeserialize(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        Input input = new Input(bis);
        return kryo.readClassAndObject(input);
    }

    public static Object kryoDeserializeFromByteBuffer(ByteBuffer buffer, int offset, int numBytesToBeConsidered) {
        Input input = new Input(buffer.array(), offset, numBytesToBeConsidered);
        buffer.position(offset + numBytesToBeConsidered);
        return kryo.readClassAndObject(input);
    }

    public static byte[] javaSerialization(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(object);
        return bos.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(bis);
        return in.readObject();
    }
}
