package networking.acknowledgement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AcknowledgementEventManager {

    private Map<Long, Runnable> messageIDToCallbackFunction = new ConcurrentHashMap<>();

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
