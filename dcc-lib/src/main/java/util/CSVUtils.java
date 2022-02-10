package util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class CSVUtils {
    public static void writeCSVFile(String filePath, List<String> csvHeader, List<List<?>> csvRows, String delimiter) throws
            IOException {
        CSVFormat.Builder builder = CSVFormat.Builder.create()
                .setDelimiter(delimiter);
        File f = new File(filePath);
        Appendable out;
        if (!f.exists()) {
            // if it does not exist, create file and write header
            builder.setHeader(csvHeader.toArray(new String[0]));
            out = new FileWriter(f);
        } else {
            // append to CSV file without writing header
            out = Files.newBufferedWriter(
                    f.toPath(),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE
            );
        }
        CSVFormat csvFormat = builder.build();
        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (List<?> values : csvRows) {
                printer.printRecord(values);
            }
        }
    }
}
