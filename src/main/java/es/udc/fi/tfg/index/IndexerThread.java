package es.udc.fi.tfg.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            for (final File trialXml : trials) {
                logger.info("Processing file '{}'", trialXml.getName());
                final Trial trial = parseXml(trialXml);
                indexTrial(trial, writer);
            }
        }

        logger.info("Folder '{}' processed in {} ms", file.getName(), System.currentTimeMillis() - start);

    }

    private Trial parseXml(final File file) {

        final XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            final XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(file));

            final List<String> keywords = new ArrayList<>();

            boolean isCriteria = false;
            String nctId = null;
            String gender = null;
            String minAge = null;
            String maxAge = null;
            String healthyVolunteers = null;
            String criteria = null;

            while (reader.hasNext()) {
                final int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    final String elementName = reader.getLocalName();

                    switch (elementName) {
                    case "nct_id" -> nctId = reader.getElementText().toLowerCase();
                    case "gender" -> gender = reader.getElementText().toLowerCase();
                    case "minimum_age" -> minAge = reader.getElementText().toLowerCase();
                    case "maximum_age" -> maxAge = reader.getElementText().toLowerCase();
                    case "healthy_volunteers" -> healthyVolunteers = reader.getElementText().toLowerCase();
                    case "keyword", "mesh_term" -> keywords.add(reader.getElementText().toLowerCase());
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

            return new Trial(nctId, criteria, gender, minAge, maxAge, healthyVolunteers, keywords);

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

        final String gender = trial.gender();
        final String minAge = trial.minAge();
        final String maxAge = trial.maxAge();

        doc.add(new KeywordField("nct_id", trial.nctId(), Field.Store.YES));
        doc.add(new StringField("gender", gender == null ? "all" : gender, Field.Store.YES));
        doc.add(new StringField("min_age", minAge == null ? "n/a" : minAge, Field.Store.YES));
        doc.add(new StringField("max_age", maxAge == null ? "n/a" : maxAge, Field.Store.YES));
        doc.add(new TextField("contents", trial.toString(), Field.Store.NO));

        return doc;
    }
}
