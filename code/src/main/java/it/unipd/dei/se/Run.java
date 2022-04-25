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
    public static void main(String[] args) {
        String task = args[0];
        String indexDirectoryPath = args.length > 1 ? args[1] : "experiment/index";
        String stopListFilePath = args.length > 2 ? args[2] : "atire.txt";
        String synonymsListFilePath = args.length > 3 ? args[3] : "dictionary/adj.exc";
        boolean filter = args.length > 4 && Boolean.parseBoolean(args[4]);
        String matching = args.length > 5 ? args[5] : "bm25";
        String runId = args.length > 6 ? args[6] : "seupd2122-kueri";
        String runDirectoryPath = args.length > 7 ? args[7] : "runs";
        String qrelFilePath = args.length > 8 ? args[8] : "code/src/main/resource/qrels/example.txt";


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
                doIndex(indexDirectoryPath, stopListFilePath, synonymsListFilePath, similarity);
                break;
            case "search":
                doSearch(indexDirectoryPath, runId, runDirectoryPath, stopListFilePath, synonymsListFilePath, filter, similarity);
                break;
            case "rf":
                doRFSearch(indexDirectoryPath, runId, runDirectoryPath, qrelFilePath);
                break;
            case "rrf":
                doRRFSearch(runId, "rrf");
                break;
        }
    }

    private static void doIndex(String indexDirectoryPath, String stopListFilePath, String synonymsListFilePath, Similarity similarity) {
        try {
            DirectoryIndexer.doIndex(indexDirectoryPath, similarity, stopListFilePath, synonymsListFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void doSearch(String indexDirectoryPath, String runId, String runDirectoryPath, String stopWordsFilePath, String synonymsListFilePath, boolean filter, Similarity similarity) {
        try {
            Searcher.doSearch(indexDirectoryPath, runId, runDirectoryPath, stopWordsFilePath, synonymsListFilePath, filter, similarity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doRFSearch(String indexDirectoryPath, String runId, String runDirectoryPath, String qrelFilePath) {
        try {
            RF.doSearch(indexDirectoryPath, runDirectoryPath, runId, qrelFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doRRFSearch(String runId, String runDirectoryPath) {
        try {
            RRF.doSearch(runDirectoryPath, runId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

