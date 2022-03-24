package networking.acknowledgement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used in order to submit callback methods for individual messages that are executed if the corresponding message ID has been
 * acknowledged.
 */
public class AcknowledgementEventManager {

    private final Map<Long, Runnable> messageIDToCallbackFunction = new ConcurrentHashMap<>();

    public void messageRequiresAcknowledgment(long messageID, Runnable onMessageAcknowledged) {
        messageIDToCallbackFunction.put(messageID, onMessageAcknowledged);
    }

    public void messageAcknowledged(long messageID) {
        Runnable callbackFunction = messageIDToCallbackFunction.remove(messageID);
        if (callbackFunction != null) {
            callbackFunction.run();
        }
    }

}
