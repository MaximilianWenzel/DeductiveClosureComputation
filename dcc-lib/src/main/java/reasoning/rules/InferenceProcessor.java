package reasoning.rules;

import java.io.Serializable;

public interface InferenceProcessor<A extends Serializable> extends Serializable {
    void processInference(A axiom);
}
