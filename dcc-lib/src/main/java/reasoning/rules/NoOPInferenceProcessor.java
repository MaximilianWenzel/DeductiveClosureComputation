package reasoning.rules;

public class NoOPInferenceProcessor implements InferenceProcessor {

    private static NoOPInferenceProcessor instance = new NoOPInferenceProcessor();

    public static NoOPInferenceProcessor getInstance() {
        return instance;
    }

    @Override
    public void processInference(Object axiom) {
    }
}
