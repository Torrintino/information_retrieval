// IO
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// Data structures
import java.util.LinkedList;
import java.util.Queue;

// Network
import java.net.URI;;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// JSON
import org.json.JSONObject;
import org.json.JSONArray;

// Jsoup
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class IMDBSpider {

    public IMDBSpider() {
    }

    /**
     * For each title in file movieListJSON:
     *
     * <pre>
     * You should:
     * - First, read a list of 500 movie titles from the JSON file in 'movieListJSON'.
     *
     * - Secondly, for each movie title, perform a web search on IMDB and retrieve
     * movie’s URL: https://www.imdb.com/find?q=<MOVIE>&s=tt&ttype=ft
     *
     * - Thirdly, for each movie, extract metadata (actors, budget, description)
     * from movie’s URL and store to a JSON file in directory 'outputDir':
     *    https://www.imdb.com/title/tt0499549/?ref_=fn_al_tt_1 for Avatar - store
     * </pre>
     *
     * @param movieListJSON JSON file containing movie titles
     * @param outputDir     output directory for JSON files with metadata of movies.
     * @throws IOException
     */
    public void persistOutput(JSONArray result, String outputDir) {
	String file_name = outputDir + "result.json";
	File output_file = new File(file_name);
	try {
	    output_file.createNewFile();
	} catch (IOException e) {
	    System.err.println("Cannot create new file");
	    return;
	}
	try {
	    FileWriter writer = new FileWriter(file_name);
	    writer.write(result.toString());
	    writer.close();
	} catch (IOException e) {
	    System.err.println("Cannot write to file");
	    return;
	}
    }

    public void fetchIMDBMovies(String movieListJSON, String outputDir) throws IOException {
	// TODO: ID is missing
	Queue<String> movie_queue = new LinkedList<>();
	JSONArray result = new JSONArray();
	File file = new File(movieListJSON);
	String content = FileUtils.readFileToString(file, "utf-8");
        JSONArray movie_list = new JSONArray(content);

	for (Object o : movie_list) {
	    JSONObject movie_object = (JSONObject) o;
	    String movie = (String) movie_object.get("movie_name");
	    movie_queue.add(movie);
	}

	var client = HttpClient.newHttpClient();
	for (String movie : movie_queue) {
	    var output = this.scrapeMovie(client, movie);
	    result.put(output);
	}
	//this.persistOutput(result, outputDir);
    }

    public String titleToQuery(String title) {
	return title.replaceAll(" ", "+")
	    .replaceAll(":", "%3A")
	    .replaceAll(",", "%2C")
	    .replaceAll("&", "%26")
	    .replaceAll("/", "%2F");
    }

    public Element getLinkFromSearch(String content) {
	Document search_html = Jsoup.parse(content);
	Element table = search_html.getElementsByClass("findList").first();
	Element tr = table.getElementsByTag("tr").first();
	Element td = tr.getElementsByClass("result_text").first();
	return td.getElementsByTag("a").first();
    }

    public String getContent(HttpClient client, String url) {
	HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .build();
	HttpResponse<String> response;
	try {
	    response = client.send(request, HttpResponse.BodyHandlers.ofString());
	} catch (Exception e) {
	    System.err.println("Connection error for URL: " + url);
	    return null;
	}
	return response.body();
    }

    public JSONObject scrapeMovie(HttpClient client, String title) {
	// ToDo: Consider Async Call
	String url = "https://www.imdb.com/find?q=" + titleToQuery(title) + "&s=tt&ttype=ft";
	String content = getContent(client, url);
	if (content == null) {
	    return null;
	}
	Element link = getLinkFromSearch(content);
	String href = "https://www.imdb.com" + link.attr("href") + "?ref_=fn_ft_tt_1";
	JSONObject result = parsePage(client, href);
	return result;
    }

    public JSONObject parsePage(HttpClient client, String url) {
	JSONObject result = new JSONObject();
	result.put("url", url);
	
	String content = getContent(client, url);
	Document html = Jsoup.parse(content);

	Element title_wrapper = html.getElementsByClass("title_wrapper").first();
	Element heading = title_wrapper.getElementsByTag("h1").first();
	String title = heading.text();
	String year = heading.child(0).child(0).text();
	result.put("title", title);
	result.put("year", year);

	JSONArray genreList = new JSONArray();
        Element genreSection = html.select("h4:contains(Genres:)").first().parent();
	Elements genreElements = genreSection.getElementsByTag("a");
	for(var e : genreElements) {
	    genreList.put(e.text());
	}
	result.put("genreList", genreList);
	
	return result;
    }

    /**
     * Helper method to remove html and formatting from text.
     *
     * @param text The text to be cleaned
     * @return clean text
     */
    protected static String cleanText(String text) {
        return text.replaceAll("\\<.*?>", "").replace("&nbsp;", " ")
            .replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    public static void main(String[] argv) throws IOException {
        String moviesPath = "./data/movies.json";
        String outputDir = "./data";

        if (argv.length == 2) {
            moviesPath = argv[0];
            outputDir = argv[1];

        } else if (argv.length != 0) {
            System.out.println("Call with: IMDBSpider.jar <moviesPath> <outputDir>");
            System.exit(0);
        }

        IMDBSpider sp = new IMDBSpider();
        sp.fetchIMDBMovies(moviesPath, outputDir);
    }
}
