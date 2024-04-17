package es.udc.fi.tfg.eval;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.udc.fi.tfg.data.Topic;
import es.udc.fi.tfg.eval.metrics.MeanMetrics;
import es.udc.fi.tfg.eval.metrics.TopicMetrics;

public class SearchEvalTrecClinicalTrials {

    private static final Logger logger = LoggerFactory.getLogger(SearchEvalTrecClinicalTrials.class);

    public static void main(final String[] args) {

        // Program arguments.
        final SearchEvalParams params = SearchEvalTrecClinicalTrialsHelper.parseSearchInputParams(args);

        // Topics and relevance parsing.
        final Collection<Topic> topics = SearchEvalTrecClinicalTrialsHelper.parseTopics(params.getDocsPath());
        final Map<Integer, Map<String, Integer>> qrels = SearchEvalTrecClinicalTrialsHelper
                .parseQrels(params.getDocsPath());

        try (final IndexReader reader = DirectoryReader.open(FSDirectory.open(Path.of(params.getIndexPath())))) {

            final IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(params.getModel());
            final QueryParser parser = new QueryParser("contents", new StandardAnalyzer());

            // Initialize the metrics calculator for all queries.
            final MeanMetrics meanMetrics = new MeanMetrics();

            for (final Topic topic : topics) {

                logger.info("Processing topic '{}'", topic.getId());

                final Map<String, Integer> innerMap = qrels.get(topic.getId());
                final long totalRelevant = innerMap.values().stream().filter(e -> e != 0).count();

                final Query query = parser.parse(QueryParser.escape(topic.getDescription()));
                final TopDocs hits = searcher.search(query, params.getCut());
                final int retrieved = (int) hits.totalHits.value;
                final StoredFields storedFields = searcher.storedFields();

                // Initialize the metrics calculator for the current query.
                final TopicMetrics topicMetrics = new TopicMetrics(retrieved, totalRelevant);

                for (int i = 0; i < Math.min(retrieved, params.getCut()); i++) {

                    final ScoreDoc hit = hits.scoreDocs[i];
                    final String docId = storedFields.document(hit.doc).get("nct_id");
                    final int relevance = innerMap.getOrDefault(docId, 0);

                    topicMetrics.updateMetrics(relevance, i);

                    logger.info("Topic {} - Document{} '{}' with score '{}'", topic.getId(), relevance != 0 ? "*" : "",
                            docId, hit.score);
                }

                final double precision = topicMetrics.getPrecision(params.getCut());
                final double recall = topicMetrics.getRecall();
                final double averagePrecision = topicMetrics.getAveragePrecision();
                final double reciprocalRank = topicMetrics.getReciprocalRank();

                meanMetrics.updateMetrics(precision, recall, averagePrecision, reciprocalRank);

                logger.info(
                        "Topic '{}' - Precision: '{}', Recall: '{}', Average Precision: '{}', Reciprocal Rank: '{}'",
                        topic.getId(), precision, recall, averagePrecision, reciprocalRank);

            }

            logger.info("Mean metrics - Precision: '{}', Recall: '{}', Average Precision: '{}', Reciprocal Rank: '{}'",
                    meanMetrics.getMeanPrecision(), meanMetrics.getMeanRecall(), meanMetrics.getMeanAveragePrecision(),
                    meanMetrics.getMeanReciprocalRank());

        } catch (final IOException e) {
            logger.error("Error opening index - '{}'", e.getMessage());
        } catch (final ParseException e) {
            logger.error("Error parsing query - '{}'", e.getMessage());
        }

    }
}
