package until.serialization.async;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AsyncSerializer {

	/* Registration Management */

	private final Map<Class<?>, SerializerRegistration<?>> registrationsByClass_ = new HashMap<>();
	private final Map<Integer, SerializerRegistration<?>> registrationsById_ = new HashMap<>();

	public boolean isRegistered(Class<?> cls) {
		return registrationsByClass_.get(cls) != null;
	}

	public boolean isRegistered(int id) {
		return registrationsById_.get(id) != null;
	}

	private int nextClassId_ = 0;

	int getUnregisteredId() {
		while (isRegistered(nextClassId_)) {
			nextClassId_++;
		}
		return nextClassId_;
	}

	public <T> void register(Class<? extends T> cls, Serializer<T> serializer,
			int id) {
		if (isRegistered(id)) {
			throw new IllegalArgumentException("Id already registered: " + id);
		}
		SerializerRegistration<T> r = new SerializerRegistration<>(serializer,
				id);
		registrationsById_.put(id, r);
		registrationsByClass_.put(cls, r);
	}

	public <T> void register(Class<? extends T> cls, Serializer<T> serializer) {
		register(cls, serializer, getUnregisteredId());
	}

	public void register(Class<? extends AsyncSerializable> cls, int id) {
		register(cls, new AsyncSerializableSerializer<>(cls), id);
	}

	public void register(Class<? extends AsyncSerializable> cls) {
		register(cls, getUnregisteredId());
	}

	public void unregister(Class<?> cls) {
		SerializerRegistration<?> r = registrationsByClass_.remove(cls);
		if (r != null) {
			registrationsById_.remove(r.getId());
		}
	}

	<T> SerializerRegistration<T> getRegistration(int id) {
		@SuppressWarnings("unchecked")
		SerializerRegistration<T> result = (SerializerRegistration<T>) registrationsById_
				.get(id);
		if (result == null) {
			throw new RuntimeException("Unknow class id: " + id);
		}
		return result;
	}

	<T> SerializerRegistration<T> getRegistration(Class<? extends T> cls) {
		@SuppressWarnings("unchecked")
		SerializerRegistration<T> result = (SerializerRegistration<T>) registrationsByClass_
				.get(cls);
		if (result == null) {
			throw new RuntimeException("Class not registered: " + cls);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	<T> SerializerRegistration<T> getRegistration(T o) {
		return (SerializerRegistration<T>) getRegistration(
				o == null ? Void.class : o.getClass());
	}

	// Register serializers for predefined classes
	{
		register(Void.class, new NullSerializer());
		register(String.class, new StringSerializer());
	}

	/* Management of pending operations */

	/**
	 * Pending operations waiting for the space in buffer
	 */
	private Runnable pending_;

	/**
	 * Returns and clears the current pending operations
	 * 
	 * @return write or read operations that have not been completed yet due to
	 *         the lack of space in the buffer(s) to which they were writing or
	 *         from which they were reading; or {@code null} if there are no
	 *         pending operations
	 */
	public Runnable pendingOperations() {
		Runnable result = pending_;
		pending_ = null;
		return result;
	}

	boolean hasPending() {
		return pending_ != null;
	}

	/**
	 * Schedule execution of given operation at the end of the current pending
	 * operations
	 * 
	 * @param last
	 *            the pending operation that should be executed last
	 */
	public void append(Runnable last) {
		Runnable pending = pending_;
		this.pending_ = pending == null ? last : () -> {
			pending.run();
			if (hasPending()) { // still leftovers
				append(last);
			} else {
				last.run();
			}
		};
	}

	/* Writing */

	public void writeInt(ByteBuffer out, int val) {
		if (hasPending() || out.remaining() < 4) {
			append(() -> writeInt(out, val));
		} else {
			out.putInt(val);
		}
	}

	public <T> void writeObject(ByteBuffer out, T o) {
		if (o == null) {
			throw new IllegalArgumentException("Object should not be null");
		}
		getRegistration(o).write(this, out, o);
	}

	public void writeString(ByteBuffer out, String s) {
		writeObject(out, s);
	}

	public <T> void writeClassAndObject(ByteBuffer out, T o) {
		if (hasPending() || out.remaining() < 4) {
			append(() -> writeClassAndObject(out, o));
		} else {
			SerializerRegistration<T> r = getRegistration(o);
			out.putInt(r.getId());
			r.write(this, out, o);
		}
	}

	/* Reading */

	public void readInt(ByteBuffer in, Consumer<? super Integer> ic) {
		if (hasPending() || in.remaining() < 4) {
			append(() -> readInt(in, ic));
		} else {
			int value = in.getInt();
			ic.accept(value);
		}
	}

	public <O> void readObject(ByteBuffer in, Consumer<? super O> oc,
			Class<? extends O> cls) {
		getRegistration(cls).read(this, in, oc);
	}

	public void readString(ByteBuffer in, Consumer<? super String> sc) {
		readObject(in, sc, String.class);
	}

	public <O> void readClassAndObject(ByteBuffer in, Consumer<? super O> oc) {
		if (hasPending() || in.remaining() < 4) {
			append(() -> readClassAndObject(in, oc));
		} else {
			this.<O> getRegistration(in.getInt()).read(this, in, oc);
		}
	}

}
