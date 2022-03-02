package util;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueFactory {

    private static final int DEFAULT_CAPACITY = 1_000_000;
    private static final int CAPACITY_MESSAGE_WRITER = 1_000_000;

    public static <E> BlockingQueue<E> createSaturationToDo() {
        return new LinkedBlockingQueue<>();
    }

    public static <E> BlockingQueue<E> createDistributedSaturationToDo() {
        return new LinkedBlockingQueue<>();
    }

    public static <E> BlockingQueue<E> createNIOMessageWriterQueue() {
        return new ArrayBlockingQueue<>(CAPACITY_MESSAGE_WRITER);
    }

    public static <E> Queue<E> getSingleThreadedToDo() {
        return new LinkedBlockingQueue<>();
    }
}
