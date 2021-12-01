package reasoning.rules;

import java.io.Serializable;

public interface InferenceProcessor extends Serializable {
    void processInference(Serializable axiom);
}
