package es.udc.fi.tfg.data;

import java.util.Collection;

public record Trial(String nctId, String criteria, String gender, String minAge, String maxAge,
        String healthyVolunteers, Collection<String> keywords) {

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();

        if (criteria != null) {
            sb.append(criteria).append("\n");
        }

        sb.append("gender: ").append(gender).append("\n");

        if (minAge != null) {
            sb.append("minimum age: ").append(minAge).append("\n");
        }

        if (maxAge != null) {
            sb.append("maximum age: ").append(maxAge).append("\n");
        }

        if (healthyVolunteers != null) {
            sb.append("admits healthy volunteers: ").append(healthyVolunteers).append("\n");
        }

        if (keywords != null) {
            sb.append("keywords: ").append(String.join(", ", keywords)).append("\n");
        }

        return sb.toString();
    }
}
