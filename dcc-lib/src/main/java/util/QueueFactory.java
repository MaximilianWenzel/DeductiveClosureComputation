package util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class QueueFactory<E> {

    private static final int DEFAULT_CAPACITY = 1_000_000;

    public static <E> BlockingQueue<E> createSaturationToDo() {
        return new ArrayBlockingQueue<>(DEFAULT_CAPACITY);
    }
}
