package benchmark.microbenchmark;

import java.util.ArrayList;
import java.util.List;

public class TestObjectWithFields extends TestObject {

    List<TestObject> randomList;

    public TestObjectWithFields() {

    }

    public TestObjectWithFields(int numContainedObjects) {
        randomList = new ArrayList<>(numContainedObjects);
        for (int i = 0; i < numContainedObjects; i++) {
            randomList.add(new TestObject());
        }
    }
}
