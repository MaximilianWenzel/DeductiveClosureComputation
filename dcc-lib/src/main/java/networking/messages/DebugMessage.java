package networking.messages;

public class DebugMessage implements MessageModel {

    private String message;

    public DebugMessage(String message) {
        this.message = message;
    }

    @Override
    public int getMessageID() {
        return MessageType.DEBUG_MESSAGE;
    }

    public String getMessage() {
        return message;
    }
}
