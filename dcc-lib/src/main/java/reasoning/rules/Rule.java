package reasoning.rules;

import data.Closure;

import java.io.Serializable;

public abstract class Rule implements Serializable {

    protected Closure closure;
    private InferenceProcessor inferenceProcessor = null;

    protected Rule() {

    }

    public abstract void apply(Object axiom);

    public void setClosure(Closure closure) {
        this.closure = closure;
    }

    protected void processInference(Serializable axiom) {
        inferenceProcessor.processInference(axiom);
    }

    public void setInferenceProcessor(InferenceProcessor inferenceProcessor) {
        this.inferenceProcessor = inferenceProcessor;
    }
}
