package reasoning.rules;

import data.Closure;

public abstract class Rule {

    protected Closure closure;
    private InferenceProcessor inferenceProcessor = NoOPInferenceProcessor.getInstance();

    protected Rule() {

    }

    public abstract void apply(Object axiom);

    public void setClosure(Closure closure) {
        this.closure = closure;
    }

    protected void processInference(Object axiom) {
        inferenceProcessor.processInference(axiom);
    }

    public void setInferenceProcessor(InferenceProcessor inferenceProcessor) {
        this.inferenceProcessor = inferenceProcessor;
    }
}
