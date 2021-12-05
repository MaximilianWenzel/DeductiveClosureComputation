package reasoning.rules;

import data.Closure;

import java.io.Serializable;

public abstract class Rule<C extends Closure<A>, A extends Serializable> implements Serializable {

    protected C closure;
    private InferenceProcessor<A> inferenceProcessor = null;

    protected Rule() {

    }

    public abstract void apply(A axiom);

    public void setClosure(C closure) {
        this.closure = closure;
    }

    protected void processInference(A axiom) {
        inferenceProcessor.processInference(axiom);
    }

    public void setInferenceProcessor(InferenceProcessor<A> inferenceProcessor) {
        this.inferenceProcessor = inferenceProcessor;
    }
}
