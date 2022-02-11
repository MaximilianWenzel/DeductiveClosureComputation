package until.serialization.async;

import java.nio.ByteBuffer;

public interface AsyncSerializable {

	void write(AsyncSerializer ser, ByteBuffer in);

	void read(AsyncSerializer ser, ByteBuffer out);

}
