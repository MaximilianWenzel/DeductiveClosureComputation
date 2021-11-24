package exceptions;

public class MessageProtocolViolationException extends RuntimeException {
    public MessageProtocolViolationException() {
    }

    public MessageProtocolViolationException(String message) {
        super(message);
    }
}
