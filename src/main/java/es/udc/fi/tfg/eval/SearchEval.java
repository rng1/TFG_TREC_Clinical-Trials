package es.udc.fi.tfg.eval;

import static es.udc.fi.tfg.util.Parameters.CUT;
import static es.udc.fi.tfg.util.Parameters.INDEX_PATH;
import static es.udc.fi.tfg.util.Parameters.SIMILARITY;
import static es.udc.fi.tfg.util.Parameters.TRIALS_PER_TOPIC;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

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
import es.udc.fi.tfg.eval.metrics.MeanMetrics;
import es.udc.fi.tfg.eval.metrics.TopicMetrics;
import es.udc.fi.tfg.util.Utility;

public class SearchEval {

    private static final Logger logger = LoggerFactory.getLogger(SearchEval.class);

    public static void main(final String[] args) {

        // Topics and relevance parsing.
        final Collection<Topic> topics = SearchEvalHelper.parseTopics();
        final Map<Integer, Map<String, Integer>> qrels = SearchEvalHelper.parseQrels();

        try (final IndexReader reader = DirectoryReader.open(FSDirectory.open(Path.of(INDEX_PATH)));
                final BufferedWriter writer = new BufferedWriter(
                        new FileWriter(SearchEvalHelper.getMetricsFileName()))) {

            final IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(SIMILARITY);
            final QueryParser parser = new QueryParser("contents", new StandardAnalyzer());

            final MeanMetrics meanMetrics = new MeanMetrics();
            writer.write("id;nDCG@" + CUT + ";P@" + CUT + ";RR\n");

            for (final Topic topic : topics)
                processTopic(topic, qrels, parser, searcher, writer, meanMetrics);

            logger.info("Mean metrics - nDCG: {}, RPrec: {}, P: {}, MRR: {}", meanMetrics.getMnDCG(),
                    meanMetrics.getMRP(), meanMetrics.getMP(), meanMetrics.getMRR());

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
     * @param qrels
     *            the relevance judgments.
     * @param parser
     *            the query parser.
     * @param searcher
     *            the index searcher.
     * @param meanMetrics
     *            the mean metrics calculator.
     */
    private static void processTopic(final Topic topic, final Map<Integer, Map<String, Integer>> qrels,
            final QueryParser parser, final IndexSearcher searcher, final BufferedWriter writer,
            final MeanMetrics meanMetrics) throws IOException, ParseException {

        logger.info("Processing topic {}", topic.getId());

        final Map<String, Integer> innerMap = qrels.get(topic.getId());
        final int totalRelevant = (int) innerMap.values().stream().filter(e -> e == 2).count();

        final BooleanQuery query = getQuery(topic, parser);

        final TopDocs hits = searcher.search(query, TRIALS_PER_TOPIC);
        final StoredFields storedFields = searcher.storedFields();

        final int retrieved = (int) hits.totalHits.value;
        final int cut = Math.min(retrieved, CUT);

        // Initialize the metrics calculator for the current query.
        final TopicMetrics topicMetrics = new TopicMetrics();

        // @cut
        processDocuments(topic, innerMap, hits, storedFields, topicMetrics, cut, 0);

        final double p = topicMetrics.getP(cut);
        final double rr = topicMetrics.getRR();
        final double dcg = topicMetrics.getDCG(cut);
        final double idcg = topicMetrics.getIDCG(cut);
        final double ndcg = (idcg == 0) ? 0 : dcg / idcg;

        // @totalRelevant (R-Prec)
        processDocuments(topic, innerMap, hits, storedFields, topicMetrics, totalRelevant, cut);

        final double rprec = topicMetrics.getP(totalRelevant);

        writer.write(topic.getId() + ";" + ndcg + ";" + rprec + ";" + rr + ";" + p + "\n");
        meanMetrics.updateMetrics(p, rr, ndcg, rprec);

        logger.info("Topic {} - nDCG@{}: {}, RPrec: {}, P@{}: {}, RR: {}", topic.getId(), cut, ndcg, rprec, cut, p,
                rr);
    }

    /**
     * Process the documents retrieved by the query.
     *
     * @param topic
     *            the topic being processed.
     * @param innerMap
     *            the relevance judgments.
     * @param hits
     *            the documents retrieved.
     * @param storedFields
     *            the stored fields.
     * @param topicMetrics
     *            the topic metrics calculator.
     * @param limit
     *            the limit of documents to process.
     * @param start
     *            the start index.
     */
    private static void processDocuments(final Topic topic, final Map<String, Integer> innerMap, final TopDocs hits,
            final StoredFields storedFields, final TopicMetrics topicMetrics, final int limit, final int start)
            throws IOException {

        for (int i = start; i < limit; i++) {

            final ScoreDoc hit = hits.scoreDocs[i];
            final String docId = storedFields.document(hit.doc).get("nct_id");
            final int relevance = innerMap.getOrDefault(docId, 0);

            topicMetrics.updateMetrics(relevance, i);

            logger.info(
                    "Topic {} {} Document{} '{}' ({}) with score {}", topic.getId(), start == 0 ? "#" : "-",
                    relevance == 2 ? "*" : " ", docId, relevance, hit.score);
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

        // Combined query.
        return new BooleanQuery.Builder()
                .add(descriptionQuery, BooleanClause.Occur.MUST)
                .add(genderBooleanQuery, BooleanClause.Occur.FILTER)
                .add(ageFilter, BooleanClause.Occur.FILTER)
                .build();

    }
}
