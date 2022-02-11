package until.serialization.async;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class AsyncSerializableSerializer<T extends AsyncSerializable>
		implements Serializer<T> {

	private final Class<? extends T> cls_;

	public AsyncSerializableSerializer(Class<? extends T> cls) {
		this.cls_ = cls;
	}

	@Override
	public void write(AsyncSerializer async, ByteBuffer out, T o) {
		o.write(async, out);		
	}

	@Override
	public void read(AsyncSerializer async, ByteBuffer in,
			Consumer<? super T> oc) {
		T o = createObject();
		o.read(async, in);
		if (async.hasPending()) {
			async.append(() -> oc.accept(o));
		} else {
			oc.accept(o);
		}
	}

	T createObject() {
		try {
			Constructor<? extends T> cn = cls_.getConstructor();
			return cn.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}