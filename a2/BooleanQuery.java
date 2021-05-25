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
    String name;
    String type;
    String year;
    ArrayList<String> plot;
    ArrayList<String> title; // Tokenized title

    public void Document() {}

}

public class BooleanQuery {

    ArrayList<Document> doc_list;
    HashMap<String, HashMap<Integer, ArrayList<Integer>>> title_index;
    HashMap<String, HashMap<Integer, ArrayList<Integer>>> plot_index;
    HashMap<String, LinkedList<Integer>> type_index;

    String MOVIE_NAME_REGEX = "[^\"]+";
    String YEAR_REGEX = " \\([0-9\\?]{4}(\\/(X|V|I|L|C)+)?\\)";
    String MOVIE_REGEX = MOVIE_NAME_REGEX + YEAR_REGEX;
    String SERIES_REGEX = "\"" + MOVIE_NAME_REGEX + "\"" + YEAR_REGEX;
    String EPISODE_REGEX = SERIES_REGEX + " \\{[^\\}]+\\}";
    String TELEVISION_REGEX = MOVIE_REGEX + " \\(TV\\)";
    String VIDEO_REGEX = MOVIE_REGEX + " \\(V\\)";
    String VIDEOGAME_REGEX = MOVIE_REGEX + " \\(VG\\)";

    String DELIM_REGEX = "[\\ \\.\\:\\!\\?]";

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

	if(doc.type.equals("episode")) {
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
	doc.title = new ArrayList<String>(Arrays.asList(title.split(DELIM_REGEX)));
	while(doc.title.remove("")); // Remove empty tokens
	for(int i=0; i<doc.title.size(); i++)
	    doc.title.set(i, doc.title.get(i).toLowerCase());
       	doc.year = year.substring(2,6);
	return doc;
    }

