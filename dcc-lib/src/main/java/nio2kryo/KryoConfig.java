package nio2kryo;

import com.esotericsoftware.kryo.Kryo;
import org.objenesis.strategy.StdInstantiatorStrategy;

public class KryoConfig {

	private static final Kryo kryo;

	static {
		kryo = new Kryo();
		kryo.register(Edge.class);
		kryo.setReferences(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}

	public static Kryo get() {
		return kryo;
	}

}
