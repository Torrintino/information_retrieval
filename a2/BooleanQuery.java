// DO NOT CHANGE THIS PACKAGE NAME.

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class BooleanQuery {

    /**
     * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
     */
    public BooleanQuery() {
    }

    /**
     * A method for reading the textual movie plot file and building indices. The
     * purpose of these indices is to speed up subsequent boolean searches using
     * the {@link #booleanQuery(String) booleanQuery} method.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param plotFile the textual movie plot file 'plot.list' for personal, non-commercial
     *                 use.
     */
    public void buildIndices(Path plotFile) {
        // TODO: insert code here
    }

    /**
     * A method for performing a boolean search on a textual movie plot file after
     * indices were built using the {@link #buildIndices(String) buildIndices}
     * method. The movie plot file contains entries of the <b>types</b> movie,
     * series, episode, television, video, and videogame. This method allows term
     * and phrase searches (the latter being enclosed in double quotes) on any of
     * the <b>fields</b> title, plot, year, episode, and type. Multiple term and
     * phrase searches can be combined by using the character sequence " AND ".
     * Note that queries are case-insensitive.<br>
     * <br>
     * Examples of queries include the following:
     *
     * <pre>
     * title:"game of thrones" AND type:episode AND plot:shae AND plot:Baelish
     * plot:Skywalker AND type:series
     * plot:"year 2200"
     * plot:Berlin AND plot:wall AND type:television
     * plot:Cthulhu
     * title:"saber rider" AND plot:april
     * plot:"James Bond" AND plot:"Jaws" AND type:movie
     * title:"Pimp my Ride" AND episodetitle:mustang
     * plot:"matt berninger"
     * title:"grand theft auto" AND type:videogame
     * plot:"Jim Jefferies"
     * plot:Berlin AND type:videogame
     * plot:starcraft AND type:movie
     * type:video AND title:"from dusk till dawn"
     * </pre>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param queryString the query string, formatted according to the Lucene query syntax,
     *                    but only supporting term search, phrase search, and the AND
     *                    operator
     * @return the exact content (in the textual movie plot file) of the title
     * lines (starting with "MV: ") of the documents matching the query
     */
    public Set<String> booleanQuery(String queryString) {
        // TODO: insert code here
        return new HashSet<>();
    }

    public static void main(String[] args) {
        BooleanQuery bq = new BooleanQuery();
        if (args.length < 3) {
            System.err.println("Usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
            System.exit(-1);
        }

        Path moviePlotFile = Paths.get(args[0]);
        Path queriesFile = Paths.get(args[1]);
        Path resultsFile = Paths.get(args[2]);

        // build indices
        System.out.println("Building indices...");
        long tic = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.totalMemory();
        bq.buildIndices(moviePlotFile);

        System.out.println("Runtime: " + (System.nanoTime() - tic) + " nanoseconds");
        System.out.println("Memory: " + ((runtime.totalMemory() - mem) / (1048576L)) + " MB (rough estimate)");

        // parsing the queries that are to be run from the queries file
        List<String> queries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(queriesFile, ISO_8859_1)) {
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
        try (BufferedReader reader = Files.newBufferedReader(resultsFile, ISO_8859_1)) {
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
            System.out.println("Expected result: " + expectedResultSorted.toString());
            System.out.println("Actual result:   " + actualResultSorted.toString());
            System.out.println(expectedResult.equals(actualResult) ? "SUCCESS" : "FAILURE");
        }
    }

}
