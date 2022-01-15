package benchmark.rdfsreasoning.dataset;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.TripleID;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;

public class RDFSReasoningTriples {

    private long domainID;
    private long rangeID;
    private long rdfTypeID;
    private long subPropertyOfID;
    private long subClassOfID;
    private Map<Long, Set<Long>> predicateIDToSubjectObjectPair = new HashMap<>();
    private Map<Long, Set<Long>> propertyIDToDomainIDs = new HashMap<>();
    private Map<Long, Set<Long>> propertyIDToRangeIDs = new HashMap<>();
    private Map<Long, Set<Long>> propertyIDToSuperPropertyIDs = new HashMap<>();
    private Map<Long, Set<Long>> rdfClassIDToSuperRDFClassIDs = new HashMap<>();
    private Map<Long, Set<Long>> propertyIDToSubPropertyIDs = new HashMap<>();
    private Map<Long, Set<Long>> rdfClassIDToSubRDFClassIDs = new HashMap<>();
    private Map<Long, RoaringBitmap> rdfClassIDToInstancesIDs = new HashMap<>();

    public RDFSReasoningTriples(long domainID, long rangeID, long rdfTypeID, long subPropertyOfID, long subClassOfID) {
        this.domainID = domainID;
        this.rangeID = rangeID;
        this.rdfTypeID = rdfTypeID;
        this.subPropertyOfID = subPropertyOfID;
        this.subClassOfID = subClassOfID;
    }

    public void add(TripleID tripleID) {
        long predicateID = tripleID.getPredicate();
        if (predicateID == rdfTypeID) {
            long instanceID = tripleID.getSubject();
            RoaringBitmap instances = rdfClassIDToInstancesIDs.computeIfAbsent(instanceID, x -> new RoaringBitmap());
            instances.add((int) instanceID);
        } else if (predicateID == rangeID) {
            Set<Long> rangeIDs = propertyIDToRangeIDs.computeIfAbsent(tripleID.getSubject(), x -> new UnifiedSet<>());
            rangeIDs.add(tripleID.getObject());

        } else if (predicateID == domainID) {
            Set<Long> domainIDs = propertyIDToDomainIDs.computeIfAbsent(tripleID.getSubject(), x -> new UnifiedSet<>());
            domainIDs.add(tripleID.getObject());

        } else if (predicateID == subClassOfID) {
            Set<Long> subClassIDs = rdfClassIDToSubRDFClassIDs.computeIfAbsent(tripleID.getObject(),
                    x -> new UnifiedSet<>());
            subClassIDs.add(tripleID.getSubject());

            Set<Long> superClassIDs = rdfClassIDToSuperRDFClassIDs.computeIfAbsent(tripleID.getSubject(),
                    x -> new UnifiedSet<>());
            superClassIDs.add(tripleID.getObject());

        } else if (predicateID == subPropertyOfID) {
            Set<Long> subPropertyIDs = propertyIDToSubPropertyIDs.computeIfAbsent(tripleID.getObject(),
                    x -> new UnifiedSet<>());
            subPropertyIDs.add(tripleID.getSubject());

            Set<Long> superPropertyIDs = propertyIDToSuperPropertyIDs.computeIfAbsent(tripleID.getSubject(),
                    x -> new UnifiedSet<>());
            superPropertyIDs.add(tripleID.getObject());
        }
        Set<Long> subjectObjectPairs = predicateIDToSubjectObjectPair.computeIfAbsent(predicateID,
                x -> new UnifiedSet<>());
        long sbjID = tripleID.getSubject();
        long objId = tripleID.getObject();
        long sbjObjPair = (sbjID << 32) | objId;
        subjectObjectPairs.add(sbjObjPair);
    }

    public Iterator<TripleID> search(TripleID triplePattern) {
        String patternString = triplePattern.getPatternString();
        if (triplePattern.isEmpty()) {
            return new BitsEncodedSubjectObjectPairsIterator(this.predicateIDToSubjectObjectPair);
        }

        if (triplePattern.getPredicate() == 0 &&
                (triplePattern.getSubject() != 0 || triplePattern.getObject() != 0)) {
            throw new IllegalArgumentException(
                    "Triple pattern query not implemented: " + triplePattern.getPatternString());
        }

        long predicateID = triplePattern.getPredicate();

        if (predicateID == rdfTypeID) {
            if (!patternString.equals("?PO")) {
                throw new IllegalArgumentException();
            }
            return new SubstitutedComponentTripleIterator(
                    triplePattern,
                    TripleComponentRole.OBJECT,
                    this.rdfClassIDToInstancesIDs.get(predicateID).stream().asLongStream().iterator()
            );

        } else if (predicateID == rangeID) {
            if (!patternString.equals("SP?")) {
                throw new IllegalArgumentException();
            }
            return new SubstitutedComponentTripleIterator(
                    triplePattern,
                    TripleComponentRole.OBJECT,
                    this.propertyIDToRangeIDs.get(predicateID).iterator()
            );


        } else if (predicateID == domainID) {
            if (!patternString.equals("SP?")) {
                throw new IllegalArgumentException();
            }
            return new SubstitutedComponentTripleIterator(
                    triplePattern,
                    TripleComponentRole.OBJECT,
                    this.propertyIDToDomainIDs.get(predicateID).iterator()
            );

        } else if (predicateID == subClassOfID) {
            switch (patternString) {
                case "SP?":
                    return new SubstitutedComponentTripleIterator(
                            triplePattern,
                            TripleComponentRole.OBJECT,
                            this.rdfClassIDToSuperRDFClassIDs.get(predicateID).iterator()
                    );
                case "?PO":
                    return new SubstitutedComponentTripleIterator(
                            triplePattern,
                            TripleComponentRole.SUBJECT,
                            this.rdfClassIDToSubRDFClassIDs.get(predicateID).iterator()
                    );
                default:
                    throw new IllegalArgumentException();
            }

        } else if (predicateID == subPropertyOfID) {
            switch (patternString) {
                case "SP?":
                    return new SubstitutedComponentTripleIterator(
                            triplePattern,
                            TripleComponentRole.OBJECT,
                            this.propertyIDToSuperPropertyIDs.get(predicateID).iterator()
                    );
                case "?PO":
                    return new SubstitutedComponentTripleIterator(
                            triplePattern,
                            TripleComponentRole.SUBJECT,
                            this.propertyIDToSubPropertyIDs.get(predicateID).iterator()
                    );
                default:
                    throw new IllegalArgumentException();
            }
        }

        if (!patternString.equals("P??")) {
            throw new IllegalArgumentException();
        }

        return new BitsEncodedSubjectObjectPairsIterator(
                Collections.singletonMap(predicateID, this.predicateIDToSubjectObjectPair.get(predicateID)));
    }
}
