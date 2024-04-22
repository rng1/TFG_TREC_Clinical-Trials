package es.udc.fi.tfg.index;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexerHelper {

    protected static String[] parseCriteria(final String text) {
        final String inclusionRegex = "(?i)Inclusion criteria(.*?)(?=Exclusion criteria|$)";
        final String exclusionRegex = "(?i)Exclusion criteria(.*?)(?=Inclusion criteria|$)";

        final String inclusionCriteria = extractCriteria(text, inclusionRegex);
        final String exclusionCriteria = extractCriteria(text, exclusionRegex);

        if (inclusionCriteria.isEmpty() && exclusionCriteria.isEmpty()) {
            return new String[] { text, text };
        } else {
            return new String[] { inclusionCriteria, exclusionCriteria };
        }
    }

    private static String extractCriteria(final String text, final String regex) {
        final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            return "";
        }
    }
}
