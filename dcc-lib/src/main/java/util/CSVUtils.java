package util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVUtils {
    public static void writeCSVFile(String filePath, List<String> csvHeader, List<List<?>> csvRows, String delimiter) throws
            IOException {
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(csvHeader.toArray(new String[0]))
                .setDelimiter(delimiter)
                .build();
        FileWriter out = new FileWriter(filePath);
        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (List<?> values : csvRows) {
                printer.printRecord(values);
            }
        }
    }
}
