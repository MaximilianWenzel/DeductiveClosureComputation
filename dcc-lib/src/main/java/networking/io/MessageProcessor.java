package networking.io;

public interface MessageProcessor {

    void process(long socketID, Object message);
}
