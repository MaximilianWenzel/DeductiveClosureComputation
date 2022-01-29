package util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class KryoSerializer implements Serializer {

    private Kryo kryo;
    private ByteBufferInput bbi = new ByteBufferInput();
    private ByteBufferOutput bbo = new ByteBufferOutput();
    private Input in = new Input();

    {
        kryo = new Kryo();
        KryoUtils.getClasses().forEach(kryo::register);
    }

    public KryoSerializer() {

    }

    @Override
    public byte[] serialize(Object object) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeClassAndObject(output, object);
        output.flush();
        return bos.toByteArray();
    }

    @Override
    public void serializeToByteBuffer(Object object, ByteBuffer buffer) {
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

    public Kryo getKryo() {
        return kryo;
    }

    public ByteBufferInput getByteBufferInput() {
        return bbi;
    }

    public ByteBufferOutput getByteBufferOutput() {
        return bbo;
    }
}
