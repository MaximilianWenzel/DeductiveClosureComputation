package until.serialization.async;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import com.github.jsonldjava.shaded.com.google.common.base.Preconditions;

public class StringSerializer implements Serializer<String> {

	@Override
	public void write(AsyncSerializer async, ByteBuffer out, String o) {
		Preconditions.checkNotNull(o);
		// TODO: optimize for short strings
		if (async.hasPending() || out.remaining() < 4) {
			async.append(() -> write(async, out, o));
		} else {
			out.putInt(o.length());
			writeStringChars(async, out, o, 0);
		}
	}

	private void writeStringChars(AsyncSerializer async, ByteBuffer out,
			String s, int pos) {
		for (; pos < s.length(); pos++) {
			if (async.hasPending() || out.remaining() < 2) {
				int p = pos;
				async.append(() -> writeStringChars(async, out, s, p));
				return;
			} else {
				out.putChar(s.charAt(pos));
			}
		}
	}

	@Override
	public void read(AsyncSerializer async, ByteBuffer in,
			Consumer<? super String> sc) {
		if (async.hasPending() || in.remaining() < 4) {
			async.append(() -> read(async, in, sc));
		} else {
			int length = in.getInt();
			readStringChars(async, in, sc, new char[length], 0);
		}
	}

	private void readStringChars(AsyncSerializer async, ByteBuffer in,
			Consumer<? super String> sc, char[] data, int pos) {
		for (; pos < data.length; pos++) {
			if (async.hasPending() || in.remaining() < 2) {
				int p = pos;
				async.append(() -> readStringChars(async, in, sc, data, p));
				return;
			} else {
				data[pos] = in.getChar();
			}
		}
		sc.accept(new String(data));
	}

}
