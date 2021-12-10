package util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class KryoSerializer implements Serializer {

    private Kryo kryo = new Kryo();

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
        ByteBufferOutputStream bos = new ByteBufferOutputStream(buffer);
        Output output = new Output(bos);
        kryo.writeClassAndObject(output, object);
        output.flush();
    }

    public Object deserializeFromByteBuffer(ByteBuffer buffer, int offset, int numBytes) {
        Input input = new Input(buffer.array(), offset, numBytes);
        buffer.position(offset + numBytes);
        return kryo.readClassAndObject(input);
    }

    @Override
    public Object deserializeFromByteBuffer(ByteBuffer buffer) {
        int previousPosition = buffer.position();
        ByteBufferInputStream bis = new ByteBufferInputStream(buffer);
        Input input = new Input(bis);
        Object o = kryo.readClassAndObject(input);

        // set new buffer position since kryo sets current buffer position to the end
        int readBytes = (int) input.total();
        buffer.position(previousPosition + readBytes);

        return o;
    }

    @Override
    public Object deserialize(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        Input input = new Input(bis);
        return kryo.readClassAndObject(input);
    }
}
