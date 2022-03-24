package util.serialization;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

public interface Serializer {

    byte[] serialize(Serializable obj) throws IOException;

    void serializeToByteBuffer(Serializable obj, ByteBuffer buffer) throws IOException;

    Object deserializeFromByteBuffer(ByteBuffer buffer) throws IOException, ClassNotFoundException;

    Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException;
}
