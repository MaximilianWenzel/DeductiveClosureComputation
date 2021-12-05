package data;

import java.io.Serializable;
import java.util.ArrayDeque;

public class SingleThreadToDo<A extends Serializable> extends ArrayDeque<A> implements ToDoQueue<A> {
}
