package es.udc.fi.tfg.data;

import static es.udc.fi.tfg.util.Parameters.INDEX_KEYWORDS;

import java.util.Collection;

public record Trial(String nctId, String[] criteria, String gender, String minAge, String maxAge,
        String healthyVolunteers, Collection<String> keywords, Collection<String> conditions, String briefTitle,
        String officialTitle, String summary, String description) {

    public String getInclusionCriteria() {
        if (criteria == null) {
            return "";
        }
        return criteria[0];
    }

    public String getExclusionCriteria() {
        if (criteria == null) {
            return "";
        }
        return criteria[1];
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();

        if (briefTitle != null) {
            sb.append("brief title: ").append(briefTitle).append("\n");
        }

        if (officialTitle != null && !officialTitle.equals(briefTitle)) {
            sb.append("official title: ").append(officialTitle).append("\n");
        }

        if (summary != null) {
            sb.append("summary: ").append(summary).append("\n");
        }

        if (description != null) {
            sb.append("description: ").append(description).append("\n");
        }

        if (gender != null) {
            sb.append("gender: ").append(gender).append("\n");
        }

        if (minAge != null) {
            sb.append("minimum age: ").append(minAge).append("\n");
        }

        if (maxAge != null) {
            sb.append("maximum age: ").append(maxAge).append("\n");
        }

        if (healthyVolunteers != null) {
            sb.append("admits healthy volunteers: ").append(healthyVolunteers).append("\n");
        }

        if (keywords != null && INDEX_KEYWORDS) {
            sb.append("keywords: ").append(String.join(", ", keywords)).append("\n");
        }

        if (conditions != null && INDEX_KEYWORDS) {
            sb.append("conditions: ").append(String.join(", ", conditions)).append("\n");
        }

        return sb.toString();
    }
}
