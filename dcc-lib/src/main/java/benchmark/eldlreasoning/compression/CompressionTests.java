package benchmark.eldlreasoning.compression;

import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.rdf.RDFParserCallback;
import org.rdfhdt.hdt.rdf.parsers.RDFParserRIOT;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CompressionTests {

    public static void main(String[] args) throws ParserException {
        parsingTests();
    }

    public static void parsingTests() throws ParserException {
        File f = new File(CompressionTests.class.getClassLoader().getResource("pizza.ttl").getFile());


        HashDictionary dictionary = new HashDictionary();
        RDFParserRIOT parser = new RDFParserRIOT();
        RDFNotation rdfNotation = RDFNotation.guess(f);
        parser.doParse(f.toString(), "", rdfNotation, new RDFParserCallback.RDFCallback() {
            @Override
            public void processTriple(TripleString tripleString, long l) {
                System.out.println(tripleString);
                dictionary.add(tripleString.getSubject().toString());
                dictionary.add(tripleString.getPredicate().toString());
                dictionary.add(tripleString.getObject().toString());
            }
        });

    }

    public static void hashMapSpace() {
        Map<Integer, String> map = new HashMap<>();
        long usedMemoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println();
        for (int i = 0; i < 500_000; i++) {
            map.put(i, i + "");
        }
        long usedMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Required space by map: " + (usedMemoryAfter - usedMemoryBefore));
    }
}
