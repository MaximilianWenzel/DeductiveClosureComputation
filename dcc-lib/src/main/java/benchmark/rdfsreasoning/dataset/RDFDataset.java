package benchmark.rdfsreasoning.dataset;

public class RDFDataset {

    RDFSReasoningTriples triples;
    RDFSReasoningDictionary dictionary;

    public RDFDataset(RDFSReasoningTriples triples, RDFSReasoningDictionary dictionary) {
        this.triples = triples;
        this.dictionary = dictionary;
    }

    public RDFSReasoningTriples getTriples() {
        return triples;
    }

    public RDFSReasoningDictionary getDictionary() {
        return dictionary;
    }
}
