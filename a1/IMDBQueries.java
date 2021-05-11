import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class IMDBQueries {

    /**
     * A helper class for pairs of objects of generic types 'K' and 'V'.
     *
     * @param <K> first value
     * @param <V> second value
     */
    public static class Tuple<K, V> {
        K first;
        V second;

        public Tuple(K f, V s) {
            first = f;
            second = s;
        }

        @Override
        public int hashCode() {
            return first.hashCode() + second.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Tuple<?, ?>
                && first.equals(((Tuple<?, ?>) obj).first)
                && second.equals(((Tuple<?, ?>) obj).second);
        }
    }

    /**
     * All-rounder: Determine all movies in which the director stars as an actor
     * (cast). Return the top ten matches sorted by decreasing IMDB rating.
     *
     * @param movies the list of movies which is to be queried
     * @return top ten movies and the director, sorted by decreasing IMDB rating
     */
    protected List<Tuple<Movie, String>> queryAllRounder(List<Movie> movies) {

        LinkedList<Tuple<Movie, String>> resultList = new LinkedList<>();
        Comparator<Tuple<Movie, String>> comparator = Comparator.comparing(o -> o.first.getRatingValue());

        for (Movie movie : movies) {

            for (String director : movie.getDirectorList()) {

                if (movie.getCastList().contains(director)) {

                    resultList.add(new Tuple<>(movie, director));

                }

            }

        }

        resultList.sort(comparator);
        Collections.reverse(resultList);

        while (resultList.size() > 10) {

            resultList.removeLast();

        }

        return resultList;

    }

    /**
     * Under the Radar: Determine the top ten US-American movies until (including)
     * 2015 that have made the biggest loss despite an IMDB score above
     * (excluding) 8.0, based on at least 1,000 votes. Here, loss is defined as
     * gross minus budget.
     *
     * @param movies the list of movies which is to be queried
     * @return top ten highest rated US-American movie until 2015, sorted by
     * monetary loss, which is also returned
     */
    protected List<Tuple<Movie, Long>> queryUnderTheRadar(List<Movie> movies) {

        LinkedList<Tuple<Movie, Long>> resultList = new LinkedList<>();
        Comparator<Tuple<Movie, Long>> comparator = Comparator.comparing(o -> o.second);

        for (Movie movie : movies) {

            int year = Integer.parseInt(movie.getYear());
            double rating = Double.parseDouble(movie.getRatingValue());
            int ratingCount = Integer.parseInt(movie.getRatingCount().replaceAll("[^0-9]", ""));

            if (year <= 2015 && rating > 8.0 && ratingCount >= 1000) {

                // calculate loss
                Long gross = Long.parseLong(movie.getGross().replaceAll("[^0-9]", ""));
                Long budget = Long.parseLong(movie.getBudget().replaceAll("[^0-9]", ""));

                Long loss = -(gross-budget);

                resultList.add(new Tuple<>(movie, loss));

            }

        }

        resultList.sort(comparator);
        Collections.reverse(resultList);

        while (resultList.size() > 10) {

            resultList.removeLast();

        }

        return resultList;

    }

    /**
     * The Pillars of Storytelling: Determine all movies that contain both
     * (sub-)strings "kill" and "love" in their lowercase description
     * (String.toLowerCase()). Sort the results by the number of appearances of
     * these strings and return the top ten matches.
     *
     * @param movies the list of movies which is to be queried
     * @return top ten movies, which have the words "kill" and "love" as part of
     * their lowercase description, sorted by the number of appearances of
     * these words, which is also returned.
     */
    protected List<Tuple<Movie, Integer>> queryPillarsOfStorytelling(List<Movie> movies) {

        LinkedList<Tuple<Movie, Integer>> resultList = new LinkedList<>();

        for (Movie movie : movies) {

            String description = movie.getDescription().toLowerCase();

            String kill = "kill", love = "love";
            int kill_count = 0, love_count = 0, index = 0;

            while ((index = description.indexOf(kill, index)) != -1 ){

                kill_count++;
                index++;

            }

            index = 0;

            while ((index = description.indexOf(love, index)) != -1 ){

                love_count++;
                index++;

            }

            if (kill_count > 0 && love_count > 0) {

                int word_count = kill_count+love_count;

                // First entry
                if (resultList.isEmpty()) {

                    resultList.add(new Tuple<>(movie, word_count));
                    continue;

                }

                // Latter entries
                int i = 0;

                while (i < resultList.size()) {

                    int list_word_count = resultList.get(i).second;

                    if (word_count > list_word_count) {

                        resultList.add(i, new Tuple<>(movie, word_count));
                        break;

                    }

                    i++;

                    if (i == resultList.size()) {

                        resultList.add(i, new Tuple<>(movie, word_count));
                        break;

                    }

                }

                if (resultList.size() > 10) {

                    resultList.removeLast();

                }

            }

        }

        return resultList;

    }

    /**
     * The Red Planet: Determine all movies of the Sci-Fi genre that mention
     * "Mars" in their description (case-aware!). List all found movies in
     * ascending order of publication (year).
     *
     * @param movies the list of movies which is to be queried
     * @return list of Sci-Fi movies involving Mars in ascending order of
     * publication.
     */
    protected List<Movie> queryRedPlanet(List<Movie> movies) {

        LinkedList<Movie> resultList = new LinkedList<>();
        Comparator<Movie> comparator = Comparator.comparing(o -> Integer.parseInt(o.getYear()));

        for (Movie movie : movies) {

            String description = movie.getDescription();

	    // if (movie.getGenreList().contains("Sci-Fi")) {
	    // 	int index = 0;
	    // 	while ((index = description.indexOf("Mars", index)) != -1 ) {
	    // 	    switch(description.charAt(index+4)) {
	    // 	    case '!':
	    // 	    case '.':
	    // 	    case ' ':
	    // 	    case '?':
	    // 		resultList.add(movie);
	    // 		break;
	    // 	    default:
	    // 		break;
	    // 	    }
	    // 	    index++;
	    // 	}
	    // }
            if (movie.getGenreList().contains("Sci-Fi") && (description.contains("Mars.")
                    || description.contains("Mars,") || description.contains("Mars ")
                    || description.contains("Mars?") || description.contains("Mars!"))) {

                resultList.add(movie);

            }

        }

        resultList.sort(comparator);

        return resultList;

    }

    /**
     * Colossal Failure: Determine all US-American movies with a duration beyond 2
     * hours, a budget beyond 1 million and an IMDB rating below 5.0. Sort results
     * by ascending IMDB rating.
     *
     * @param movies the list of movies which is to be queried
     * @return list of US-American movies with high duration, large budgets and a
     * bad IMDB rating, sorted by ascending IMDB rating
     */
    protected List<Movie> queryColossalFailure(List<Movie> movies) {

        LinkedList<Movie> resultList = new LinkedList<>();
        Comparator<Movie> comparator = Comparator.comparing(o -> Double.parseDouble(o.getRatingValue()));

        for (Movie movie : movies) {

            if (movie.getDuration().equals("")) {

                continue;

            }

            int h=0, min=0;

            if (movie.getDuration().contains("min")) {

                min = Integer.parseInt(movie.getDuration().split("h")[1].split("m")[0].trim());

            }

            h = Integer.parseInt(movie.getDuration().split("h")[0]);

            int duration = 60*h + min;
            Long budget = Long.parseLong(movie.getBudget().replaceAll("[^0-9]", ""));
            double rating = Double.parseDouble(movie.getRatingValue());

            if (duration > 120 && budget > 1000000 && rating < 5.0) {

                resultList.add(movie);

            }

        }

        resultList.sort(comparator);

        return resultList;

    }

    /**
     * Uncreative Writers: Determine the 10 most frequent character names of all
     * times ordered by frequency of occurrence. Filter any lowercase names
     * containing substrings "himself", "doctor", and "herself" from the result.
     *
     * @param movies the list of movies which is to be queried
     * @return the top 10 character names and their frequency of occurrence;
     * sorted in decreasing order of frequency
     */
    protected List<Tuple<String, Integer>> queryUncreativeWriters(List<Movie> movies) {

        LinkedList<Tuple<String, Integer>> resultList = new LinkedList<>();
        Comparator<Tuple<String, Integer>> comparator = Comparator.comparing(o -> o.second);

        for (Movie movie : movies) {

            for (String character : movie.getCharacterList()) {

                if (character.contains("himself") || character.contains("herself") || character.contains("doctor")) {

                    continue;

                }

                // First entry
                if (resultList.isEmpty()) {

                    resultList.add(new Tuple<>(character, 1));
                    continue;

                }

                // Latter entries
                boolean in_list = false;

                for (Tuple<String, Integer> element : resultList) {

                    if (element.first.equals(character)) {

                        element.second++;
                        in_list = true;
                        break;

                    }

                }

                if (!in_list) {

                    resultList.add(new Tuple<>(character, 1));

                }

            }

        }

        resultList.sort(comparator);
        Collections.reverse(resultList);

        while (resultList.size() > 10) {

            resultList.removeLast();

        }

        return resultList;
    }

    /**
     * Workhorse: Provide a ranked list of the top ten most active actors (i.e.
     * starred in most movies) and the number of movies they played a role in.
     *
     * @param movies the list of movies which is to be queried
     * @return the top ten actors and the number of movies they had a role in,
     * sorted by the latter.
     */
    protected List<Tuple<String, Integer>> queryWorkHorse(List<Movie> movies) {

        LinkedList<Tuple<String, Integer>> resultList = new LinkedList<>();
        Comparator<Tuple<String, Integer>> comparator = Comparator.comparing(o -> o.second);

        for (Movie movie : movies) {

            for (String actor : movie.getCastList()) {

                // First entry
                if (resultList.isEmpty()) {

                    resultList.add(new Tuple<>(actor, 1));
                    continue;

                }

                // Latter entries
                boolean in_list = false;

                for (Tuple<String, Integer> element : resultList) {

                    if (element.first.equals(actor)) {

                        element.second++;
                        in_list = true;
                        break;

                    }

                }

                if (!in_list) {

                    resultList.add(new Tuple<>(actor, 1));

                }

            }

        }

        resultList.sort(comparator);
        Collections.reverse(resultList);

        while (resultList.size() > 10) {

            resultList.removeLast();

        }

        return resultList;

    }

    /**
     * Must See: List the best-rated movie of each year starting from 1990 until
     * (including) 2010 with more than 10,000 ratings. Order the movies by
     * ascending year.
     *
     * @param movies the list of movies which is to be queried
     * @return best movies by year, starting from 1990 until 2010.
     */
    protected List<Movie> queryMustSee(List<Movie> movies) {

        LinkedList<Movie> resultList = new LinkedList<>();
        Movie dummy = new Movie();
        dummy.setRatingValue("0.0");

        for (int i=0; i < 21; i++) {

            resultList.add(dummy);

        }

        for (Movie movie : movies) {

            int year = Integer.parseInt(movie.getYear());
            int year_diff = year-1990;

            int ratingCount = Integer.parseInt(movie.getRatingCount().replaceAll("[^0-9]", ""));
            double rating = Double.parseDouble(movie.getRatingValue());

            if(year_diff >= 0 && year_diff <= 20 && ratingCount > 10000) {

                double rating_list = Double.parseDouble(resultList.get(year_diff).getRatingValue());

                if (rating > rating_list) {

                    resultList.set(year_diff, movie);

                }

            }

        }

        return resultList;

    }

    /**
     * Rotten Tomatoes: List the worst-rated movie of each year starting from 1990
     * till (including) 2010 with an IMDB score larger than 0. Order the movies by
     * increasing year.
     *
     * @param movies the list of movies which is to be queried
     * @return worst movies by year, starting from 1990 till (including) 2010.
     */
    protected List<Movie> queryRottenTomatoes(List<Movie> movies) {

        LinkedList<Movie> resultList = new LinkedList<>();
        Movie dummy = new Movie();
        dummy.setRatingValue("10.0");

        for (int i=0; i < 21; i++) {

            resultList.add(dummy);

        }

        for (Movie movie : movies) {

            int year = Integer.parseInt(movie.getYear());
            int year_diff = year-1990;

            double rating = Double.parseDouble(movie.getRatingValue());

            if(year_diff >= 0 && year_diff <= 20 && rating > 0) {

                double rating_list = Double.parseDouble(resultList.get(year_diff).getRatingValue());

                if (rating < rating_list) {

                    resultList.set(year_diff, movie);

                }

            }

        }

        return resultList;

    }

    protected String pairToSingle(String actor1, String actor2) {

        if(actor1.compareTo(actor2) < 0) {

            return actor1 + ":" + actor2;

        } else {

            return actor2 + ":" + actor1;

        }
    }


    /**
     * Magic Couples: Determine those couples that feature together in the most
     * movies. E.g., Adam Sandler and Allen Covert feature together in multiple
     * movies. Report the top ten pairs of actors, their number of movies and sort
     * the result by the number of movies.
     *
     * @param movies the list of movies which is to be queried
     * @return report the top 10 pairs of actors and the number of movies they
     * feature together. Sort by number of movies.
     */
    protected List<Tuple<Tuple<String, String>, Integer>> queryMagicCouple(List<Movie> movies) {

        ArrayList<Tuple<Tuple<String, String>, Integer>> result = new ArrayList<>();
        HashMap<String, Integer> store = new HashMap<>();

        for(var movie : movies) {

            var cast_list = movie.getCastList();

            for(int i=0; i<cast_list.size()-1; i++) {

                String actor1 = cast_list.get(i);

                for(int j=i+1; j<cast_list.size(); j++) {

                    String actor2 = cast_list.get(j);
                    String name = pairToSingle(actor1, actor2);

                    if(! store.containsKey(name)) {

                        Tuple<Tuple<String, String>, Integer> entry = new Tuple<>(new Tuple<>(actor1, actor2), 1);
                        store.put(name, result.size());
                        result.add(entry);

                    } else {

                        var index = store.get(name);
                        var entry = result.get(index);
                        entry.second++;

                        while(index > 0) {

                            var temp = result.get(index-1);

                            if(temp.second < entry.second) {

                                result.set(index - 1, entry);
                                result.set(index, temp);
                                String old_name = pairToSingle(temp.first.first, temp.first.second);
                                store.put(old_name, index);

                            } else {

                                break;

                            }
                            index--;
                            store.put(name, index);
                        }
                    }
                }
            }
        }

        return result.subList(0, 10);

    }



    public static void main(String[] argv) throws IOException {
        String moviesPath = "./data/movies/";

        if (argv.length == 1) {
            moviesPath = argv[0];
        } else if (argv.length != 0) {
            System.out.println("Call with: IMDBQueries.jar <moviesPath>");
            System.exit(0);
        }

        MovieReader movieReader = new MovieReader();
        List<Movie> movies = movieReader.readMoviesFrom(Paths.get(moviesPath));

        System.out.println("All-rounder");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, String>> result = queries.queryAllRounder(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() == 10) {
                for (Tuple<Movie, String> tuple : result) {
                    System.out.println("\t" + tuple.first.getRatingValue() + "\t"
                        + tuple.first.getTitle() + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Under the radar");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, Long>> result = queries.queryUnderTheRadar(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<Movie, Long> tuple : result) {
                    System.out.println("\t" + tuple.first.getTitle() + "\t"
                        + tuple.first.getRatingCount() + "\t"
                        + tuple.first.getRatingValue() + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("The pillars of storytelling");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, Integer>> result = queries
                .queryPillarsOfStorytelling(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<Movie, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first.getTitle() + "\t"
                        + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("The red planet");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryRedPlanet(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie movie : result) {
                    System.out.println("\t" + movie.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("ColossalFailure");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryColossalFailure(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie movie : result) {
                    System.out.println("\t" + movie.getTitle() + "\t"
                        + movie.getRatingValue());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Uncreative writers");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<String, Integer>> result = queries
                .queryUncreativeWriters(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<String, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Workhorse");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<String, Integer>> result = queries.queryWorkHorse(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<String, Integer> actor : result) {
                    System.out.println("\t" + actor.first + "\t" + actor.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Must see");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryMustSee(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie m : result) {
                    System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                        + "\t" + m.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Rotten tomatoes");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryRottenTomatoes(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie m : result) {
                    System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                        + "\t" + m.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Magic Couples");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Tuple<String, String>, Integer>> result = queries
                .queryMagicCouple(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<Tuple<String, String>, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first.first + ":"
                        + tuple.first.second + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
            System.out.println("");
        }
    }
}