    private ArrayList<String> parse_plotline(String line) {
	line = line.substring(4);
	ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(line.split(DELIM_REGEX)));
	while(tokens.remove("")); // Remove empty tokens
	for(int i=0; i<tokens.size(); i++)
	    tokens.set(i, tokens.get(i).toLowerCase());
	return tokens;
    }

    private void parse_plot(Document doc, BufferedReader reader) throws IOException {
	String line;
	doc.plot = new ArrayList<>();
	while((line = reader.readLine()).contains("PL: ")) {
	    ArrayList<String> tokens = parse_plotline(line);
	    doc.plot.addAll(tokens);
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
	doc.name = line;
	String title_line = line.substring(4);
	int pos;
	if((pos = title_line.indexOf("{{SUSPENDED}}")) != -1)
	    title_line = title_line.substring(0, pos-1);

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
	    int i = 0;
            while ((doc = parse_paragraph(reader, doc_list)) != null) {
		doc.ID = i;
		doc_list.add(doc);
		i++;
	    }
	} catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void build_title_index() {
	title_index = new HashMap<>();
	Document doc;

	for(int j=0; j<doc_list.size(); j++) {
	    doc = doc_list.get(j);

	    for(int i=0; i<doc.title.size(); i++) {
		String key = doc.title.get(i);
		HashMap<Integer, ArrayList<Integer>> entry = title_index.get(key);
		if(entry == null) {
		    entry = new HashMap<>();
		    title_index.put(key, entry);
		}
		ArrayList<Integer> pos_list = entry.get(j);
		if(pos_list == null) {
		    pos_list = new ArrayList<Integer>();
		    entry.put(j, pos_list);
		}
		pos_list.add(i);
	    }
	}
    }

    private void build_plot_index() {
	plot_index = new HashMap<>();
	Document doc;

	for(int j=0; j<doc_list.size(); j++) {
	    doc = doc_list.get(j);

	    for(int i=0; i<doc.plot.size(); i++) {
		String key = doc.plot.get(i);
		HashMap<Integer, ArrayList<Integer>> entry = plot_index.get(key);
		if(entry == null) {
		    entry = new HashMap<>();
		    plot_index.put(key, entry);
		}
		ArrayList<Integer> pos_list = entry.get(j);
		if(pos_list == null) {
		    pos_list = new ArrayList<Integer>();
		    entry.put(j, pos_list);
		}
		pos_list.add(i);
	    }
	}
    }

    private void build_type_index() {
	type_index = new HashMap<>();

	for(String type : Arrays.asList("movie", "series", "episode",
					"television", "video", "videogame")) {
	    type_index.put(type, new LinkedList<Integer>());
	}

	Document doc;
	for(int j=0; j<doc_list.size(); j++) {
	    doc = doc_list.get(j);
	    type_index.get(doc.type).add(j);
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
	System.out.println("Start parsing...");
	build_doc_list(plotFile);

	System.out.println("Start building indices...");
	System.out.println("Title index...");
	build_title_index();
	System.out.println("Plot index...");
	build_plot_index();
	System.out.println("Start building indices...");
	build_type_index();

	System.out.println("Done.");
    }

    public boolean phrase_search(ArrayList<String> doc, ArrayList<Integer> occurences,
				      ArrayList<String> phrase) {
	boolean found;
	for(var o : occurences) {
	    found = true;
	    for(int i=0; i<phrase.size(); i++) {
		if(o + i >= doc.size()) {
		    found = false;
		    break;
		}
		String phrase_token = phrase.get(i);
		String doc_token = doc.get(o + i);
		if(!(phrase_token.equals(doc_token))) {
		    found = false;
		    break;
		}
	    }
	    if(found)
		return true;
	}
	return false;
    }

    public LinkedList<Integer> plot_search(String phrase) {
	LinkedList<Integer> result = new LinkedList<>();
	if(phrase.charAt(0) == '"') {
		phrase = phrase.substring(1, phrase.length() - 1);
		ArrayList<String> token_list = new ArrayList<String>(Arrays.asList(phrase.split(DELIM_REGEX)));
		var doc_map = plot_index.get(token_list.get(0));
		if(doc_map == null)
		    return result;
		for(int doc_id : doc_map.keySet()) {
		    if(phrase_search(doc_list.get(doc_id).plot, doc_map.get(doc_id), token_list))
			result.add(doc_id);
		}
		return result;
	}

	if(!plot_index.containsKey(phrase))
	    return result;
	for(var doc_id : plot_index.get(phrase).keySet())
	    result.add(doc_id);
	return result;
    }

    public LinkedList<Integer> title_search(String phrase) {
	LinkedList<Integer> result = new LinkedList<>();
	if(phrase.charAt(0) == '"') {
		phrase = phrase.substring(1, phrase.length() - 1);
		ArrayList<String> token_list = new ArrayList<String>(Arrays.asList(phrase.split(DELIM_REGEX)));
		var doc_map = title_index.get(token_list.get(0));
		if(doc_map == null)
		    return result;
		for(int doc_id : doc_map.keySet()) {
		    if(phrase_search(doc_list.get(doc_id).title, doc_map.get(doc_id), token_list))
			result.add(doc_id);
		}
		return result;
	}

	if(!title_index.containsKey(phrase))
	    return result;
	for(var doc_id : title_index.get(phrase).keySet())
	    result.add(doc_id);
	return result;
    }

    public LinkedList<Integer> type_search(String type) {
	LinkedList<Integer> result = new LinkedList<>();

	if(!Arrays.asList("movie", "series", "episode",
			  "television", "video", "videogame").contains(type)) {
	    System.out.println("Type not supported: " + type);
	    return result;
	}
	
	return type_index.get(type);
    }

    public LinkedList<Integer> get_query_results(String token) {
	String[] query = token.split(":");
	String field = query[0];
	String phrase = query[1];

	phrase = phrase.toLowerCase();
	if(field.equals("plot")) {
	    return plot_search(phrase);
	} else if (field.equals("title")) {
	    return title_search(phrase);
	} else if (field.equals("type")) {
	    return type_search(phrase);
	} else {
	    System.err.println("Field not supported: " + field);
	}
	return null;
    }

    /*  Gets the result of the token.
     *  If current_result is empty, every found document will be saved.
     *  Otherwise all entries in current_result
     */
    public LinkedList<Integer> update_query_results(LinkedList<Integer> current_result,
						    String token,
						    boolean first) {
	if(!first) {
	    if(current_result.size() == 0)
		return current_result;
	}
	    
	LinkedList<Integer> tmp_result = get_query_results(token);
	if(tmp_result == null)
	    return null;
	Collections.sort(tmp_result);
		     
	if(first)
	    return tmp_result;

	int i=0, j=0;
	LinkedList<Integer> result = new LinkedList<>();
	while(i < current_result.size() && j < tmp_result.size()) {
	    int doc1 = current_result.get(i);
	    int doc2 = tmp_result.get(j);
	    if(doc1 == doc2) {
		result.add(doc1);
		i++;
		j++;
	    } else if (doc1 < doc2) {
		i++;
	    } else {
		j++;
	    }
	}
	return result;
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
	ArrayList<String> query_tokens = new ArrayList<>(Arrays.asList(queryString.split(" AND ")));
	for(int i=0; i<query_tokens.size(); i++)
	    query_tokens.set(i, query_tokens.get(i).strip());


	LinkedList<Integer> current_result = null;
	boolean first = true;
	for(var token : query_tokens) {
	    current_result = update_query_results(current_result, token, first);
	    if(current_result == null)
		return new HashSet<>();
	    first = false;
	}

	HashSet<String> result = new HashSet<>();
	for(int doc_id : current_result)
	    result.add(doc_list.get(doc_id).name);

        return result;
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
