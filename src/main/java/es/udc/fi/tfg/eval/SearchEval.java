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
            writer.write("id\tnDCG@" + CUT + "\tP@" + CUT + "\tRR\n");

            for (final Topic topic : topics)
                processTopic(topic, qrels, parser, searcher, writer, meanMetrics);

            logger.info("Mean metrics - nDCG: {}, P: {}, MRR: {}", meanMetrics.getMnDCG(), meanMetrics.getMP(),
                    meanMetrics.getMRR());

        } catch (final IOException e) {
            logger.error("Error opening index - {}", e.getMessage());
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
            final MeanMetrics meanMetrics) {

        logger.info("Processing topic {}", topic.getId());

        final Map<String, Integer> innerMap = qrels.get(topic.getId());

        try {
            final Query descriptionQuery = parser.parse(QueryParser.escape(topic.getDescription()));

            // Create the gender filters and combine them.
            final String genderFilterValue = SearchEvalHelper.getGenderFilterValue(topic.getGender());
            final Query genderFilter = new TermQuery(new Term("gender", genderFilterValue));
            final Query allGenderFilter = new TermQuery(new Term("gender", "all"));
            final BooleanQuery genderBooleanQuery = new BooleanQuery.Builder()
                    .add(genderFilter, BooleanClause.Occur.SHOULD)
                    .add(allGenderFilter, BooleanClause.Occur.SHOULD)
                    .build();

            // Combine the description query and the gender filter.
            final BooleanQuery query = new BooleanQuery.Builder()
                    .add(descriptionQuery, BooleanClause.Occur.MUST)
                    .add(genderBooleanQuery, BooleanClause.Occur.FILTER)
                    .build();

            final TopDocs hits = searcher.search(query, TRIALS_PER_TOPIC);
            final StoredFields storedFields = searcher.storedFields();

            final int retrieved = (int) hits.totalHits.value;
            final int cut = Math.min(retrieved, CUT);

            // Initialize the metrics calculator for the current query.
            final TopicMetrics topicMetrics = new TopicMetrics();

            for (int i = 0; i < cut; i++) {

                final ScoreDoc hit = hits.scoreDocs[i];
                final String docId = storedFields.document(hit.doc).get("nct_id");
                final int relevance = innerMap.getOrDefault(docId, 0);

                topicMetrics.updateMetrics(relevance, i);

                logger.info("Topic {} - Document{} '{}' with score {}", topic.getId(), relevance == 2 ? "*" : "",
                        docId, hit.score);
            }

            final double p = topicMetrics.getP(cut);
            final double rr = topicMetrics.getRR();
            final double dcg = topicMetrics.getDCG(cut);
            final double idcg = topicMetrics.getIDCG(cut);
            final double ndcg = (idcg == 0) ? 0 : dcg / idcg;

            writer.write(topic.getId() + "\t" + ndcg + "\t" + p + "\t" + rr + "\n");
            meanMetrics.updateMetrics(p, rr, ndcg);

            logger.info("Topic {} - nDCG@{}: {}, P@{}: {}, RR: {}", topic.getId(), cut, ndcg, cut, p, rr);

        } catch (final ParseException e) {
            logger.error("Error parsing query - {}", e.getMessage());
        } catch (final IOException e) {
            logger.error("Error searching index - {}", e.getMessage());
        }
    }
}
