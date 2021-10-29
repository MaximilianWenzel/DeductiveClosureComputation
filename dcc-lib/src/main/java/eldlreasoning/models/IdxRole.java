package eldlreasoning.models;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class IdxRole {
    protected final AtomicInteger occurs = new AtomicInteger(0);

    public AtomicInteger getOccurs() {
        return occurs;
    }
}
