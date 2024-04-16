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
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.udc.fi.tfg.data.Trial;

public class IndexerThread implements Runnable {

    private final File file;
    private final IndexWriter writer;

    final Logger logger = LoggerFactory.getLogger(IndexerThread.class);

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
                    case "nct_id" -> nctId = reader.getElementText();
                    case "gender" -> gender = reader.getElementText();
                    case "minimum_age" -> minAge = reader.getElementText();
                    case "maximum_age" -> maxAge = reader.getElementText();
                    case "healthy_volunteers" -> healthyVolunteers = reader.getElementText();
                    case "criteria" -> isCriteria = true;
                    case "textblock" -> {
                        if (isCriteria)
                            criteria = reader.getElementText();
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
            logger.info("Indexing trial '{}'", trial.getNctId());
            final Document doc = createDocument(trial);

            try {
                writer.addDocument(doc);
            } catch (final IOException e) {
                logger.error("Error indexing trial - {}", e.getMessage());
            }

            logger.info("Trial '{}' indexed", trial.getNctId());

        } else {
            logger.error("Error indexing trial - trial is null");
        }
    }

    private Document createDocument(final Trial trial) {

        final Document doc = new Document();

        final FieldType criteriaField = new FieldType();
        criteriaField.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        criteriaField.setStoreTermVectors(true);
        criteriaField.setStored(true);

        doc.add(new KeywordField("nct_id", trial.getNctId(), Field.Store.YES));
        doc.add(new Field("criteria", trial.getCriteria(), criteriaField));
        doc.add(new StringField("gender", trial.getGender(), Field.Store.YES));
        doc.add(new StringField("min_age", trial.getMinAge(), Field.Store.YES));
        doc.add(new StringField("max_age", trial.getMaxAge(), Field.Store.YES));
        doc.add(new StringField("healthy_volunteers", trial.getHealthyVolunteers(), Field.Store.YES));

        return doc;
    }
}
