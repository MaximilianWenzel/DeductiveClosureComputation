package networking.io;

/**
 * This interface can be used to determine how messages from different sockets are processed.
 */
public interface MessageHandler {

    void process(long socketID, Object message);
}
