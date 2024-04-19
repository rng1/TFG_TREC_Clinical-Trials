package es.udc.fi.tfg.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {

    public static double normalizeAge(final String age) {

        if (age != null) {

            final Pattern pattern = Pattern.compile("(\\d+)\\s+(year|month|week)s?");
            final Matcher matcher = pattern.matcher(age);

            if (matcher.find()) {
                final double value = Double.parseDouble(matcher.group(1));
                final String unit = matcher.group(2);

                return switch (unit) {
                case "year" -> value;
                case "month" -> value / 12; // Considering 1 year = 12 months
                case "week" -> value / 52; // Considering 1 year = 52 weeks
                default -> -1;
                };
            }
        }

        return -1;
    }
}
