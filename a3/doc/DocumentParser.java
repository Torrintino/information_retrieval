package doc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.util.regex.*;

import doc.Document;


public class DocumentParser {

    String MOVIE_NAME_REGEX = "[^\"]+";
    String YEAR_REGEX = " \\([0-9\\?]{4}(\\/(X|V|I|L|C)+)?\\)";
    String MOVIE_REGEX = MOVIE_NAME_REGEX + YEAR_REGEX;
    String SERIES_REGEX = "\"" + MOVIE_NAME_REGEX + "\"" + YEAR_REGEX;
    String EPISODE_REGEX = SERIES_REGEX + " \\{[^\\}]+\\}";
    String TELEVISION_REGEX = MOVIE_REGEX + " \\(TV\\)";
    String VIDEO_REGEX = MOVIE_REGEX + " \\(V\\)";
    String VIDEOGAME_REGEX = MOVIE_REGEX + " \\(VG\\)";
    String DELIM_REGEX = "[\\ \\,\\.\\:\\!\\?]";

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
	while((line = reader.readLine()) != null) {
	    if(line.contains("--------------------------------------------------------------------------"))
		break;
	    if(line.length() < 4)
		continue;
	    if(!(line.substring(0, 4).equals("PL: ")))
		continue;
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

    public ArrayList<Document> build_doc_list(Path plotFile) {
	ArrayList<Document> doc_list = new ArrayList<>();

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
	return doc_list;
    }
    
}
