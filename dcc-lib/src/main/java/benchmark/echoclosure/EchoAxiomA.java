package benchmark.echoclosure;

public class EchoAxiomA extends EchoAxiom {

    protected EchoAxiomA() {
        super();
    }

    public EchoAxiomA(int x) {
        super(x);
    }

    @Override
    public String toString() {
        return "EchoAxiomA(" + x + ")";
    }

}
