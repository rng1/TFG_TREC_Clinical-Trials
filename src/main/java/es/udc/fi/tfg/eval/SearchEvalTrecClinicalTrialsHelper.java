package es.udc.fi.tfg.eval;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.search.similarities.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.udc.fi.tfg.data.Topic;
import es.udc.fi.tfg.util.Utility;

public class SearchEvalTrecClinicalTrialsHelper {

    private static final Logger logger = LoggerFactory.getLogger(SearchEvalTrecClinicalTrialsHelper.class);

    protected static SearchEvalParams parseSearchInputParams(final String[] params) {

        final Options options = new Options();

        options.addOption("model", true, "the model to be used");
        options.addOption("index", true, "path to the index");
        options.addOption("docs", true, "path to the documents to be parsed");
        options.addOption("cut", true, "cut in ranking for metric computation");

        try {
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, params);

            final Similarity model = Utility.parseModel(cmd.getOptionValue("model"));
            final String indexPath = cmd.getOptionValue("index");
            final String docsPath = cmd.getOptionValue("docs");
            final int cut = Integer.parseInt(cmd.getOptionValue("cut"));

            return new SearchEvalParams(model, indexPath, docsPath, cut);
        } catch (final ParseException e) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("WebIndexer", options, true);
            throw new RuntimeException("Error parsing command line options", e);
        }
    }

    protected static Set<Topic> parseTopics(final String dataPath) {

        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final String topicsPath = dataPath.concat("/topics2022.xml");

        final Set<Topic> topics = new HashSet<>();

        try {
            final XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(topicsPath));

            while (reader.hasNext()) {
                // Get the next event.
                final int event = reader.next();

                // If the event is the start of an element.
                if (event == XMLStreamConstants.START_ELEMENT) {
                    final String elementName = reader.getLocalName();
                    // Get the name of the element.
                    if (elementName.equals("topic")) {
                        final String id = reader.getAttributeValue(null, "number");
                        final String description = reader.getElementText().toLowerCase();

                        topics.add(new Topic(id, description));
                    }
                }
            }
        } catch (final XMLStreamException e) {
            logger.error("Error reading XML file - {}", e.getMessage());
        } catch (final FileNotFoundException e) {
            logger.error("File not found - {}", e.getMessage());
        }

        return topics;
    }

    protected static Map<Integer, Map<String, Integer>> parseQrels(final String dataPath) {

        final String qrelsPath = dataPath.concat("/qrels2022.txt");

        final Map<Integer, Map<String, Integer>> qrelsMap = new HashMap<>();

        try {
            final List<String> allLines = Files.readAllLines(Path.of(qrelsPath));

            for (final String line : allLines) {

                final String[] parts = line.split(" ");
                final int topicId = Integer.parseInt(parts[0]);
                final String docId = parts[2].toLowerCase();
                final int relevance = Integer.parseInt(parts[3]);

                final Map<String, Integer> innerMap = qrelsMap.getOrDefault(topicId, new HashMap<>());
                innerMap.put(docId, relevance);

                qrelsMap.put(topicId, innerMap);
            }
        } catch (final IOException e) {
            logger.error("Error reading file - {}", e.getMessage());
        }

        return qrelsMap;
    }

}
