package benchmark.echoclosure;

import java.io.Serializable;
import java.util.Objects;

public class EchoAxiom implements Serializable {
    int x;

    protected EchoAxiom() {

    }

    public EchoAxiom(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EchoAxiom echoAxiom = (EchoAxiom) o;
        return x == echoAxiom.x;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), x);
    }
}
