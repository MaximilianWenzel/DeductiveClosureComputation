package data;

import exceptions.InsufficientQueueCapacityException;
import util.ConsoleUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultToDo<A> extends ArrayBlockingQueue<A> {

    public static final int DEFAULT_CAPACITY = 1_000_000;
    private final Logger log = ConsoleUtils.getLogger();

    // TODO change default offer timeout
    public static final TimeUnit OFFER_TIME_UNIT = TimeUnit.MILLISECONDS;
    //public static final long OFFER_TIMEOUT = 500;
    public static final long OFFER_TIMEOUT = Long.MAX_VALUE;

    public DefaultToDo() {
        super(DEFAULT_CAPACITY);
    }

    @Override
    public boolean offer(A a) {
        try {
            /*
            if (log.getLevel().intValue() <= Level.INFO.intValue()) {
                if (size() == DEFAULT_CAPACITY) {
                    log.log(Level.WARNING, "queue is full");
                }
            }

             */
            if (!this.offer(a, OFFER_TIMEOUT,
                    OFFER_TIME_UNIT)) {
                throw new InsufficientQueueCapacityException(DEFAULT_CAPACITY);
            }
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
