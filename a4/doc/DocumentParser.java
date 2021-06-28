package doc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
//import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.util.regex.*;


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
	boolean episode = false;
	if(title_line.matches(MOVIE_REGEX)) {
	    doc.add(new StringField("type", "movie", Field.Store.YES));
	} else if (title_line.matches(SERIES_REGEX)) {
	    doc.add(new StringField("type", "series", Field.Store.YES));
	} else if (title_line.matches(EPISODE_REGEX)) {
	    doc.add(new StringField("type", "episode", Field.Store.YES));
	    episode = true;
	} else if (title_line.matches(TELEVISION_REGEX)) {
	    doc.add(new StringField("type", "television", Field.Store.YES));
	} else if (title_line.matches(VIDEO_REGEX)) {
	    doc.add(new StringField("type", "video", Field.Store.YES));
	} else if (title_line.matches(VIDEOGAME_REGEX)) {
	    doc.add(new StringField("type", "videogame", Field.Store.YES));
	} else {
	    System.out.println("Unknown type: " + title_line);
	}

	Pattern year_pattern = Pattern.compile(YEAR_REGEX);
	Pattern episode_pattern = Pattern.compile("\\{[^\\}]+\\}");
	Matcher em = episode_pattern.matcher(title_line);

	if(episode) {
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
	doc.add(new TextField("title", title.toLowerCase(), Field.Store.YES));
	doc.add(new StringField("year", year.substring(2,6), Field.Store.YES));
	return doc;
    }

    private void parse_plot(Document doc, BufferedReader reader) throws IOException {
	String line;
	while((line = reader.readLine()) != null) {
	    if(line.contains("--------------------------------------------------------------------------"))
		break;
	    if(line.length() < 4)
		continue;
	    if(!(line.substring(0, 4).equals("PL: ")))
		continue;
	    line = line.substring(4);
	    doc.add(new TextField("title", line.toLowerCase(), Field.Store.YES));
	}
    }

    private Document parse_paragraph(BufferedReader reader) throws IOException {
	String line;
	do {
	    line = reader.readLine();
	    if(line == null)
		return null;
	} while(!line.contains("MV: "));

	Document doc = new Document();
	doc.add(new StringField("name", line, Field.Store.YES));

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

    public void build_doc_list(Path plotFile, IndexWriter writer) {

        try (BufferedReader reader = Files.newBufferedReader(plotFile, ISO_8859_1)) {
	    skip_initial_lines(reader);
	    Document doc;
	    int i = 0;
            while ((doc = parse_paragraph(reader)) != null) {
		doc.add(new StringField("id", Integer.toString(i), Field.Store.YES));
		writer.addDocument(doc);
		i++;
	    }
	} catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
}
