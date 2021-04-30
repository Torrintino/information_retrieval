import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.List;

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
    public void fetchIMDBMovies(String movieListJSON, String outputDir) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(movieListJSON));

        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonArray movie_list = gson.fromJson(bufferedReader, JsonArray.class);

        int movie_list_length = movie_list.size();

        String[] movies = new String[movie_list_length];

        for(int i=0;i<movie_list_length;i++){
            movies[i] = movie_list.get(i).getAsJsonObject().get("movie_name").getAsString();
        }

        JsonArray arrayResult = new JsonArray();
        int i = 0;

        for(String movie : movies){

            //for debugging
            System.out.println(i+"/500");

            JsonObject result = scrapeMoviePage(movie, movies, i);
            arrayResult.add(result);

            try (Writer writer = new FileWriter(outputDir + "/" + movie.toLowerCase().replaceAll("/", "-").replaceAll(" ", "-") + ".json")) {
                gson.toJson(arrayResult, writer);
            }

            arrayResult.remove(0);

            i++;

        }

    }

    public JsonObject scrapeMoviePage(String target, String[] movies, int index) throws IOException {

        JsonObject result = new JsonObject();
        String movie_url = getTargetURL(target);
        Document html = Jsoup.connect(movie_url).get();

        // Movie URL

        result.addProperty("url", movie_url);

        // Title

        try {

            String title = html.select("div.originalTitle").first().ownText();

            if(title.equals(target)){
                result.addProperty("title", target);
            } else {
                result.addProperty("title", title);
                movies[index] = title;
            }

        } catch (NullPointerException n) {

            try {

                String title = html.select("div.title_wrapper").select("h1").first().ownText();

                if(title.equals(target)){
                    result.addProperty("title", target);
                } else {
                    result.addProperty("title", title);
                    movies[index] = title;
                }

            } catch (NullPointerException m) {

                result.addProperty("title", "");

            }
        }

        // Year

        try {

            Element span = html.getElementById("titleYear");
            Elements a = span.select("a");
            String year = a.text();

            result.addProperty("year", year);

        } catch (NullPointerException n){

            result.addProperty("year", "");

        }

        // GenreList

        JsonArray genres = new JsonArray();

        try {

            Element genreStart = html.select("h4:contains(Genre)").first().parent();
            Elements genreCollection = genreStart.getElementsByTag("a");

            for(Element genre : genreCollection){
                genres.add(genre.text());
            }

            result.add("genreList", genres);

        } catch (NullPointerException n) {

            result.add("genreList", genres);

        }

        // CountryList

        JsonArray countries = new JsonArray();

        try {

            Element countryStart = html.select("h4:contains(Countr)").first().parent();
            Elements countryCollection = countryStart.getElementsByTag("a");

            for(Element country : countryCollection){
                countries.add(country.text());
            }

            result.add("countryList", countries);

        } catch (NullPointerException n) {

            result.add("countryList", countries);

        }

        // KeywordList

        JsonArray keywords = new JsonArray();

        try {

            Element keywordStart = html.select("h4:contains(Plot Keyword)").first().parent();
            Elements keywordCollection = keywordStart.getElementsByTag("a").not("a:contains(See All)");

            for(Element keyword : keywordCollection){
                keywords.add(keyword.text());
            }

            result.add("keywordList", keywords);

        } catch (NullPointerException n) {

            result.add("keywordList", keywords);

        }

        // Description

        try {

            Element storyline = html.select("h2:contains(Storyline)").first().nextElementSibling().child(0).child(0);
            String description = storyline.text();

            result.addProperty("description", description);

        } catch (NullPointerException n) {

            result.addProperty("description", "");

        }

        // DirectorList

        JsonArray directors = new JsonArray();

        try {

            Element directorStart = html.select("h4:contains(Director)").first().parent();
            Elements directorCollection = directorStart.getElementsByTag("a").not("a:contains(more credit)");

            for(Element director : directorCollection){
                directors.add(director.text());
            }

            result.add("directorList", directors);

        } catch (NullPointerException n) {

            result.add("directorList", directors);

        }

        // CastList

        JsonArray cast = new JsonArray();

        try {

            Element castStart = html.select("table.cast_list").first();
            Elements castCollection = castStart.getElementsByAttributeValueContaining("href", "/name/");

            for(Element actor : castCollection){

                if(actor.text().equals("")){
                    continue;
                }

                cast.add(actor.text());
            }

            result.add("castList", cast);

        } catch (NullPointerException n) {

            result.add("castList", cast);

        }

        // CharacterList

        JsonArray characters = new JsonArray();

        try {

            Element castStart = html.select("table.cast_list").first();
            Elements characterCollection = castStart.getElementsByClass("character");

            for(Element character : characterCollection){
                characters.add(character.text());
            }

            result.add("characterList", characters);

        } catch (NullPointerException n) {

            result.add("characterList", characters);

        }

        // Duration

        try {

            Element movie_length = html.select("time").first();
            String duration = movie_length.text();

            result.addProperty("duration", duration);

        } catch (NullPointerException n) {

            result.addProperty("duration", "");

        }

        // Budget

        try {

            String budget = "";

            Element budgetElement = html.select("h4:contains(Budget)").first().parent();
            budgetElement.select("h4").remove();

            if (budgetElement.text().contains("$")) {

                budget = budgetElement.text();

            }

            result.addProperty("budget", budget);

        } catch (NullPointerException n){

            result.addProperty("budget", "");

        }

        // Gross

        String gross = "";

        try {

            Element grossElement = html.select("h4:contains(Cumulative Worldwide Gross)").first().parent();
            grossElement.select("h4").remove();

            if(grossElement.text().contains("$")) {

                gross = grossElement.text();

            } else {

                grossElement = html.select("h4:contains(Gross USA)").first().parent();
                grossElement.select("h4").remove();

                if(grossElement.text().contains("$")) {

                    gross = grossElement.text();

                }

            }

            result.addProperty("gross", gross);

        } catch (NullPointerException n) {

            try {

                Element grossElement = html.select("h4:contains(Gross USA)").first().parent();
                grossElement.select("h4").remove();

                if(grossElement.text().contains("$")) {

                    gross = grossElement.text();

                }

                result.addProperty("gross", gross);

            } catch (NullPointerException m) {

                result.addProperty("gross", gross);

            }

        }

        // RatingValue

        try {

            String ratingValue = html.select("div.ratingValue").first().child(0).child(0).text();

            result.addProperty("ratingValue", ratingValue);

        } catch (NullPointerException n) {

            result.addProperty("ratingValue", "");

        }

        // RatingCount

        try {

            String ratingCount = html.select("div.ratingValue").first().nextElementSibling().child(0).text();

            result.addProperty("ratingCount", ratingCount);

        } catch (NullPointerException n) {

            result.addProperty("ratingCount", "");

        }

        // ContentRating

        try {

            Element titleWrapper = html.select("div.title_wrapper").first();
            String contentRating = titleWrapper.select("div.subtext").first().ownText().replaceAll(",", "").trim();

            result.addProperty("contentRating", contentRating);

        } catch (NullPointerException n) {

            result.addProperty("contentRating", "");

        }

        // AspectRatio

        try {

            Element aspectRatioElement = html.select("h4:contains(Aspect Ratio)").first().parent();
            aspectRatioElement.select("h4").remove();
            String aspectRatio = aspectRatioElement.text();

            result.addProperty("aspectRatio", aspectRatio);

        } catch (NullPointerException n) {

            result.addProperty("aspectRatio", "");

        }

        // Reviews

        try {

            Element titleReviewBar = html.select("div.titleReviewBar").first();
            String reviews = titleReviewBar.select("div:containsOwn(Reviews)").first().nextElementSibling().child(0).child(0).text();

            result.addProperty("reviews", reviews);

        } catch (NullPointerException n) {

            result.addProperty("reviews", "");

        }

        // Critics

        try {

            Element titleReviewBar = html.select("div.titleReviewBar").first();
            String critics = titleReviewBar.select("div:containsOwn(Reviews)").first().nextElementSibling().child(0).child(2).text();

            result.addProperty("critics", critics);

        } catch (NullPointerException n) {

            result.addProperty("critics", "");

        }

        // return JsonObject

        return result;
    }

    public String getTargetURL(String target) throws IOException {

        String base_url = "https://www.imdb.com";
        String url = base_url + "/find?q=" + encodeAsURL(target) + "&s=tt&ttype=ft";

        Document html = Jsoup.connect(url).get();

        Element table = html.select("table").first();
        Element link = table.select("a").first();
        String relHref = link.attr("href");

        return base_url+relHref;
    }

    public String encodeAsURL(String text){

        try {

            return URLEncoder.encode(text, StandardCharsets.UTF_8.toString());

        } catch (UnsupportedEncodingException ex) {

            throw new RuntimeException(ex.getCause());
        }
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
