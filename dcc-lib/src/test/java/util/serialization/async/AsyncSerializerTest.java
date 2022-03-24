package util.serialization.async;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import until.serialization.async.AsyncSerializable;
import until.serialization.async.AsyncSerializer;

public class AsyncSerializerTest {

	static AsyncSerializer s;
	ByteBuffer buf;
	Random rnd;

	@BeforeAll
	static void init() {
		s = new AsyncSerializer();
		s.register(Node.class);
	}

	@BeforeEach
	void initByteBuf() {
		// use some small size to test frequent interruptions
		buf = ByteBuffer.allocateDirect(10);
	}

	@BeforeEach
	void initRandom() {
		rnd = new Random();
	}

	@ParameterizedTest
	@ValueSource(ints = { 10, 100, 10_000 })
	void testNodes(int rounds) {
		roundtrip(rounds, () -> generateNode());
	}

	void roundtrip(int rounds, Supplier<? extends AsyncSerializable> sup) {
		int sentHash = 0, receivedHash = 0;
		Object[] received = new Object[1];
		Runnable pendingRead = null, pendingWrite = null;
		while (rounds > 0 || pendingRead != null || pendingWrite != null) {
			if (pendingRead == null) {
				AsyncSerializable next = sup.get();
				rounds--;
				sentHash += next == null ? 0 : next.hashCode();
				s.writeClassAndObject(buf, next);
			} else {
				pendingRead.run();
			}
			pendingRead = s.pendingOperations();
			buf.flip();
			if (pendingWrite == null) {
				s.readClassAndObject(buf, o -> received[0] = o);
			} else {
				pendingWrite.run();
			}
			pendingWrite = s.pendingOperations();
			if (pendingWrite == null) {
				// System.out.println("Received: " + received[0]);
				receivedHash += received[0] == null ? 0
						: received[0].hashCode();
			}
			buf.compact();
		}
		assertEquals(sentHash, receivedHash, "Hash mismatch!");
	}

	Node generateNode() {
		return generateNode(1024);
	}

	Node generateNode(int maxSize) {
		if (maxSize == 0 || rnd.nextInt(3) == 0) {
			return null;
		}
		// else
		byte[] data = new byte[rnd.nextInt(20)];
		rnd.nextBytes(data);
		return new Node(new String(data), rnd.nextInt(),
				generateNode(maxSize / 2), generateNode((maxSize + 1) / 2));
	}

}
