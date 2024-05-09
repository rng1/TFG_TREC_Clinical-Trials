package es.udc.fi.tfg.eval;

import static es.udc.fi.tfg.util.Parameters.EVAL_FILENAME;
import static es.udc.fi.tfg.util.Parameters.INDEX_PATH;
import static es.udc.fi.tfg.util.Parameters.RUN_NAME;
import static es.udc.fi.tfg.util.Parameters.SIMILARITY;
import static es.udc.fi.tfg.util.Parameters.TRIALS_PER_TOPIC;
import static es.udc.fi.tfg.util.Parameters.USE_QUERY_FILTER;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DoubleRange;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.udc.fi.tfg.data.Topic;
import es.udc.fi.tfg.util.Utility;

public class SearchEval {

    private static final Logger logger = LoggerFactory.getLogger(SearchEval.class);

    public static void main(final String[] args) {

        // Topics and relevance parsing.
        final Collection<Topic> topics = SearchEvalHelper.parseTopics();

        try (final IndexReader reader = DirectoryReader.open(FSDirectory.open(Path.of(INDEX_PATH)));
                final PrintWriter printWriter = new PrintWriter(EVAL_FILENAME)) {

            final IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(SIMILARITY);
            final QueryParser parser = new QueryParser("contents", new StandardAnalyzer());

            for (final Topic topic : topics)
                processTopic(topic, parser, searcher, printWriter);

        } catch (final IOException e) {
            logger.error("Error handling the index - {}", e.getMessage());
        } catch (final ParseException e) {
            logger.error("Error parsing query - {}", e.getMessage());
        }

    }

    /**
     * Process a single topic.
     *
     * @param topic
     *            the topic to process.
     * @param parser
     *            the query parser.
     * @param searcher
     *            the index searcher.
     */
    private static void processTopic(final Topic topic, final QueryParser parser, final IndexSearcher searcher,
            final PrintWriter printWriter)
            throws IOException, ParseException {

        logger.info("Processing topic {}", topic.getId());

        final BooleanQuery query = getQuery(topic, parser);

        final TopDocs hits = searcher.search(query, TRIALS_PER_TOPIC);
        final StoredFields storedFields = searcher.storedFields();

        // @cut
        processDocuments(topic, hits, storedFields, printWriter);
    }

    /**
     * Process the documents retrieved by the query.
     *
     * @param topic
     *            the topic being processed.
     * @param hits
     *            the documents retrieved.
     * @param storedFields
     *            the stored fields.
     */
    private static void processDocuments(final Topic topic, final TopDocs hits,
            final StoredFields storedFields, final PrintWriter printWriter)
            throws IOException {

        for (int i = 0; i < 1000; i++) {

            final ScoreDoc hit = hits.scoreDocs[i];
            final String docId = storedFields.document(hit.doc).get("nct_id");
            printWriter.println(
                    topic.getId() + " Q0 " + docId.toUpperCase() + " " + (i + 1) + " " + hit.score + " " + RUN_NAME);

            logger.info("Topic {} Document {} with score {}", topic.getId(), docId, hit.score);
        }
    }

    /**
     * Build the query for the given topic from the required filters.
     *
     * @param topic
     *            the topic.
     * @param parser
     *            the query parser.
     * @return the query.
     */
    private static BooleanQuery getQuery(final Topic topic, final QueryParser parser) throws ParseException {

        final Query descriptionQuery = parser.parse(QueryParser.escape(topic.getDescription()));

        // Gender filters.
        final String genderFilterValue = SearchEvalHelper.getGenderFilterValue(topic.getGender());
        final Query genderFilter = new TermQuery(new Term("gender", genderFilterValue));
        final Query allGenderFilter = new TermQuery(new Term("gender", "all"));
        final BooleanQuery genderBooleanQuery = new BooleanQuery.Builder()
                .add(genderFilter, BooleanClause.Occur.SHOULD)
                .add(allGenderFilter, BooleanClause.Occur.SHOULD)
                .build();

        // Age filter.
        final double[] ageNorm = new double[] { Utility.normalizeAge(topic.getAge()) };
        final Query ageFilter = DoubleRange.newCrossesQuery("age_range", ageNorm, ageNorm);

        // Query builder.
        return USE_QUERY_FILTER
                ? new BooleanQuery.Builder()
                        .add(descriptionQuery, BooleanClause.Occur.MUST)
                        .add(genderBooleanQuery, BooleanClause.Occur.FILTER)
                        .add(ageFilter, BooleanClause.Occur.FILTER)
                        .build()
                : new BooleanQuery.Builder()
                        .add(descriptionQuery, BooleanClause.Occur.MUST)
                        .build();

    }
}
