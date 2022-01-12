package util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class QueueFactory {

    private static final int DEFAULT_CAPACITY = 1_000_000;
    private static final int CAPACITY_MESSAGE_WRITER = 1_000_000;

    public static <E> BlockingQueue<E> createSaturationToDo() {
        return new ArrayBlockingQueue<>(DEFAULT_CAPACITY);
    }

    public static <E> BlockingQueue<E> createNIOMessageWriterQueue() {
        return new ArrayBlockingQueue<>(CAPACITY_MESSAGE_WRITER);
    }
}
