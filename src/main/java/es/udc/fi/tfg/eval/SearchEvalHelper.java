package es.udc.fi.tfg.eval;

import static es.udc.fi.tfg.util.Parameters.DOCS_PATH;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.udc.fi.tfg.data.Topic;
import es.udc.fi.tfg.util.Parameters;

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
     * Parse all the possible gender values to "male", "female" or ""(unknown)
     *
     * @param gender
     *            the gender to parse.
     * @return the parsed gender.
     */
    protected static String getGenderFilterValue(final String gender) {
        return switch (gender) {
        case "woman", "female", "girl" -> "female";
        case "man", "male", "boy" -> "male";
        default -> "";
        };
    }

    /**
     * Performs the TOPSIS (Technique for Order of Preference by Similarity to Ideal Solution) method on the given
     * TopDocs and StoredFields. This method calculates the evaluation matrix, normalizes it, applies weights to it, and
     * calculates the ratios. The result is a map where each key is a document ID and the value is the calculated ratio.
     * The map is sorted in descending order by the ratio values.
     *
     * @param hitsMain
     *            the TopDocs for the main index.
     * @param hitsIn
     *            the TopDocs for the inclusion criteria index.
     * @param hitsEx
     *            the TopDocs for the exclusion criteria index.
     * @param storedFieldsMain
     *            the StoredFields for the main index.
     * @param storedFieldsIn
     *            the StoredFields for the inclusion criteria index.
     * @param storedFieldsEx
     *            the StoredFields for the exclusion criteria index.
     * @return a sorted Map where the keys are document IDs and the values are the calculated ratios.
     * @throws IOException
     *             if an I/O error occurs.
     */
    protected static Map<String, Double> performTopsis(final TopDocs hitsMain, final TopDocs hitsIn,
            final TopDocs hitsEx, final StoredFields storedFieldsMain, final StoredFields storedFieldsIn,
            final StoredFields storedFieldsEx) throws IOException {

        final Map<String, double[]> evaluationMatrix = buildEvaluationMatrix(hitsMain, hitsIn, hitsEx, storedFieldsMain,
                storedFieldsIn, storedFieldsEx);
        final Map<String, double[]> normalisedDecisionMatrix = buildNormalisedDecisionMatrix(evaluationMatrix);
        final Map<String, double[]> weightedMatrix = applyWeights(normalisedDecisionMatrix);
        final Map<String, Double> result = calculateRatios(weightedMatrix);

        return result.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double> comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    private static Map<String, double[]> buildEvaluationMatrix(final TopDocs hitsMain, final TopDocs hitsIn,
            final TopDocs hitsEx, final StoredFields storedFieldsMain, final StoredFields storedFieldsIn,
            final StoredFields storedFieldsEx) throws IOException {

        final Map<String, double[]> matrixMap = new HashMap<>();

        // Iterate over each TopDocs
        for (final TopDocs hits : new TopDocs[] { hitsMain, hitsIn, hitsEx }) {
            // For each document
            for (final ScoreDoc hit : hits.scoreDocs) {
                String nctId = null;
                // Update the corresponding score
                if (hits == hitsMain) {
                    nctId = storedFieldsMain.document(hit.doc).get("nct_id");
                } else if (hits == hitsIn) {
                    nctId = storedFieldsIn.document(hit.doc).get("nct_id");
                } else if (hits == hitsEx) {
                    nctId = storedFieldsEx.document(hit.doc).get("nct_id");
                }

                // Get the current scores array or create a new one with default values
                final double[] scores = matrixMap.getOrDefault(nctId, new double[] { 0, 0, 0 });

                // Update the corresponding score
                if (hits == hitsMain) {
                    scores[0] = hit.score;
                } else if (hits == hitsIn) {
                    scores[1] = hit.score;
                } else if (hits == hitsEx) {
                    scores[2] = hit.score;
                }

                // Put the scores array back into the map
                matrixMap.put(nctId, scores);
            }
        }

        return matrixMap;
    }

    private static Map<String, double[]> buildNormalisedDecisionMatrix(final Map<String, double[]> evaluationMatrix) {

        final int cols = evaluationMatrix.values().iterator().next().length;

        // Create a new map with the same keys as the original map
        final Map<String, double[]> normalizedMatrix = new HashMap<>();

        // Calculate the square root of the sum of squares for each column
        final double[] columnSums = new double[cols];
        for (int j = 0; j < cols; j++) {
            double sum = 0;
            for (final double[] row : evaluationMatrix.values()) {
                sum += Math.pow(row[j], 2);
            }
            columnSums[j] = Math.sqrt(sum);
        }

        // Divide each element in the original matrix by the corresponding column's square root of the sum of squares
        for (final Map.Entry<String, double[]> entry : evaluationMatrix.entrySet()) {
            final double[] normalizedRow = new double[cols];
            for (int j = 0; j < cols; j++) {
                normalizedRow[j] = entry.getValue()[j] / columnSums[j];
            }
            normalizedMatrix.put(entry.getKey(), normalizedRow);
        }

        return normalizedMatrix;
    }

    private static Map<String, double[]> applyWeights(final Map<String, double[]> normalizedMatrix) {
        final Map<String, double[]> weightedMatrix = new HashMap<>();

        final double[] idealBest = { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
        final double[] idealWorst = { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };

        for (final Map.Entry<String, double[]> entry : normalizedMatrix.entrySet()) {
            final double[] weightedRow = new double[3];
            weightedRow[0] = entry.getValue()[0] * Parameters.MAIN_WEIGHT;
            weightedRow[1] = entry.getValue()[1] * Parameters.INCLUSION_WEIGHT;
            weightedRow[2] = entry.getValue()[2] * Parameters.EXCLUSION_WEIGHT;

            idealBest[0] = Math.max(weightedRow[0], idealBest[0]);
            idealBest[1] = Math.max(weightedRow[1], idealBest[1]);
            idealBest[2] = Math.min(weightedRow[2], idealBest[2]);

            idealWorst[0] = Math.min(weightedRow[0], idealWorst[0]);
            idealWorst[1] = Math.min(weightedRow[1], idealWorst[1]);
            idealWorst[2] = Math.max(weightedRow[2], idealWorst[2]);

            weightedMatrix.put(entry.getKey(), weightedRow);
        }

        // Add idealBest and idealWorst to the matrix
        weightedMatrix.put("ideal_best", idealBest);
        weightedMatrix.put("ideal_worst", idealWorst);

        return weightedMatrix;
    }

    private static Map<String, Double> calculateRatios(final Map<String, double[]> weightedMatrix) {
        final Map<String, Double> result = new HashMap<>();
        final double[] idealBest = weightedMatrix.get("ideal_best");
        final double[] idealWorst = weightedMatrix.get("ideal_worst");

        for (final Map.Entry<String, double[]> entry : weightedMatrix.entrySet()) {
            final String key = entry.getKey();
            if (!key.equals("ideal_best") && !key.equals("ideal_worst")) {
                final double[] value = entry.getValue();
                double d_ib = 0;
                double d_iw = 0;
                for (int i = 0; i < value.length; i++) {
                    d_ib += Math.pow(value[i] - idealBest[i], 2);
                    d_iw += Math.pow(value[i] - idealWorst[i], 2);
                }
                d_ib = Math.sqrt(d_ib);
                d_iw = Math.sqrt(d_iw);
                result.put(key, d_iw / (d_iw + d_ib));
            }
        }
        return result;
    }

}
