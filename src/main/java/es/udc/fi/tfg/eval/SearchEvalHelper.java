package es.udc.fi.tfg.eval;

import static es.udc.fi.tfg.util.Parameters.DOCS_PATH;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.udc.fi.tfg.data.Topic;

public class SearchEvalHelper {

    private static final Logger logger = LoggerFactory.getLogger(SearchEvalHelper.class);

    /**
     * Parses the topics file and returns a set with the topics.
     *
     * @return a set with the topics.
     */
    protected static Set<Topic> parseTopics() {

        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final String topicsPath = DOCS_PATH.concat("/topics2022.xml");

        final Set<Topic> topics = new HashSet<>();

        try (final FileInputStream fis = new FileInputStream(topicsPath)) {
            final XMLStreamReader reader = factory.createXMLStreamReader(fis);

            while (reader.hasNext()) {
                final int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT && "topic".equals(reader.getLocalName())) {
                    final String id = reader.getAttributeValue(null, "number");
                    final String description = reader.getElementText().toLowerCase();

                    topics.add(new Topic(id, description));
                }
            }
        } catch (final XMLStreamException | IOException e) {
            logger.error("Error reading XML file - {}", e.getMessage());
        }

        return topics;
    }

    protected static String getGenderFilterValue(final String gender) {
        return switch (gender) {
        case "woman", "female", "girl" -> "female";
        case "man", "male", "boy" -> "male";
        default -> "";
        };
    }

}
