package networking.acknowledgement;

import java.util.HashMap;
import java.util.Map;

public class AcknowledgementEventManager {

    private Map<Long, Runnable> messageIDToCallbackFunction = new HashMap<>();

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
