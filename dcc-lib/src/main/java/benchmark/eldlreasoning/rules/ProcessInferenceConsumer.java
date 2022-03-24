package benchmark.eldlreasoning.rules;

import reasoning.rules.ConclusionProcessor;

import java.io.Serializable;
import java.util.function.Consumer;

public class ProcessInferenceConsumer<A extends Serializable> implements Consumer<A> {
    private ConclusionProcessor<A> conclusionProcessor;

    ProcessInferenceConsumer() {

    }

    public ProcessInferenceConsumer(ConclusionProcessor<A> conclusionProcessor) {
        this.conclusionProcessor = conclusionProcessor;
    }

    @Override
    public void accept(A axiom) {
        conclusionProcessor.processConclusion(axiom);
    }
}
