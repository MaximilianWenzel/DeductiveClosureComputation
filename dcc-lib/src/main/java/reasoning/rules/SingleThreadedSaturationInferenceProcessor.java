package reasoning.rules;

import data.ToDoQueue;

public class SingleThreadedSaturationInferenceProcessor implements InferenceProcessor {

    private ToDoQueue toDoQueue;

    public SingleThreadedSaturationInferenceProcessor(ToDoQueue toDoQueue) {
        this.toDoQueue = toDoQueue;
    }

    @Override
    public void processInference(Object axiom) {
        toDoQueue.add(axiom);
    }
}
