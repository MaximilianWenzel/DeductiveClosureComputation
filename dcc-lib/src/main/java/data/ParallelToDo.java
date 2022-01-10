package data;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;

public class ParallelToDo extends LinkedBlockingQueue<Serializable> implements ToDoQueue<Serializable> {
}
