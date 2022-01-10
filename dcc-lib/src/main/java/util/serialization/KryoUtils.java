package util.serialization;

import benchmark.jmh.TestObject;
import benchmark.transitiveclosure.DerivedReachability;
import benchmark.transitiveclosure.Reachability;
import benchmark.transitiveclosure.ReachabilityClosure;
import benchmark.transitiveclosure.ToldReachability;
import data.Closure;
import networking.messages.AxiomCount;
import networking.messages.MessageEnvelope;
import networking.messages.MessageModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KryoUtils {

    public static final List<Class<?>> CLASSES_FOR_REGISTRATION;
    static {
        List<Class<?>> classList = new ArrayList<>();
        classList.add(Reachability.class);
        classList.add(ToldReachability.class);
        classList.add(DerivedReachability.class);
        classList.add(MessageModel.class);
        classList.add(MessageEnvelope.class);
        classList.add(ReachabilityClosure.class);
        classList.add(TestObject.class);
        classList.add(MessageModel.class);
        classList.add(ArrayList.class);
        classList.add(AxiomCount.class);
        classList.add(Closure.class);
        classList.add(Serializable.class);
        CLASSES_FOR_REGISTRATION = Collections.unmodifiableList(classList);
    }
}
