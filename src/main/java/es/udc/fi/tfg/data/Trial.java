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
}
