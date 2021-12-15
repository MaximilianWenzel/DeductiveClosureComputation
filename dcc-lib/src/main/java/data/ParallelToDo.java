package data;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ParallelToDo<A extends Serializable> extends LinkedBlockingQueue<A> implements ToDoQueue<A> {
}
