package it.unipd.dei.se;

import it.unipd.dei.se.index.DirectoryIndexer;
import it.unipd.dei.se.rf.RF;
import it.unipd.dei.se.rrf.RRF;
import it.unipd.dei.se.search.Searcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;

public class Run {
    /**
     * It takes in a task, an index directory, a stop list file, a boolean for whether to filter, a similarity function, a
     * run id, a run directory, and a qrel file, and then does the task
     */
    public static void main(String[] args) {
        String task = args[0];
        String indexDirectoryPath = args.length > 1 ? args[1] : "experiment/index";
        String stopListFilePath = args.length > 2 ? args[2] : "lucene.txt";
        boolean filter = args.length > 3 && Boolean.parseBoolean(args[3]);
        String matching = args.length > 4 ? args[4] : "bm25";
        String runId = args.length > 5 ? args[5] : "seupd2122-kueri";
        String runDirectoryPath = args.length > 6 ? args[6] : "runs";
        String qrelFilePath = args.length > 7 ? args[8] : "code/src/main/resource/qrels/example.txt";


        Similarity similarity = null;
        switch (matching) {
            case "bm25":
                similarity = new BM25Similarity();
                break;
            case "tfidf":
                similarity = new ClassicSimilarity();
                break;
            case "lmd":
                similarity = new LMDirichletSimilarity();
                break;
            default:
                similarity = new BM25Similarity();
                break;
        }

        switch (task) {
            case "index":
                doIndex(indexDirectoryPath, stopListFilePath, similarity);
                break;
            case "search":
                doSearch(indexDirectoryPath, runId, runDirectoryPath, stopListFilePath, filter, similarity);
                break;
            case "rf":
                doRFSearch(indexDirectoryPath, runId, runDirectoryPath, qrelFilePath);
                break;
            case "rrf":
                doRRFSearch(runId, "rrf");
                break;
        }
    }

    /**
     * It takes a directory of files, a stop list file, and a similarity function, and creates an index of the files in the
     * directory
     *
     * @param indexDirectoryPath The path to the directory where the index will be stored.
     * @param stopListFilePath The path to the stop list file.
     * @param similarity The similarity function to use.
     */
    private static void doIndex(String indexDirectoryPath, String stopListFilePath, Similarity similarity) {
        try {
            DirectoryIndexer.doIndex(indexDirectoryPath, similarity, stopListFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * It takes in the path to the index directory, the run id, the path to the run directory, the path to the stop words
     * file, a boolean value indicating whether to filter stop words or not, and the similarity function to use, and then
     * it performs the search
     *
     * @param indexDirectoryPath The path to the directory where the index is stored.
     * @param runId The name of the run. This will be used to name the output file.
     * @param runDirectoryPath The directory where the run file will be written.
     * @param stopWordsFilePath The path to the stop words file.
     * @param filter If true, the stop words file will be used to filter out stop words from the query.
     * @param similarity The similarity function to use.
     */
    private static void doSearch(String indexDirectoryPath, String runId, String runDirectoryPath, String stopWordsFilePath, boolean filter, Similarity similarity) {
        try {
            Searcher.doSearch(indexDirectoryPath, runId, runDirectoryPath, stopWordsFilePath, filter, similarity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It takes in the path to the index directory, the path to the run directory, the run id, and the path to the qrel
     * file, and then it runs the RF search
     *
     * @param indexDirectoryPath The path to the directory where the index is stored.
     * @param runId The name of the run. This will be used to name the run file.
     * @param runDirectoryPath The directory where the run file will be written.
     * @param qrelFilePath The path to the qrel file.
     */
    private static void doRFSearch(String indexDirectoryPath, String runId, String runDirectoryPath, String qrelFilePath) {
        try {
            RF.doSearch(indexDirectoryPath, runDirectoryPath, runId, qrelFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It takes a runId and a runDirectoryPath as input, and then it calls the doSearch function in the RRF class, which is
     * located in the RRF.java file
     *
     * @param runId This is the name of the run. It will be used to name the output files.
     * @param runDirectoryPath The path to the directory where the run files are located.
     */
    private static void doRRFSearch(String runId, String runDirectoryPath) {
        try {
            RRF.doSearch(runDirectoryPath, runId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

