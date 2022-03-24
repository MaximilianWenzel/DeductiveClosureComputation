package until.serialization.async;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class NullSerializer implements Serializer<Void> {

	@Override
	public void write(AsyncSerializer async, ByteBuffer out, Void o) {
		// nothing to be written
	}

	@Override
	public void read(AsyncSerializer async, ByteBuffer in,
			Consumer<? super Void> oc) {
		oc.accept(null);
	}

}
