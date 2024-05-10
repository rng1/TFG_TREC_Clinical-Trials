package es.udc.fi.tfg.index;

import static es.udc.fi.tfg.index.IndexerHelper.parseCriteria;
import static es.udc.fi.tfg.util.Utility.normalizeAge;

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
import org.apache.lucene.document.DoubleRange;
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
    private final IndexWriter writerIn;
    private final IndexWriter writerEx;
    private final IndexWriter writerMain;

    private final Logger logger = LoggerFactory.getLogger(IndexerThread.class);

    public IndexerThread(final File file, final IndexWriter writerIn, final IndexWriter writerEx,
            final IndexWriter writerMain) {
        this.file = file;
        this.writerIn = writerIn;
        this.writerEx = writerEx;
        this.writerMain = writerMain;
    }

    @Override
    public void run() {

        final long start = System.currentTimeMillis();

        logger.info("Processing folder '{}'", file.getName());

        final File[] trials = file.listFiles();

        if (trials != null) {
            for (final File trialXml : trials) {
                // logger.info("Processing file '{}'", trialXml.getName());
                final Trial trial = parseXml(trialXml);
                indexTrial(trial, writerIn, writerEx, writerMain);
            }
        }

        logger.info("Folder '{}' processed in {} ms", file.getName(), System.currentTimeMillis() - start);

    }

    /**
     * Parse a trial from an XML file.
     *
     * @param file
     *            the XML file.
     * @return the trial.
     */
    private Trial parseXml(final File file) {

        final XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            final XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(file));

            final List<String> keywords = new ArrayList<>();
            final List<String> conditions = new ArrayList<>();

            boolean isCriteria = false;
            boolean isSummary = false;
            boolean isDescription = false;

            String nctId = null;
            String gender = null;
            String minAge = null;
            String maxAge = null;
            String summary = null;
            String description = null;
            String briefTitle = null;
            String officialTitle = null;
            String[] criteria = null;

            while (reader.hasNext()) {
                final int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    final String elementName = reader.getLocalName();

                    switch (elementName) {
                    case "nct_id" -> nctId = reader.getElementText().toLowerCase();
                    case "gender" -> gender = reader.getElementText().toLowerCase();
                    case "minimum_age" -> minAge = reader.getElementText().toLowerCase();
                    case "maximum_age" -> maxAge = reader.getElementText().toLowerCase();
                    case "keyword", "mesh_term" -> keywords.add(reader.getElementText().toLowerCase());
                    case "condition" -> conditions.add(reader.getElementText().toLowerCase());
                    case "brief_title" -> briefTitle = reader.getElementText().toLowerCase();
                    case "official_title" -> officialTitle = reader.getElementText().toLowerCase();
                    case "brief_summary" -> isSummary = true;
                    case "detailed_description" -> isDescription = true;
                    case "criteria" -> isCriteria = true;
                    case "textblock" -> {
                        if (isCriteria)
                            criteria = parseCriteria(reader.getElementText().toLowerCase());
                        if (isSummary)
                            summary = reader.getElementText().toLowerCase();
                        if (isDescription)
                            description = reader.getElementText().toLowerCase();
                    }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (reader.getLocalName().equals("criteria"))
                        isCriteria = false;
                    if (reader.getLocalName().equals("brief_summary"))
                        isSummary = false;
                    if (reader.getLocalName().equals("detailed_description"))
                        isDescription = false;
                }
            }

            return new Trial(nctId, criteria, gender, minAge, maxAge, keywords, conditions, briefTitle, officialTitle,
                    summary, description);

        } catch (final XMLStreamException e) {
            logger.error("Error reading XML file - {}", e.getMessage());
        } catch (final FileNotFoundException e) {
            logger.error("File not found - {}", e.getMessage());
        }

        return null;
    }

    /**
     * Index a trial in the Lucene index.
     *
     * @param trial
     *            the trial to index.
     * @param writerIn
     *            the inclusion criteria index writer.
     * @param writerEx
     *            the exclusion criteria index writer.
     * @param writerMain
     *            the main index writer.
     */
    private void indexTrial(final Trial trial, final IndexWriter writerIn, final IndexWriter writerEx,
            final IndexWriter writerMain) {

        if (trial != null) {
            final Document docMain = createDocument(trial, "main");
            final Document docIn = createDocument(trial, "in");
            final Document docEx = createDocument(trial, "ex");

            try {
                writerMain.addDocument(docMain);
                writerIn.addDocument(docIn);
                writerEx.addDocument(docEx);
            } catch (final IOException e) {
                logger.error("Error indexing trial - {}", e.getMessage());
            }
        } else {
            logger.error("Error indexing trial - trial is null");
        }
    }

    /**
     * Create a Lucene document from a trial.
     *
     * @param trial
     *            the trial to index.
     * @param type
     *            the index where the document belongs (main, inclusion, exclusion).
     * @return the Lucene document.
     */
    private Document createDocument(final Trial trial, final String type) {

        final Document doc = new Document();

        final String gender = trial.gender();
        final String minAge = trial.minAge();
        final String maxAge = trial.maxAge();
        final double minAgeNorm = normalizeAge(minAge);
        final double maxAgeNorm = normalizeAge(maxAge);
        final double[] minAgeRange = new double[] { minAgeNorm == -1 ? Double.NEGATIVE_INFINITY : minAgeNorm };
        final double[] maxAgeRange = new double[] { maxAgeNorm == -1 ? Double.POSITIVE_INFINITY : maxAgeNorm };

        doc.add(new KeywordField("nct_id", trial.nctId(), Field.Store.YES));
        doc.add(new StringField("gender", gender == null ? "all" : gender, Field.Store.YES));
        doc.add(new StringField("min_age", minAge == null ? "n/a" : minAge, Field.Store.YES));
        doc.add(new StringField("max_age", maxAge == null ? "n/a" : maxAge, Field.Store.YES));
        doc.add(new DoubleRange("age_range", minAgeRange, maxAgeRange));

        if (type.equals("main"))
            doc.add(new TextField("contents", trial.toString(), Field.Store.NO));
        if (type.equals("in"))
            doc.add(new TextField("contents", trial.getInclusionCriteria(), Field.Store.NO));
        if (type.equals("ex"))
            doc.add(new TextField("contents", trial.getExclusionCriteria(), Field.Store.NO));

        return doc;
    }

}
