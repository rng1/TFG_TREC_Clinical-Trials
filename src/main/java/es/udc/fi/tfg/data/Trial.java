package es.udc.fi.tfg.data;

import static es.udc.fi.tfg.util.Parameters.INDEX_KEYWORDS;

import java.util.Collection;

public record Trial(String nctId, String criteria, String summary, String description, String gender, String minAge,
        String maxAge,
        Collection<String> keywords) {

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();

        if (criteria != null) {
            sb.append(criteria).append("\n");
        }

        if (summary != null) {
            sb.append(summary).append("\n");
        }

        if (description != null) {
            sb.append(description).append("\n");
        }

        if (keywords != null && INDEX_KEYWORDS) {
            sb.append(String.join(", ", keywords)).append("\n");
        }

        return sb.toString();
    }
}
