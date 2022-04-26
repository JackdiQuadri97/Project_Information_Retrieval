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

    private final String apiKey;
    private final ArgumentQualityClient argumentQualityClient;

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

    public ArgumentQualityReranker(String apiKeyPath, String apiKeyPropertyName) throws IOException {
        // read secret
        File f = new File(apiKeyPath);
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        Properties prop = new Properties();
        prop.load(is);


        // set properties
        this.apiKey = prop.getProperty(apiKeyPropertyName);
        this.argumentQualityClient =  DebaterApi.builder().build().getArgumentQualityClient();
    }

    public ArgumentQualityReranker() throws IOException {
        this("./secrets/secrets.txt", "DEBATER_API_KEY");
    }

    /**
     * Reranks the documents contained in {@code inputRunFilePath} based on the scores
     * given by the Ibm Project Debater API.
     * Processes at max {@code linesLimit} lines of input run file (to prevent an escessive use of the API)
     * @param inputRunFilePath
     * @param linesLimit
     * @throws IOException
     * @throws ParseException
     */
    public void rerank(String inputRunFilePath, String outputRunFilePath, int linesLimit) throws IOException, ParseException {
        String topicsFilePath = "./code/src/main/resource/topics-task2.xml";
        String indexFilePath = "./experiment/index";
        String outputRunFileRunId = "reranked";

        List<RunFilePartialLine> newRunFileLines = new ArrayList<>();
        List<ParsedTopic> topics = this.getTopics(topicsFilePath);
        BufferedReader runFileReader = new BufferedReader(new FileReader(inputRunFilePath));
        int count = 0;
        // for each line of the input run file ...
        for (String fileLine; (fileLine = runFileReader.readLine()) != null; ) {
            count += 1;
            if (count >= linesLimit) {
                break;
            }
            System.out.println(fileLine);

            // create an object representing the line
            ArrayList<String> lineParts = new ArrayList<>(Arrays.asList(fileLine.split("\t")));
            RunFilePartialLine lineObject = new RunFilePartialLine(
                    lineParts.get(0),
                    lineParts.get(2),
                    Integer.parseInt(lineParts.get(3)),
                    Double.parseDouble(lineParts.get(4)),
                    outputRunFileRunId);


            // retrieve topic
            Optional<ParsedTopic> correspondingTopic = topics.stream()
                    .filter(topic -> (Objects.equals(topic.getNumber(), lineObject.topicId)))
                    .findAny();
            if(correspondingTopic.isEmpty()) continue;

            // retrieve document
            Document correspondingDocument = this.getDocument(lineObject.documentId, indexFilePath);

            // get DebaterScore
            SentenceTopicPair sentenceTopicPair = new SentenceTopicPair(
                    correspondingDocument.getField(ParsedDocument.FIELDS.CONTENTS).stringValue(),
                    correspondingTopic.get().getTitle());
            Float debaterScore = this.getAPIScores(List.of(sentenceTopicPair)).get(0);

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

    public List<Float> getAPIScores(List<SentenceTopicPair> sentenceTopicPairs) throws IOException {
        return this.argumentQualityClient.getScores(sentenceTopicPairs, this.apiKey);
    }

    public Document getDocument(String documentID, String indexDirectoryPath) throws ParseException, IOException {

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

    public List<ParsedTopic> getTopics(String topicsFile) throws IOException {
        // TOPICS READING AND PARSING

        BufferedReader in = Files.newBufferedReader(Paths.get(topicsFile), StandardCharsets.UTF_8);
        // old way of reading the topics
        // topics = new TrecTopicsReader().readQueries(in);

        // create a temp list
        List<ParsedTopic> tempList = new ArrayList<>();
        // create a topicparser
        XMLTopicParser topicsParser = new XMLTopicParser(in);
        // fill list with parsed topics
        topicsParser.forEachRemaining(tempList::add);
        // convert list to array and assign to attribute topics
        return tempList;
    }

    public static Double combineScores(Double score1, Double score2) { return score1 * score2; }

    public static void main (String[] args) throws IOException, ParseException {

        ArgumentQualityReranker argumentQualityVerifier = new ArgumentQualityReranker();

        argumentQualityVerifier.rerank(
                "./runs/test_run_file.txt",
                "./runs/",
                5);

        /*String docId = "clueweb12-0000tw-00-14115___11";
        Document doc = argumentQualityVerifier.getDocument(docId, "./experiment/index");
        String docContent = doc.getField(ParsedDocument.FIELDS.CONTENTS).stringValue();
        System.out.println(docContent);*/

        /*List<SentenceTopicPair> sentenceTopicPairs = Arrays.asList(
                new SentenceTopicPair("Cars should only provide assisted driving, not complete autonomy.","We should further explore the development of autonomous vehicles"),
                new SentenceTopicPair("Cars cars cars cars who cares","We should further explore the development of autonomous vehicles"),
                new SentenceTopicPair("that he given sun roads sea","We should further explore the development of autonomous vehicles"));

        ArgumentQualityVerifier argumentQualityVerifier = new ArgumentQualityVerifier();

        List<Float> scores = argumentQualityVerifier.computeScores(sentenceTopicPairs);

        System.out.println("Pairs of (sentence, topic) and their argument-quality scores:\n");
        for (int i = 0; i < sentenceTopicPairs.size(); i++){
            System.out.println(String.format("Sentence: %s\nTopic: %s\nScore: %.4f\n",
                    sentenceTopicPairs.get(i).getSentence(), sentenceTopicPairs.get(i).getTopic(),scores.get(i)));
        }*/
    }

}