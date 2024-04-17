package es.udc.fi.tfg.data;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;

@Data
public class Topic {

    private final int id;
    private final String age;
    private final String gender;
    private final String description;

    public Topic(final String id, final String description) {
        this.id = Integer.parseInt(id);
        this.description = description;
        this.age = parseTopicAge(description);
        this.gender = parseTopicGender(description);
    }

    private String parseTopicAge(final String description) {
        final Pattern pattern = Pattern.compile("\\d+-year-old|\\d+-month-old|\\d+-week-old");
        final Matcher matcher = pattern.matcher(description);
        if (matcher.find()) {
            return matcher.group();
        }
        return "unknown";
    }

    private String parseTopicGender(final String description) {
        final String[] tokens = description.split(" ");
        final List<String> genderWords = Arrays.asList("male", "man", "boy", "female", "woman", "girl", "infant");
        for (final String token : tokens) {
            final String cleanToken = token.replaceAll("\\p{Punct}", "").toLowerCase();
            if (genderWords.contains(cleanToken)) {
                return cleanToken;
            }
        }
        return "unknown";
    }
}
