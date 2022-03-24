package until.serialization.async;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface Serializer<T> {

	void write(AsyncSerializer async, ByteBuffer out, T o);

	void read(AsyncSerializer async, ByteBuffer in, Consumer<? super T> oc);

}
