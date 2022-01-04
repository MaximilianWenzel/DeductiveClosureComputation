package data;

import java.util.concurrent.ArrayBlockingQueue;

public class DefaultToDo<A> extends ArrayBlockingQueue<A> {

    public static final int DEFAULT_CAPACITY = 1_000_000;


    public DefaultToDo() {
        super(DEFAULT_CAPACITY);
    }

}
