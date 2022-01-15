package benchmark.rdfsreasoning.dataset;

import benchmark.eldlreasoning.compression.CompressionTests;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.impl.section.HashDictionarySection;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.parsers.RDFParserRIOT;
import org.rdfhdt.hdt.triples.TripleID;
import util.ConsoleUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class RDFDatasetGenerator {
    Logger log = ConsoleUtils.getLogger();

    List<TripleID> tempTripleIDList = new ArrayList<>();
    String rdfDumpPath;
    File rdfDumpFile;
    Set<Long> propertyIDs = new UnifiedSet<>();
    Set<Long> rdfClassIDs = new UnifiedSet<>();

    HashDictionarySection resourcesDictionarySection = new HashDictionarySection();
    HashDictionarySection literalsDictionarySection = new HashDictionarySection();

    long rdfTypeID = -1L;
    long subClassOfID = -1L;
    long subPropertyOfID = -1L;
    long rdfsDatatypeID = -1L;
    long domainID = -1L;
    long rangeID = -1L;
    long rdfPropertyID = -1L;

    public RDFDatasetGenerator(String rdfDumpPath) {
        this.rdfDumpPath = rdfDumpPath;
        this.rdfDumpFile = new File(rdfDumpPath);
    }

    public static void main(String[] args) {
        File f = new File(CompressionTests.class.getClassLoader().getResource("pizza.ttl").getFile());
        RDFDatasetGenerator generator = new RDFDatasetGenerator(f.toString());
        generator.generate();
    }

    public RDFDataset generate() {
        insertAxiomaticTriples();
        processDataset(rdfDumpFile.toString());
        initVocabularyIDs();
        findPropertyIDsAndRDFClassIDs();
        substituteTempIDsByFinalIDs();

        RDFSReasoningDictionary dictionary = new RDFSReasoningDictionary(
                resourcesDictionarySection, literalsDictionarySection, rdfClassIDs, propertyIDs
        );
        RDFSReasoningTriples triples = new RDFSReasoningTriples(domainID, rangeID, rdfTypeID, subPropertyOfID, subClassOfID);
        tempTripleIDList.forEach(triples::add);

        return new RDFDataset(triples, dictionary);
    }

    private void substituteTempIDsByFinalIDs() {
        // ID of literals are shifted 32 bits to the left - substitute by final value
        tempTripleIDList.forEach(tID -> {
            if (tID.getObject() > Integer.MAX_VALUE) {
                // is literal - substitute by final ID
                long idWithoutOffset = tID.getObject() >> 32;
                tID.setObject(idWithoutOffset + resourcesDictionarySection.getNumberOfElements());
            }
        });
    }

    private void removeDuplicateTriples() {
        // TODO required?
    }

    private void initVocabularyIDs() {
        rdfsDatatypeID = resourcesDictionarySection.locate(RDFS.Datatype.toString());
        rdfTypeID = resourcesDictionarySection.locate(RDF.type.toString());
        subClassOfID = resourcesDictionarySection.locate(RDFS.subClassOf.toString());
        subPropertyOfID = resourcesDictionarySection.locate(RDFS.subPropertyOf.toString());
        rangeID = resourcesDictionarySection.locate(RDFS.range.toString());
        domainID = resourcesDictionarySection.locate(RDFS.domain.toString());
        rdfPropertyID = resourcesDictionarySection.locate(RDF.Property.toString());
    }

    private void findPropertyIDsAndRDFClassIDs() {
        // find property IDs and RDF class IDs
        tempTripleIDList.forEach(tID -> {
                    long predID = tID.getPredicate();
                    propertyIDs.add(predID);
                    if (predID == subPropertyOfID) {
                        propertyIDs.add(tID.getSubject());
                        propertyIDs.add(tID.getObject());
                    } else if (predID == subClassOfID) {
                        rdfClassIDs.add(tID.getSubject());
                        rdfClassIDs.add(tID.getObject());
                    } else if (predID == rdfTypeID) {
                        rdfClassIDs.add(tID.getObject());
                    } else if (predID == domainID || predID == rangeID) {
                        rdfClassIDs.add(tID.getObject());
                    } else if (predID == rdfTypeID && tID.getObject() == rdfPropertyID) {
                        propertyIDs.add(tID.getSubject());
                    }
                }
        );

    }

    public long processObject(CharSequence object) {
        Node node = NodeUtils.asNode(object.toString());
        if (node.isLiteral()) {
            Node_Literal literal = (Node_Literal) node;

            log.info("Literal: " + literal.getLiteral().toString());
            long literalID = literalsDictionarySection.add(literal.getLiteral().toString()) << 32;

            // insert: triple("s"^^d, rdf:type, d)
            // TODO: literal at subject position ???

            // insert: triple(d, rdf:type, rdfs:Datatype)
            log.info("Literal datatype: " + literal.getLiteralDatatypeURI());
            long datatypeID = resourcesDictionarySection.add(literal.getLiteralDatatypeURI());
            tempTripleIDList.add(new TripleID(datatypeID, rdfTypeID, rdfsDatatypeID));
            return literalID;

        } else {
            return resourcesDictionarySection.add(node.getURI());
        }
    }

    private void processDataset(String rdfDumpPath) {
        RDFParserRIOT parser = new RDFParserRIOT();
        RDFNotation rdfNotation = RDFNotation.guess(rdfDumpPath);
        log.info(rdfDumpPath);
        try {
            parser.doParse(rdfDumpPath, "", rdfNotation, (tripleString, count) -> {
                log.info(tripleString.toString());
                long sbjID = resourcesDictionarySection.add(tripleString.getSubject());
                long predID = resourcesDictionarySection.add(tripleString.getPredicate());
                long objID = processObject(tripleString.getObject());
                tempTripleIDList.add(new TripleID(sbjID, predID, objID));
            });
        } catch (ParserException e) {
            e.printStackTrace();
        }
    }

    public void insertAxiomaticTriples() {
        // RDF
        File axiomaticRDFTriples = new File(
                RDFDatasetGenerator.class.getClassLoader().getResource("axiomaticTriples_RDF.ttl").getFile());
        processDataset(axiomaticRDFTriples.toString());

        // RDFS
        File axiomaticRDFSTriples = new File(
                RDFDatasetGenerator.class.getClassLoader().getResource("axiomaticTriples_RDFS.ttl").getFile());
        processDataset(axiomaticRDFSTriples.toString());
    }
}
