package es.udc.fi.tfg.data;

import lombok.Data;

@Data
public final class Trial {

    private final String nctId;
    private final String criteria;
    private final String gender;
    private final String minAge;
    private final String maxAge;
    private final String healthyVolunteers;

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

        return sb.toString();
    }
}
