package exceptions;

public class InsufficientQueueCapacityException extends RuntimeException {

    public InsufficientQueueCapacityException(int capacity) {
        super("Capacity: " + capacity);
    }
}
