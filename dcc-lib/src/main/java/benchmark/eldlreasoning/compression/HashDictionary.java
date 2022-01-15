package benchmark.eldlreasoning.compression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HashDictionary {

    private AtomicInteger idCounter = new AtomicInteger(1);
    private Map<String, Integer> idToStringMap = new HashMap<>();
    private List<String> implicitIDToStringMap = new ArrayList<>();

    public HashDictionary() {

    }

    public int add(String str) {
        int id = idCounter.getAndIncrement();
        idToStringMap.put(str, id);
        return id;
    }

    public String idToString(int id) {
        return implicitIDToStringMap.get(id);
    }

    public int stringToID(String str) {
        return this.idToStringMap.get(str);
    }

}
