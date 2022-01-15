package benchmark.rdfsreasoning;

import java.io.Serializable;
import java.util.Set;

public class RDFSPartitionCollection implements Serializable {

    Set<Long> rdfClassIDs;
    Set<Long> propertyIDs;

    public RDFSPartitionCollection(Set<Long> rdfClassIDs, Set<Long> propertyIDs) {
        this.rdfClassIDs = rdfClassIDs;
        this.propertyIDs = propertyIDs;
    }

    public Set<Long> getRDFClassIDs() {
        return rdfClassIDs;
    }

    public Set<Long> getPropertyIDs() {
        return propertyIDs;
    }
}
