package reasoning.rules;

import java.io.Serializable;

public class NoOPInferenceProcessor implements InferenceProcessor {

    private static NoOPInferenceProcessor instance = new NoOPInferenceProcessor();

    public static NoOPInferenceProcessor getInstance() {
        return instance;
    }

    @Override
    public void processInference(Serializable axiom) {
    }
}
