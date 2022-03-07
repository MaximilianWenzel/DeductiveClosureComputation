package benchmark.microbenchmark;

import java.io.Serializable;

public class TestObject implements Serializable {
    double randomNumber = Math.random();

    public TestObject() {

    }

    @Override
    public String toString() {
        return "Test-" + randomNumber;
    }

}
