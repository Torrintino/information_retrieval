// DO NOT CHANGE THIS PACKAGE NAME.

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import doc.DocumentParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class BooleanQueryLucene {

    IndexSearcher indexSearcher;
    MultiFieldQueryParser queryParser;

    /**
     * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
     */
    public BooleanQueryLucene() {
    }

    /**
     * A method for reading the textual movie plot file and building a Lucene index.
     * The purpose of the index is to speed up subsequent boolean searches using
     * the {@link #booleanQuery(String) booleanQuery} method.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param plotFile the textual movie plot file 'plot.list', obtainable from <a
     *                 href="http://www.imdb.com/interfaces"
     *                 >http://www.imdb.com/interfaces</a> for personal, non-commercial
     *                 use.
     */
    public void buildIndices(Path plotFile) {
	StandardAnalyzer myAnalyzer = new StandardAnalyzer();

	try {
	    Directory index = FSDirectory.open(Paths.get("../../index"));
	    IndexWriterConfig config = new IndexWriterConfig(myAnalyzer);
	    IndexWriter writer = new IndexWriter(index, config);

	    DocumentParser parser = new DocumentParser();
	    parser.build_doc_list(plotFile, writer);
	    writer.commit();
	    writer.close();
	    IndexReader reader = DirectoryReader.open(index);
	    indexSearcher = new IndexSearcher(reader);
	    queryParser = new MultiFieldQueryParser(new String[]{"title", "plot", "type"}, new StandardAnalyzer());
	} catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
	}
    }

    /**
     * A method for performing a boolean search on a textual movie plot file after
     * Lucene indices were built using the {@link #buildIndices(String) buildIndices}
     * method. The movie plot file contains entries of the <b>types</b> movie,
     * series, episode, television, video, and videogame. This method allows queries
     * following the Lucene query syntax on any of the <b>fields</b> title, plot, year,
     * episode, and type. Note that queries are case-insensitive and stop words are
     * removed.<br>
     * <br>
     * Examples of queries include the following:
     *
     * <pre>
     * title:"game of thrones" AND type:episode AND (plot:Bastards OR (plot:Jon AND plot:Snow)) -plot:son
     * title:"Star Wars" AND type:movie AND plot:Luke AND year:[1977 TO 1987]
     * plot:Berlin AND plot:wall AND type:television
     * plot:men~1 AND plot:women~1 AND plot:love AND plot:fool AND type:movie
     * title:westworld AND type:episode AND year:2016 AND plot:Dolores
     * plot:You AND plot:never AND plot:get AND plot:A AND plot:second AND plot:chance
     * plot:Hero AND plot:Villain AND plot:destroy AND type:movie
     * (plot:lover -plot:perfect) AND plot:unfaithful* AND plot:husband AND plot:affair AND type:movie
     * (plot:Innocent OR plot:Guilty) AND plot:crime AND plot:murder AND plot:court AND plot:judge AND type:movie
     * plot:Hero AND plot:Marvel -plot:DC AND type:movie
     * plot:Hero AND plot:DC -plot:Marvel AND type:movie
     * </pre>
     * <p>
     * More details on the query syntax can be found at <a
     * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
     * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param queryString the query string, formatted according to the Lucene query syntax.
     * @return the exact content (in the textual movie plot file) of the title
     * lines (starting with "MV: ") of the documents matching the query
     */
    public Set<String> booleanQuery(String queryString) {
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
     * A method for closing any resources (e.g. open file handels or a thread pool).
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     */
    public void close() {
        // TODO: you may insert code here
    }

    public static void main(String[] args) {
        BooleanQueryLucene bq = new BooleanQueryLucene();
        if (args.length < 3) {
            System.err.println("Usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
            System.exit(-1);
        }

        // build indices
        System.out.println("Building indices...");

        long tic = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.totalMemory();

        Path plotFile = Paths.get(args[0]);
        bq.buildIndices(plotFile);

        System.out.println("Runtime: " + (System.nanoTime() - tic) + " nanoseconds");
        System.out.println("Memory: " + ((runtime.totalMemory() - mem) / (1048576L)) + " MB (rough estimate)");

        // parsing the queries that are to be run from the queries file
        List<String> queries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[1]), ISO_8859_1)) {
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
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[2]), ISO_8859_1)) {
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

        bq.close();
    }

}
