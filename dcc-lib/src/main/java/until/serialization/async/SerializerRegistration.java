package until.serialization.async;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

class SerializerRegistration<T> {

	private final Serializer<T> ser_;
	private final int id_;

	SerializerRegistration(Serializer<T> ser, int id) {
		this.ser_ = ser;
		this.id_ = id;
	}

	int getId() {
		return id_;
	}

	void write(AsyncSerializer async, ByteBuffer out, T o) {
		ser_.write(async, out, o);
	}

	void read(AsyncSerializer async, ByteBuffer in, Consumer<? super T> oc) {
		ser_.read(async, in, oc);
	}

}
