package es.udc.fi.tfg.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.udc.fi.tfg.data.Trial;

public class IndexerThread implements Runnable {

    private final File file;
    private final IndexWriter writer;

    private final Logger logger = LoggerFactory.getLogger(IndexerThread.class);

    public IndexerThread(final File file, final IndexWriter writer) {
        this.file = file;
        this.writer = writer;
    }

    @Override
    public void run() {

        final long start = System.currentTimeMillis();

        logger.info("Processing folder '{}'", file.getName());

        final File[] trials = file.listFiles();

        if (trials != null) {
            for (final File trial : trials) {
                logger.info("Processing file '{}'", trial.getName());
                final Trial trialPojo = parseXml(trial);
                indexTrial(trialPojo, writer);
            }
        }

        logger.info("Folder '{}' processed in {} ms", file.getName(), System.currentTimeMillis() - start);

    }

    private Trial parseXml(final File file) {

        final XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            final XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(file));

            boolean isCriteria = false;

            String nctId = null;
            String gender = null;
            String minAge = null;
            String maxAge = null;
            String healthyVolunteers = null;
            String criteria = null;

            while (reader.hasNext()) {
                // Get the next event.
                final int event = reader.next();

                // If the event is the start of an element.
                if (event == XMLStreamConstants.START_ELEMENT) {
                    final String elementName = reader.getLocalName();
                    // Get the name of the element.
                    switch (elementName) {
                    case "nct_id" -> nctId = reader.getElementText().toLowerCase();
                    case "gender" -> gender = reader.getElementText().toLowerCase();
                    case "minimum_age" -> minAge = reader.getElementText().toLowerCase();
                    case "maximum_age" -> maxAge = reader.getElementText().toLowerCase();
                    case "healthy_volunteers" -> healthyVolunteers = reader.getElementText().toLowerCase();
                    case "criteria" -> isCriteria = true;
                    case "textblock" -> {
                        if (isCriteria)
                            criteria = reader.getElementText().toLowerCase();
                    }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (reader.getLocalName().equals("criteria"))
                        isCriteria = false;
                }
            }

            return new Trial(nctId, criteria, gender, minAge, maxAge, healthyVolunteers);

        } catch (final XMLStreamException e) {
            logger.error("Error reading XML file - {}", e.getMessage());
        } catch (final FileNotFoundException e) {
            logger.error("File not found - {}", e.getMessage());
        }

        return null;
    }

    private void indexTrial(final Trial trial, final IndexWriter writer) {

        if (trial != null) {
            final Document doc = createDocument(trial);

            try {
                writer.addDocument(doc);
            } catch (final IOException e) {
                logger.error("Error indexing trial - {}", e.getMessage());
            }
        } else {
            logger.error("Error indexing trial - trial is null");
        }
    }

    private Document createDocument(final Trial trial) {

        final Document doc = new Document();

        // TODO: revisar campos
        doc.add(new KeywordField("nct_id", trial.getNctId(), Field.Store.YES));
        doc.add(new TextField("criteria", emptyIfNull(trial.getCriteria()), Field.Store.NO));
        doc.add(new StringField("gender", emptyIfNull(trial.getGender()), Field.Store.NO));
        doc.add(new StringField("min_age", emptyIfNull(trial.getMinAge()), Field.Store.NO));
        doc.add(new StringField("max_age", emptyIfNull(trial.getMaxAge()), Field.Store.NO));
        doc.add(new StringField("healthy_volunteers", emptyIfNull(trial.getHealthyVolunteers()), Field.Store.NO));
        doc.add(new TextField("contents", trial.toString(), Field.Store.YES));

        return doc;
    }

    private String emptyIfNull(final String str) {
        return str == null ? "" : str;
    }
}
