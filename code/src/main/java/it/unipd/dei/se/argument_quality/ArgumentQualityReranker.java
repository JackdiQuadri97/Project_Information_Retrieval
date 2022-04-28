package it.unipd.dei.se.argument_quality;

/* (C) Copyright IBM Corp. 2020. */
/* created using IBM Project Debater */

import com.ibm.hrl.debater.clients.DebaterApi;
import com.ibm.hrl.debater.clients.SentenceTopicPair;
import com.ibm.hrl.debater.clients.argument_quality.ArgumentQualityClient;
import it.unipd.dei.se.parse.document.ParsedDocument;
import it.unipd.dei.se.parse.topic.ParsedTopic;
import it.unipd.dei.se.parse.topic.XMLTopicParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ArgumentQualityReranker {

    private static class RunFilePartialLine {
        public String topicId;
        public String documentId;
        public Integer rank;
        public Double score;
        public String runId;

        public RunFilePartialLine(String topicId, String documentId, Integer rank, Double score, String runId) {
            this.topicId = topicId;
            this.documentId = documentId;
            this.rank = rank;
            this.score = score;
            this.runId = runId;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Integer getRank() {
            return rank;
        }

        public void setRank(Integer rank) {
            this.rank = rank;
        }

        public String toString() {
            return String.format(Locale.ENGLISH, "%s\tQ0\t%s\t%d\t%.6f\t%s%n",
                    topicId,
                    documentId,
                    rank,
                    score,
                    runId
            );
        }
    }

    public static class CustomComparator implements Comparator<RunFilePartialLine> {
        @Override
        public int compare(RunFilePartialLine o1, RunFilePartialLine o2) {
            return o2.score.compareTo(o1.score);
        }
    }

    /**
     * Reranks the documents contained in {@code inputRunFilePath} based on the scores
     * saved in the stored field "score" of each indexed document
     * @throws IOException
     * @throws ParseException
     */
    public static void rerank(String inputRunFilePath,
                              String outputRunFilePath,
                              String outputRunFileRunId,
                              String indexFilePath) throws IOException, ParseException {

        List<RunFilePartialLine> newRunFileLines = new ArrayList<>();
        BufferedReader runFileReader = new BufferedReader(new FileReader(inputRunFilePath));
        // for each line of the input run file ...
        for (String fileLine; (fileLine = runFileReader.readLine()) != null; ) {
            System.out.println(fileLine);

            // create an object representing the line
            ArrayList<String> lineParts = new ArrayList<>(Arrays.asList(fileLine.split("\t")));
            RunFilePartialLine lineObject = new RunFilePartialLine(
                    lineParts.get(0),
                    lineParts.get(2),
                    Integer.parseInt(lineParts.get(3)),
                    Double.parseDouble(lineParts.get(4)),
                    outputRunFileRunId);

            // retrieve document
            Document correspondingDocument = ArgumentQualityReranker.getDocument(lineObject.documentId, indexFilePath);

            // get DebaterScore
            Float debaterScore = correspondingDocument.getField("score").numericValue().floatValue();
            System.out.printf("got score: %s%n", debaterScore);

            // compute and assign new score
            Double newScore = ArgumentQualityReranker.combineScores(
                    lineObject.getScore(),
                    debaterScore.doubleValue());
            lineObject.setScore(newScore);

            // add the lineObject to the list
            newRunFileLines.add(lineObject);
        }

        newRunFileLines.sort(new ArgumentQualityReranker.CustomComparator());

        Writer output = new BufferedWriter(new FileWriter(outputRunFilePath + "/" + outputRunFileRunId + ".txt"));  //clears file every time

        try {
            // for each lineObject in the list
            System.out.println(newRunFileLines.size());
            for ( int i = 0; i<newRunFileLines.size(); i++) {
                RunFilePartialLine line = newRunFileLines.get(i);
                line.setRank(i + 1); // change the rank according to the new order
                output.append(line.toString()); // write to the output file
            }

            output.flush();
        } finally {
            output.close();
        }

    }

    public static Document getDocument(String documentID, String indexDirectoryPath) throws ParseException, IOException {

        Analyzer analyzer = new WhitespaceAnalyzer();
        // System.out.printf("Searching for topic %s.%n", topicId);

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        QueryParser queryParser = new QueryParser("id", analyzer);

        queryBuilder.add(queryParser.parse(QueryParserBase.escape(String.valueOf(documentID))), BooleanClause.Occur.MUST);

        BooleanQuery query = queryBuilder.build();
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        try {
            TopDocs topDocsObject = searcher.search(query, 1);

            ScoreDoc[] topDocs = topDocsObject.scoreDocs;
            int doc;
            if (topDocs[0] != null)
                doc = topDocs[0].doc;
            else return null;

            return reader.document(doc);
        } finally {
            reader.close();
        }

    }

    public static Double combineScores(Double score1, Double score2) { return score1 * score2; }

    public static void main (String[] args) throws IOException, ParseException {

        ArgumentQualityReranker.rerank(
                "./runs/seupd2122-kueri.txt",
                "./runs/",
                "reranked",
                "./experiment/index3");
    }

}