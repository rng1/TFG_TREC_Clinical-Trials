package es.udc.fi.tfg.data;

import lombok.Data;

@Data
public class Qrel {

    private final String topicId;
    private final String docId;
    private final String relevance;
}
