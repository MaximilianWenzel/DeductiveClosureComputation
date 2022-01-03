package util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class KryoSerializer implements Serializer {

    private Kryo kryo = new Kryo();
    private ByteBufferInput bbi = new ByteBufferInput();
    private ByteBufferOutput bbo = new ByteBufferOutput();

    public KryoSerializer() {

    }

    {
        kryo = new Kryo();
        //SerializationUtils.kryo.register(TestObject.class);
        //SerializationUtils.kryo.register(MessageEnvelope.class);
        //SerializationUtils.kryo.register(SaturationAxiomsMessage.class);
        //SerializationUtils.kryo.register(ArrayList.class);
        //SerializationUtils.kryo.register(networking.messages.SaturationAxiomsMessage.class);
        //SerializationUtils.kryo.register(Closure.class);
        //SerializationUtils.kryo.register(Serializable.class);
        kryo.setRegistrationRequired(false);
    }


    @Override
    public byte[] serialize(Serializable object) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeClassAndObject(output, object);
        output.flush();
        return bos.toByteArray();
    }

    @Override
    public void serializeToByteBuffer(Serializable object, ByteBuffer buffer) {
        bbo.setBuffer(buffer);
        kryo.writeClassAndObject(bbo, object);
    }

    public Object deserializeFromByteBuffer(ByteBuffer buffer, int offset, int numBytes) {
        Input input = new Input(buffer.array(), offset, numBytes);
        buffer.position(offset + numBytes);
        return kryo.readClassAndObject(input);
    }

    @Override
    public Object deserializeFromByteBuffer(ByteBuffer buffer) {
        bbi.setBuffer(buffer);
        Object o = kryo.readClassAndObject(bbi);
        return o;
    }

    @Override
    public Object deserialize(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        Input input = new Input(bis);
        return kryo.readClassAndObject(input);
    }
}
