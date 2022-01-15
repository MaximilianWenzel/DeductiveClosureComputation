package benchmark.rdfsreasoning.dataset;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.Iterator;

public class SubstitutedComponentTripleIterator implements Iterator<TripleID> {

    TripleComponentRole componentRole;
    Iterator<Long> componentIDs;
    TripleID current;

    public SubstitutedComponentTripleIterator(TripleID tID,
                                              TripleComponentRole componentRole,
                                              Iterator<Long> componentIDs) {
        this.current = new TripleID(tID);
        this.componentRole = componentRole;
        this.componentIDs = componentIDs;
    }

    @Override
    public boolean hasNext() {
        return componentIDs.hasNext();
    }

    @Override
    public TripleID next() {
        switch (componentRole) {
            case SUBJECT:
                current.setSubject(componentIDs.next());
                break;
            case PREDICATE:
                current.setPredicate(componentIDs.next());
                break;
            case OBJECT:
                current.setObject(componentIDs.next());
                break;
        }
        return current;
    }
}
