package networking.reactor.netty.echo;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;

import java.util.ArrayList;

public class KryoConfig {

	private static final Kryo kryo;

	static {
		kryo = new Kryo();
		kryo.register(Edge.class);
		kryo.register(Object[].class);
		kryo.register(ArrayList.class);
		kryo.setReferences(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}

	public static Kryo get() {
		return kryo;
	}

}
