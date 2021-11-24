package data;

import java.util.Iterator;

public interface Dataset {

    Iterator<Object> getAllOccurringTerms();

    Iterator<Object> getInitialAxioms();
}
