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

    public Bigrams() {


    }

    /**
     * A method for reading the textual movie plot file and building a Lucene index.
     * The purpose of the index is to speed up subsequent boolean searches using
     * the {@link #booleanQuery(String) booleanQuery} method.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param plotFile the textual movie plot file 'plot.list' to use
     */
    public void buildIndices(Path plotFile) {
	System.out.println("Start parsing...");
	build_doc_list(plotFile);

	
    }

    public ArrayList<String> splitQuery(String query) {
        ArrayList<String> tokens = new ArrayList<>();
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i<query.length(); i++) {
            char c = query.charAt(i);
            switch(c) {
                case ' ':
                case '(':
                case ')':
                    if(buffer.length() > 0) {
                        tokens.add(buffer.toString());
                        buffer = new StringBuffer();
                    }
                    if(c != ' ')
                        tokens.add(Character.toString(c));
                    break;
                default:
                    buffer.append(c);
                    if (i == query.length() - 1)
                        tokens.add(buffer.toString());
            }
        }
        return tokens;
    }

    public String expand(String token) {
        String[] parts = token.split(":");
        String field = parts[0];
        String value = parts[1];

        ArrayList<String> synonyms = synset.get(value);
        if(synonyms == null) // || field.equals("type")
            return token;

        String result = "(";
        boolean first = true;
        for(String synonym : synonyms) {
            if(first) {
                first = false;
            } else {
                result += " OR ";
            }
            result += field + ":" + synonym;
	    //System.out.println("Synonym to " + value + ": " + synonym);

        }
        result += " OR " + token + ")";
        return result;
    }

    public String expandQuery(String query) {
        String result = "";
        ArrayList<String> tokens = splitQuery(query);

        boolean first = true;
        for(String token : tokens) {
            if(first) {
                first = false;
            } else {
                result += " ";
            }

            if(token.equals("(")
                    || token.equals(")")
                    || token.equals("AND")
                    || token.equals("OR")) {
                result += token;
            } else {
                result += expand(token);
            }
        }
        // For testing, remove prints before deploy
        System.out.println("Expansion: " + result);

        return result;

    }

    /**
     * A method for performing a boolean search on a textual movie plot file after
     * Lucene indices were built using the {@link #buildIndices(Path) buildIndices}
     * method. The movie plot file contains entries of the <b>types</b> movie,
     * series, episode, television, video, and videogame. This method allows queries
     * following the Lucene query syntax on any of the <b>fields</b> title, plot, year,
     * episode, and type. Note that queries are case-insensitive and stop words are
     * removed.<br>
     * <br>
     * <p>
     * More details on the query syntax can be found at <a
     * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
     * https://wordnet.princeton.edu/documentation/wndb5wn</a>.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param queryString the query string, formatted according to the Lucene query syntax.
     * @return the exact content (in the textual movie plot file) of the title
     * lines (starting with "MV: ") of the documents matching the query
     */
    public Set<String> booleanQuery(String queryString) {
        queryString = expandQuery(queryString);

        HashSet<String> result = new HashSet<>();
        Query query = null;
        TopDocs hits = null;

        try {
            query = queryParser.parse(queryString);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            hits = indexSearcher.search(query, 100);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        for(ScoreDoc scoreDoc : hits.scoreDocs) {
            try {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                result.add(doc.get("name"));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        return result;
    }

    /**
     * A method for closing any open file handels or a ThreadPool.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     */
    public void close() {
        //TODO: Implement this method!
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java -jar BooleanQueryWordnet.jar <plot list file> <wordnet directory> <queries file> <results file>");
            System.exit(-1);
        }

        // build indices + synsets
        System.out.println("Building indices...");
        long tic = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.totalMemory();

        Path plotFile = Paths.get(args[0]);
        Path wordNetDir = Paths.get(args[1]);

        BooleanQueryWordnet bq = new BooleanQueryWordnet();
        bq.buildSynsets(wordNetDir);

        bq.buildIndices(plotFile);
        System.gc();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        System.out.println("Runtime: " + (System.nanoTime() - tic) + " nanoseconds");
        System.out.println("Memory: " + ((runtime.totalMemory() - mem) / (1048576L)) + " MB (rough estimate)");

        // parsing the queries that are to be run from the queries file
        List<String> queries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[2]), ISO_8859_1)) {
            String line;
            while ((line = reader.readLine()) != null) {
                queries.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // parsing the queries' expected results from the results file
        List<Set<String>> results = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[3]), ISO_8859_1)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Set<String> result = new HashSet<>();
                results.add(result);
                for (int i = 0; i < Integer.parseInt(line); i++) {
                    result.add(reader.readLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // run queries
        for (int i = 0; i < queries.size(); i++) {
            String query = queries.get(i);
            Set<String> expectedResult = i < results.size() ? results.get(i) : new HashSet<>();
            System.out.println();
            System.out.println("Query:           " + query);
            tic = System.nanoTime();
            Set<String> actualResult = bq.booleanQuery(query);

            // sort expected and determined results for human readability
            List<String> expectedResultSorted = new ArrayList<>(expectedResult);
            List<String> actualResultSorted = new ArrayList<>(actualResult);
            Comparator<String> stringComparator = Comparator.naturalOrder();
            expectedResultSorted.sort(stringComparator);
            actualResultSorted.sort(stringComparator);

            System.out.println("Runtime:         " + (System.nanoTime() - tic) + " nanoseconds.");
            System.out.println("Expected result (" + expectedResultSorted.size() + "): " + expectedResultSorted.toString());
            System.out.println("Actual result (" + actualResultSorted.size() + "):   " + actualResultSorted.toString());
            System.out.println(expectedResult.equals(actualResult) ? "SUCCESS" : "FAILURE");
        }

        bq.close();
    }
}
