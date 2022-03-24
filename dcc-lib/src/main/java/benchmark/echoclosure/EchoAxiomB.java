package benchmark.echoclosure;

public class EchoAxiomB extends EchoAxiom {

    protected EchoAxiomB() {
    }

    public EchoAxiomB(int x) {
        super(x);
    }

    @Override
    public String toString() {
        return "EchoAxiomB(" + x + ")";
    }
}
