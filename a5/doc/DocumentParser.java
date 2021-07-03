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

    public ArrayList<Document> doc_list;

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
	String type;
	if (title_line.matches(EPISODE_REGEX)) {
	    type="episode";
	} else {
	    type="other";
	}

	Pattern year_pattern = Pattern.compile(YEAR_REGEX);
	Pattern episode_pattern = Pattern.compile("\\{[^\\}]+\\}");
	Matcher em = episode_pattern.matcher(title_line);

	if(type.equals("episode")) {
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
	ArrayList<String> plot = new ArrayList<>();

	boolean new_paragraph = true;
	while((line = reader.readLine()) != null) {
	    if(line.contains("--------------------------------------------------------------------------"))
		break;
	    if(line.length() < 4 || !(line.substring(0, 4).equals("PL: "))) {
		if(!new_paragraph) {
		    new_paragraph = true;
		    doc.plot.add(plot);
		    plot = new ArrayList<>();
		}
		continue;
	    }
	    ArrayList<String> tokens = parse_plotline(line);
	    plot.addAll(tokens);
	    new_paragraph = false;
	}

	if(!new_paragraph)
	    doc.plot.add(plot);
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
	int pos;
	if((pos = title_line.indexOf("{{SUSPENDED}}")) != -1)
	    title_line = title_line.substring(0, pos-1);

	if(parse_headline(doc, title_line) == null)
	    return null;
	reader.readLine();
	parse_plot(doc, reader);

	return doc;
    }

    public void build_doc_list(Path plotFile) {
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
    
}
