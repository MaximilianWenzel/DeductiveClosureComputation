package networking.io;

public interface MessageHandler {

    void process(long socketID, Object message);
}
