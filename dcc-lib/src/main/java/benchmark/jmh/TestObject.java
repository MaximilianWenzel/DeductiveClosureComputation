package benchmark.jmh;

import java.io.Serializable;

public class TestObject implements Serializable {

    double randomNumber = Math.random();

    @Override
    public String toString() {
        return "Test-" + randomNumber;
    }
}
