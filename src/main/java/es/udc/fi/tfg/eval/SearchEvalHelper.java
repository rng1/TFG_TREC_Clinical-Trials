package es.udc.fi.tfg.eval;

import static es.udc.fi.tfg.util.Parameters.BRANCH_NAME;
import static es.udc.fi.tfg.util.Parameters.DOCS_PATH;
import static es.udc.fi.tfg.util.Parameters.FILTER;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * Parses the relevance judgments file and returns a map with the relevance of each document for each topic.
     *
     * @return a map with the relevance of each document for each topic.
     */
    protected static Map<Integer, Map<String, Integer>> parseQrels() {

        final String qrelsPath = DOCS_PATH.concat("/qrels2022.txt");

        final Map<Integer, Map<String, Integer>> qrelsMap = new HashMap<>();

        try {
            final List<String> allLines = Files.readAllLines(Path.of(qrelsPath));

            for (final String line : allLines) {

                final String[] parts = line.split(" ");
                final int topicId = Integer.parseInt(parts[0]);
                final String docId = parts[2].toLowerCase();
                final int relevance = Integer.parseInt(parts[3]);

                final Map<String, Integer> innerMap = qrelsMap.computeIfAbsent(topicId, k -> new HashMap<>());
                innerMap.put(docId, relevance);

                qrelsMap.put(topicId, innerMap);
            }
        } catch (final IOException e) {
            logger.error("Error reading file - {}", e.getMessage());
        }

        return qrelsMap;
    }

    protected static String getMetricsFileName() {

        final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd'T'HHmm"));
        final String filePath = dateTime + "_" + BRANCH_NAME + "_" + (FILTER ? "filtered" : "unfiltered")
                + "_metrics.csv";

        return Paths.get(DOCS_PATH, "metrics", filePath).toString();
    }

    protected static String getGenderFilterValue(final String gender) {
        return switch (gender) {
        case "woman", "female", "girl" -> "female";
        case "man", "male", "boy" -> "male";
        default -> "";
        };
    }

}
