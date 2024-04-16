package es.udc.fi.tfg.index;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class IndexTrecClinicalTrialsHelper {

    public static IndexParams parseIndexInputParams(final String[] params) {

        final Options options = new Options();

        options.addOption("index", true, "path to the es.udc.fi.tfg.index");
        options.addOption("docs", true, "path to the documents");
        options.addOption("model", true, "the model to be used");
        options.addOption("threads", true, "number of threads");

        try {
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, params);

            final String indexPath = cmd.getOptionValue("index");
            final String docsPath = cmd.getOptionValue("docs");

            final Similarity model = parseModel(cmd.getOptionValue("model"));

            final int numThreads = Integer.parseInt(cmd.getOptionValue("threads"));

            return new IndexParams(indexPath, docsPath, model, numThreads);
        } catch (final ParseException e) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("WebIndexer", options, true);
            throw new RuntimeException("Error parsing command line options - ", e);
        }
    }

    private static Similarity parseModel(final String model) throws ParseException {

        final String[] aux = model.split(" ");

        if (aux.length == 1) {
            throw new ParseException(
                    "Missing parameter for indexing model BM25. Valid options are [bm25 K1]");
        }

        switch (aux[0].toLowerCase()) {
            case "jm" -> {
                return new LMJelinekMercerSimilarity(Float.parseFloat(aux[1]));
            }
            case "bm25" -> {
                return new BM25Similarity(Float.parseFloat(aux[1]), 0.75f);
            }
            default -> throw new ParseException(
                    "Unknown indexing model: " + model + ". Valid options are [jm LAMBDA|bm25 K1]");
        }
    }
}
