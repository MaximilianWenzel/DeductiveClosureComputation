package benchmark.rdfsreasoning.dataset;

import org.rdfhdt.hdt.dictionary.impl.section.HashDictionarySection;

import java.util.Set;

public class RDFSReasoningDictionary {

    private HashDictionarySection resourcesDictionarySection;
    private HashDictionarySection literalsDictionarySection;
    private Set<Long> rdfClassIDs;
    private Set<Long> propertyIDs;

    private long literalIDsOffset;
    private long maxID;

    public RDFSReasoningDictionary(HashDictionarySection resourcesDictionarySection,
                                   HashDictionarySection literalsDictionarySection,
                                   Set<Long> rdfClassIDs, Set<Long> propertyIDs) {
        this.resourcesDictionarySection = resourcesDictionarySection;
        this.literalsDictionarySection = literalsDictionarySection;
        this.rdfClassIDs = rdfClassIDs;
        this.propertyIDs = propertyIDs;
        init();
    }

    private void init() {
        this.literalIDsOffset = resourcesDictionarySection.getNumberOfElements();
        this.maxID = literalIDsOffset + literalsDictionarySection.getNumberOfElements();
    }

    public String idToString(long id) {
        if (id <= 0 || id > maxID) {
            throw new IllegalArgumentException();
        }
        if (id <= literalIDsOffset) {
            return resourcesDictionarySection.extract(id).toString();
        } else {
            return literalsDictionarySection.extract(id).toString();
        }
    }

    public long stringToID(String str) {
        if (str.startsWith("\"")) {
            // is literal
            return literalsDictionarySection.locate(str) + literalIDsOffset;
        } else {
            return resourcesDictionarySection.locate(str);
        }
    }

    public boolean isLiteral(long id) {
        return id > literalIDsOffset;
    }

    public Set<Long> getRDFClassIDs() {
        return rdfClassIDs;
    }

    public Set<Long> getPropertyIDs() {
        return propertyIDs;
    }

    public long getNumberOfElements() {
        return maxID;
    }
}
