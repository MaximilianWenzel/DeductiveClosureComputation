package exceptions;

public class IllegalEventException extends RuntimeException {
    public IllegalEventException() {
    }

    public IllegalEventException(String message) {
        super(message);
    }
}
