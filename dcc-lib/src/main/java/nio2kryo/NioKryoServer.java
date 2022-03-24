package nio2kryo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioKryoServer {

	public static final int DEFAULT_PORT = 8439;

	public final int port;

	NioKryoServer(int port) {
		this.port = port;
	}

	NioKryoServer() {
		this(DEFAULT_PORT);
	}

	public void start() throws IOException {
		Selector selector = Selector.open();
		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		serverSocket.bind(new InetSocketAddress(port));
		serverSocket.configureBlocking(false);
		serverSocket.register(selector, SelectionKey.OP_ACCEPT);

		while (true) {
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> iter = selectedKeys.iterator();

			while (iter.hasNext()) {

				SelectionKey key = iter.next();
				iter.remove();

				int interests = key.interestOps();
				int oldInterests = interests;

				if (!key.isValid()) {
					iter.remove();
					continue;
				}

				if (key.isAcceptable()) {
					SocketChannel client = serverSocket.accept();
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ,
							new NioObjectChannel(client));
				}

				if (key.isReadable()) {
					NioObjectChannel clientChannel = (NioObjectChannel) key
							.attachment();
					int available = clientChannel.readFromChannel();
					if (available < 0) {
						key.cancel();
						break;
					}
					if (flush(clientChannel) > 0) {
						interests |= SelectionKey.OP_WRITE;
					} else if (available == 0) {
						interests &= ~SelectionKey.OP_READ;
					}
				}

				if (key.isWritable()) {
					NioObjectChannel clientChannel = (NioObjectChannel) key
							.attachment();
					if (flush(clientChannel) > 0) {
						interests |= SelectionKey.OP_READ;
					}
					int remains = clientChannel.writeToChannel();
					if (remains == 0 && !clientChannel.canRead()) {
						interests &= ~SelectionKey.OP_WRITE;
					}
				}

				if (interests != oldInterests) {
					key.interestOps(interests);
				}

			}

		}

	}

	static int flush(NioObjectChannel channel) {
		int flushed = 0;
		while (channel.canRead() && channel.canWrite()) {
			channel.write(channel.read());
			flushed++;
		}
		return flushed;
	}

	public static void main(String[] args) throws IOException {

		int port = NioKryoServer.DEFAULT_PORT;

		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("port = " + port);

		new NioKryoServer(port).start();
	}

}