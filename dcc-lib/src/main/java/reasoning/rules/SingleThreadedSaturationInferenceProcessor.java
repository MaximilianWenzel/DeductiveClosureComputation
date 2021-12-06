package reasoning.rules;

import data.ToDoQueue;

import java.io.Serializable;

public class SingleThreadedSaturationInferenceProcessor<A extends Serializable> implements InferenceProcessor<A> {

    private ToDoQueue<A> toDoQueue;

    public SingleThreadedSaturationInferenceProcessor(ToDoQueue<A> toDoQueue) {
        this.toDoQueue = toDoQueue;
    }

    @Override
    public void processInference(A axiom) {
        toDoQueue.add(axiom);
    }
}
