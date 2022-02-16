package nio2kryo;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import until.serialization.async.AsyncSerializer;
import until.serialization.async.Serializer;

/**
 * A singleton class to mark the end of stream of {@link Nio2AsyncPublisher}
 * 
 * @author Yevgeny Kazakov
 */
class Nio2EndOfStream implements Serializer<Nio2EndOfStream> {

	private static final int ID_ = 558916323; // some likely unused number

	private static final Nio2EndOfStream INSTANCE_ = new Nio2EndOfStream();

	private Nio2EndOfStream() {
		// singleton
	}

	@Override
	public String toString() {
		return Nio2EndOfStream.class.getName();
	}

	static Nio2EndOfStream get() {
		return INSTANCE_;
	}

	static boolean is(Object o) {
		return o == INSTANCE_;
	}

	static void registerWith(AsyncSerializer serializer) {
		if (serializer.isRegistered(Nio2EndOfStream.class)) {
			return; // already registered
		}
		// else
		serializer.register(Nio2EndOfStream.class, INSTANCE_, ID_);
	}

	@Override
	public void write(AsyncSerializer async, ByteBuffer out,
			Nio2EndOfStream o) {
		// nothing additional to be written
	}

	@Override
	public void read(AsyncSerializer async, ByteBuffer in,
			Consumer<? super Nio2EndOfStream> oc) {
		oc.accept(INSTANCE_);
	}

}
