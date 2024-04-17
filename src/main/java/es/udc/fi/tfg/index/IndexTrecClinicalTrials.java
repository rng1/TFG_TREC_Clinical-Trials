package es.udc.fi.tfg.index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexTrecClinicalTrials {

    private static final Logger logger = LoggerFactory.getLogger(IndexTrecClinicalTrials.class);

    public static void main(final String[] args) {

        // For timing purposes.
        final long start = System.currentTimeMillis();

        // Program arguments.
        final IndexParams params = IndexTrecClinicalTrialsHelper.parseIndexInputParams(args);

        final int numThreads = params.getNumThreads() > Runtime.getRuntime().availableProcessors()
                || params.getNumThreads() < 1 ? Runtime.getRuntime().availableProcessors() : params.getNumThreads();

        final IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setSimilarity(params.getModel());

        logger.info("Indexing to directory '{}'", params.getIndexPath());

        try (final IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(params.getIndexPath())), iwc)) {

            final File dir = new File(Paths.get(params.getDocsPath()).toUri());
            final File[] files = dir.listFiles();

            if (files != null) {
                final ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);

                for (final File file : files) {
                    final Runnable worker = new IndexerThread(file, writer);
                    exec.execute(worker);
                }

                exec.shutdown();

                try {
                    if (!exec.awaitTermination(5, TimeUnit.MINUTES)) {
                        logger.error("Indexing timeout");
                    }
                } catch (final InterruptedException e) {
                    logger.error(
                            "Thread {} was interrupted while waiting for executor to terminate. Executor state: {}. Error: {}",
                            Thread.currentThread().getName(), exec.isTerminated() ? "terminated" : "not terminated",
                            e.getMessage());
                }

                writer.commit();
                logger.info("Finished indexing in {} ms", System.currentTimeMillis() - start);
            }
        } catch (final IOException e) {
            logger.error("Indexing error - '{}'", e.getMessage());
        }
    }
}
