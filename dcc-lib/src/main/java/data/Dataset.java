package data;

import java.util.Iterator;

public interface Dataset<P, T> {

    Iterator<T> getAllOccurringTerms();

    Iterator<P> getInitialAxioms();
}
