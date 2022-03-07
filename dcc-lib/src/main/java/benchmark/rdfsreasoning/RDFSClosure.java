package benchmark.rdfsreasoning;

import data.Closure;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.*;

public abstract class RDFSClosure implements Closure<TripleID> {

    /*
    public abstract boolean xIsProperty(long x);

    public abstract boolean xIsRDFSClass(long x);

    public abstract Set<Long> getDomainOfPredicate(long p);

    public abstract Set<Long> getRangeOfPredicate(long p);

    public abstract boolean getSubPropertiesOfPredicate(long p);

    public abstract boolean getSuperPropertiesOfPredicate(long p);

    public abstract Set<Long> getSubClassesOfX(long x);

    public abstract Set<Long> getSuperClassesOfX(long x);

     */

    public abstract boolean isRDFSClass(long x);

    public abstract boolean isLiteral(long x);


    public abstract int getRDFPropertyID();

    public abstract int getRDFTypeID();

    public abstract int getRDFSResourceID();

    public abstract int getRDFSContainerMembershipPropertyID();

    public abstract int getRDFSDatatypeID();

    public abstract int getRDFSLiteralID();

    public abstract Iterator<TripleID> search(TripleID triplePattern);

    public abstract long getRangeID();

    public abstract long getDomainID();

    public abstract long getSubPropertyOfID();

    public abstract long getRDFSClassID();

    public abstract long getSubClassOfID();

    public abstract long geRDFSMemberID();
}
