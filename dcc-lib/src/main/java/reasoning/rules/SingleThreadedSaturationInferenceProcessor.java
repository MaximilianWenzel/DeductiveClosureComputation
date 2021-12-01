package reasoning.rules;

import data.ToDoQueue;

import java.io.Serializable;

public class SingleThreadedSaturationInferenceProcessor implements InferenceProcessor {

    private ToDoQueue toDoQueue;

    public SingleThreadedSaturationInferenceProcessor(ToDoQueue toDoQueue) {
        this.toDoQueue = toDoQueue;
    }

    @Override
    public void processInference(Serializable axiom) {
        toDoQueue.add(axiom);
    }
}
