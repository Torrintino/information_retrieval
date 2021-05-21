// DO NOT CHANGE THIS PACKAGE NAME.

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.util.regex.*;

class Document {

    int ID;
    boolean suspended;
    String name; // Full title
    String type;
    String year;
    ArrayList<String> plot;
    ArrayList<String> title; // Tokenized title

    public void Document() {
	plot = new ArrayList<>();
	title = new ArrayList<>();
    }

}

class Occurence {
    // An occurence is a pointer a document in the doc_list (first
    //   and a list of positions in that document (second), that contain the term
    int doc_id;
    ArrayList<Integer> pos_list;
}

class Index {
    // Map Terms to Occurences
    HashMap<String, Occurence> dict;
}

public class BooleanQuery {

    ArrayList<Document> doc_list;

    String MOVIE_NAME_REGEX = "[^\"]+";
    String YEAR_REGEX = " \\([0-9\\?]{4}(\\/(X|V|I|L|C)+)?\\)";
    String MOVIE_REGEX = MOVIE_NAME_REGEX + YEAR_REGEX;
    String SERIES_REGEX = "\"" + MOVIE_NAME_REGEX + "\"" + YEAR_REGEX;
    String EPISODE_REGEX = SERIES_REGEX + " \\{[^\\}]+\\}";
    String TELEVISION_REGEX = MOVIE_REGEX + " \\(TV\\)";
    String VIDEO_REGEX = MOVIE_REGEX + " \\(V\\)";
    String VIDEOGAME_REGEX = MOVIE_REGEX + " \\(VG\\)";

    /**
     * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
     */
    public BooleanQuery() {
    }

    private void skip_initial_lines(BufferedReader reader) throws IOException {
	String line;
	while ((line = reader.readLine()) != null) {
	    if (line.contains("PLOT SUMMARIES LIST")) {
		reader.readLine();
		return;
	    }
	}
    }

    private Document parse_headline(Document doc, String title_line) {
	if(title_line.matches(MOVIE_REGEX)) {
	    doc.type="movie";
	} else if (title_line.matches(SERIES_REGEX)) {
	    doc.type="series";
	} else if (title_line.matches(EPISODE_REGEX)) {
	    doc.type="episode";
	} else if (title_line.matches(TELEVISION_REGEX)) {
	    doc.type="television";
	} else if (title_line.matches(VIDEO_REGEX)) {
	    doc.type="video";
	} else if (title_line.matches(VIDEOGAME_REGEX)) {
	    doc.type="videogame";
	} else {
	    System.out.println("Unknown type: " + title_line);
	}

	Pattern year_pattern = Pattern.compile(YEAR_REGEX);
	Pattern episode_pattern = Pattern.compile("\\{[^\\}]+\\}");
	Matcher em = episode_pattern.matcher(title_line);

	if(doc.type == "episode") {
	    int episode_found = 0;
	    while(em.find())
		episode_found = em.start();
	    if(episode_found != 0)
		title_line = title_line.substring(0, episode_found);
	}

	Matcher m = year_pattern.matcher(title_line);
	String year = "";
	int pos = 0;
	boolean found = false;
	while(m.find()) {
	    found = true;
	    year = m.group();
	    pos = m.start();
	}
	if (!found) {
	    System.out.println("Could not find year in line");
	    return null;
	}

	String title = title_line.substring(0, pos).replace("\"", "");
	doc.title = new ArrayList<String>(Arrays.asList(title.split("[\\ \\.\\:\\!\\?]")));
	while(doc.title.remove("")); // Remove empty tokens
       	doc.year = year.substring(2,6);
	return doc;
    }

    private ArrayList<String> parse_plotline(String line) {
	line = line.substring(4);
	ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(line.split("[\\ \\.\\:\\!\\?]")));
	while(tokens.remove("")); // Remove empty tokens
	return tokens;
    }

    private void parse_plot(Document doc, BufferedReader reader) throws IOException {
	String line;
	while((line = reader.readLine()).contains("PL: ")) {
	    ArrayList<String> tokens = parse_plotline(line);
	}
    }

    private Document parse_paragraph(BufferedReader reader,
				  ArrayList<Document> doc_list) throws IOException {
	String line;
	do {
	    line = reader.readLine();
	    if(line == null)
		return null;
	} while(!line.contains("MV: "));
	
	Document doc = new Document();
	String title_line = line.substring(4);
	if(title_line.contains("{{SUSPENDED}}")) {
	    doc.suspended = true;
	    return doc;
	}
	doc.suspended = false;

	if(parse_headline(doc, title_line) == null)
	    return null;
	reader.readLine();
	parse_plot(doc, reader);
 
	return doc;
    }

    private void build_doc_list(Path plotFile) {
	doc_list = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(plotFile, ISO_8859_1)) {
	    skip_initial_lines(reader);
	    Document doc;
            while ((doc = parse_paragraph(reader, doc_list)) != null) {
		doc_list.add(doc);
	    }
	} catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
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
	System.out.println("Start building indices...");

	build_doc_list(plotFile);

	System.out.println("Done.");
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
