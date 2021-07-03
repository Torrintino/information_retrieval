import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.StringBuffer;
import java.util.*;

import doc.DocumentParser;
import doc.Collocation;

import static java.nio.charset.StandardCharsets.ISO_8859_1;


class ScoreComparator implements Comparator<Collocation> {

    public int compare(Collocation a, Collocation b) {
	if(a.score > b.score) {
	    return -1;
	} else if (a.score < b.score) {
	    return 1;
	} else {
	    return 0;
	}
    }

}


public class Bigrams {

    HashMap<String, Integer> collocation_count;
    HashMap<String, Integer> occurence_count;

    LinkedList<Collocation> collocation_rank;

    public Bigrams() {}

    public String concat(String s1, String s2) {
	return s1 + ":" + s2;
    }

    public void process_paragraph(List<String> stop_words, ArrayList<String> paragraph) {
	String w1, w2;
	
	for(int i=0; i<paragraph.size(); i++) {
	    w1 = paragraph.get(i);
	    if(stop_words.contains(w1))
		continue;
	    if(occurence_count.get(w1) == null) {
		occurence_count.put(w1, 1);
	    } else {
		occurence_count.put(w1, occurence_count.get(w1) + 1);
	    }

	    if(i < paragraph.size() - 1) {
		w2 = paragraph.get(i+1);
		if(stop_words.contains(w2))
		    continue;
		String word = concat(w1, w2);
		if(collocation_count.get(word) == null) {
		    collocation_count.put(word, 1);
		} else {
		    collocation_count.put(word, collocation_count.get(word) + 1);
		}
	    }
	}
    }

    public double compute_score(String word, String word1, String word2) {
	double f1 = 2*collocation_count.get(word);
	double f2 = occurence_count.get(word1) + occurence_count.get(word2);
	if(word.equals("life:world")) {
	    System.out.println("");
	    System.out.println("life:world - " + collocation_count.get(word));
	    System.out.println(word1 + ": " + occurence_count.get(word1));
	    System.out.println(word2 + ": " + occurence_count.get(word2));
	    System.out.println(word + " - f1: " + f1 + " f2: " + f2 + " f1/f2 " + f1/f2);
	    System.out.println("");
	}
	return f1/f2;
    }

    public void buildBigrams(Path plotFile) {
	List<String> stop_words = Arrays.asList("a","about","above","after", "again", "against",
						"all", "am", "an", "and", "any", "are", "aren't",
						"as", "at", "be", "because", "been", "before",
						"being", "below", "between", "both", "but", "by",
						"can't", "cannot", "could", "couldn't", "did",
						"didn't", "do", "does", "doesn't", "doing",
						"don't", "down", "during", "each", "few", "for",
						"from", "further", "had", "hadn't", "has",
						"hasn't", "have", "haven't", "having", "he",
						"he'd", "he'll", "he's", "her", "here", "here's",
						"hers", "herself", "him", "himself", "his",
						"how", "how's", "i", "i'd", "i'll", "i'm",
						"i've", "if", "in", "into", "is", "isn't", "it",
						"it's", "its", "itself", "let's", "me", "more",
						"most", "mustn't", "my", "myself", "no", "nor",
						"not", "of", "off", "on", "once", "only", "or",
						"other", "ought", "our", "ours 	ourselves",
						"out", "over", "own", "same", "shan't", "she",
						"she'd", "she'll", "she's", "should",
						"shouldn't", "so", "some", "such", "than",
						"that", "that's", "the", "their", "theirs",
						"them", "themselves", "then", "there", "there's",
						"these", "they", "they'd", "they'll", "they're",
						"they've", "this", "those", "through", "to",
						"too", "under", "until", "up", "very", "was",
						"wasn't", "we", "we'd", "we'll", "we're",
						"we've", "were", "weren't", "what", "what's",
						"when", "when's", "where", "where's", "which",
						"while", "who", "who's", "whom", "why", "why's",
						"with", "won't", "would", "wouldn't", "you",
						"you'd", "you'll", "you're", "you've", "your",
						"yours", "yourself", "yourselves");
	System.out.println("Start parsing...");
	DocumentParser parser = new DocumentParser();
	parser.build_doc_list(plotFile);
	
	System.out.println("Computing collocations...");
	collocation_count = new HashMap<>();
	occurence_count = new HashMap<>();
	for(var doc : parser.doc_list) {
	    if(doc.title.size() > 0)
		process_paragraph(stop_words, doc.title);
	    for(var plot : doc.plot)
		process_paragraph(stop_words, plot);
	}

	collocation_rank = new LinkedList<>();
	for(String word : collocation_count.keySet()) {
	    String[] original = word.split(":");
	    if(occurence_count.get(original[0]) < 1000 || occurence_count.get(original[0]) < 1000)
		continue;
	    double score = compute_score(word, original[0], original[1]);
	    collocation_rank.add(new Collocation(original[0], original[1], score));
	}
	Collections.sort(collocation_rank, new ScoreComparator());
	for(int i=0; i<100; i++) {
	    System.out.println(collocation_rank.get(i).word1 + " " + collocation_rank.get(i).word2
			       + " " + collocation_rank.get(i).score);
	}
	
    }

    public void close() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar BooleanQueryWordnet.jar <plot list file>");
            System.exit(-1);
        }

	Bigrams bg = new Bigrams();
        Path plotFile = Paths.get(args[0]);
        bg.buildBigrams(plotFile);
    }
}
