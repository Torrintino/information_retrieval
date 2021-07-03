import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.StringBuffer;
import java.util.*;

import doc.DocumentParser;

import static java.nio.charset.StandardCharsets.ISO_8859_1;


public class Bigrams {

    public Bigrams() {}

    public void buildBigrams(Path plotFile) {
	System.out.println("Start parsing...");
	DocumentParser parser = new DocumentParser();
	parser.build_doc_list(plotFile);	
    }

    public void close() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar BooleanQueryWordnet.jar <plot list file>");
            System.exit(-1);
        }

	Bigrams bg = new Bigrams();
        Path plotFile = Paths.get(args[0]);
        bg.buildBigrams(plotFile);
    }
}
