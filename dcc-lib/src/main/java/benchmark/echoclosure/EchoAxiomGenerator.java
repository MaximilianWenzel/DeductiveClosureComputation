package benchmark.echoclosure;

import java.util.ArrayList;
import java.util.List;

public class EchoAxiomGenerator {

    private EchoAxiomGenerator() {
    }

    public static List<EchoAxiom> getInitialAxioms(int numberOfEchoMessages) {
        List<EchoAxiom> echoAxioms = new ArrayList<>(numberOfEchoMessages);
        for (int i = 0; i < numberOfEchoMessages; i++) {
            echoAxioms.add(new EchoAxiomA(i));
        }
        return echoAxioms;
    }


}
