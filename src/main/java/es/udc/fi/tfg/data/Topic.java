package es.udc.fi.tfg.data;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.Data;

@Data
public class Topic {

    private final Pattern AGE_PATTERN = Pattern.compile("\\d+-year-old|\\d+-month-old|\\d+-week-old");
    private final List<String> GENDER_WORDS = Arrays.asList("male", "man", "boy", "female", "woman", "girl", "infant");

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

    /**
     * Parse the age of the patient from the description of the topic. Matches the pattern of "X-year-old",
     * "X-month-old" or "X-week-old".
     *
     * @param description
     *            description of the topic.
     * @return the age of the patient or "unknown" if not found.
     */
    private String parseTopicAge(final String description) {
        final Matcher matcher = AGE_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group();
        }
        return "unknown";
    }

    /**
     * Parse the gender of the patient from the description of the topic. Matches the patterns of "male", "man", "boy",
     * "female", "woman", "girl" or "infant".
     *
     * @param description
     *            description of the topic.
     * @return the gender of the patient or "unknown" if not found.
     */
    private String parseTopicGender(final String description) {
        return Stream.of(description.split(" "))
                .map(token -> token.replaceAll("\\p{Punct}", "").toLowerCase())
                .filter(GENDER_WORDS::contains)
                .findFirst()
                .orElse("unknown");
    }
}
