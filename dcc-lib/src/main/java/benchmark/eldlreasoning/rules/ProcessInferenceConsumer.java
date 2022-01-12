package benchmark.eldlreasoning.rules;

import reasoning.rules.InferenceProcessor;

import java.io.Serializable;
import java.util.function.Consumer;

public class ProcessInferenceConsumer<A extends Serializable> implements Consumer<A> {
    private InferenceProcessor<A> inferenceProcessor;

    ProcessInferenceConsumer() {

    }

    public ProcessInferenceConsumer(InferenceProcessor<A> inferenceProcessor) {
        this.inferenceProcessor = inferenceProcessor;
    }

    @Override
    public void accept(A axiom) {
        inferenceProcessor.processInference(axiom);
    }
}
