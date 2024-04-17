package es.udc.fi.tfg.index;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.search.similarities.Similarity;

import es.udc.fi.tfg.util.Utility;

public class IndexTrecClinicalTrialsHelper {

    public static IndexParams parseIndexInputParams(final String[] params) {

        final Options options = new Options();

        options.addOption("index", true, "path where the index will be stored");
        options.addOption("docs", true, "path to the documents to be indexed");
        options.addOption("model", true, "the model to be used");
        options.addOption("threads", true, "number of threads");

        try {
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, params);

            final String indexPath = cmd.getOptionValue("index");
            final String docsPath = cmd.getOptionValue("docs");

            final Similarity model = Utility.parseModel(cmd.getOptionValue("model"));

            final int numThreads = Integer.parseInt(cmd.getOptionValue("threads"));

            return new IndexParams(indexPath, docsPath, model, numThreads);
        } catch (final ParseException e) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("WebIndexer", options, true);
            throw new RuntimeException("Error parsing command line options - ", e);
        }
    }
}
