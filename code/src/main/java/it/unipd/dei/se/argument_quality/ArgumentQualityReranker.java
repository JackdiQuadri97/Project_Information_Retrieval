package it.unipd.dei.se.argument_quality;

/* (C) Copyright IBM Corp. 2020. */
/* created using IBM Project Debater */
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.*;
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
            int o2topic = Integer.parseInt(o2.topicId);
            int o1topic = Integer.parseInt(o1.topicId);
            if (o2topic==o1topic){
                return o2.score.compareTo(o1.score);
            }
            else{
                return Integer.compare(o1topic,o2topic);
            }
        }
    }

    /**
     * Reranks the documents contained in {@code inputRunFilePath} based on the scores
     * saved in the file {@code scoresFilePath}
     * If a document is missing from the scores file it's score remains unchanged
     * @throws IOException
     * @throws ParseException
     */
    public static void rerank(String inputRunFilePath,
                              String outputRunFilePath,
                              String outputRunFileRunId,
                              String scoresFilePath) throws IOException, ParseException {
        Map<String,Float> scoreDocs = ArgumentQualityReranker.retrieveScoresFromFile(scoresFilePath);

        List<RunFilePartialLine> newRunFileLines = new ArrayList<>();
        BufferedReader runFileReader = new BufferedReader(new FileReader(inputRunFilePath));
        // for each line of the input run file ...
        for (String fileLine; (fileLine = runFileReader.readLine()) != null; ) {

            // create an object representing the line
            ArrayList<String> lineParts = new ArrayList<>(Arrays.asList(fileLine.split("\t")));
            RunFilePartialLine lineObject = new RunFilePartialLine(
                    lineParts.get(0),
                    lineParts.get(2),
                    Integer.parseInt(lineParts.get(3)),
                    Double.parseDouble(lineParts.get(4)),
                    outputRunFileRunId);

            // get DebaterScore
            Float debaterScore = scoreDocs.getOrDefault(lineObject.documentId, 1f);
            System.out.printf("got debater score from file: %s for document %s%n", debaterScore, lineObject.documentId);

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
            System.out.printf("Documents to be written to new run file: %s%n",newRunFileLines.size());
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

    public static Double combineScores(Double score1, Double score2) { return score1 * score2; }

    public static Map<String,Float> retrieveScoresFromFile(String filePath) throws IOException {
        Map<String,Float> scoreDocs = new HashMap<>();
        BufferedReader runFileReader = new BufferedReader(new FileReader(filePath));
        // for each line of the input run file ...
        for (String fileLine; (fileLine = runFileReader.readLine()) != null; ) {
            String[] lineParts = fileLine.split("\t");
            if(lineParts.length == 2){
                scoreDocs.put(lineParts[0], Float.valueOf(lineParts[1]));
            }
        }
        System.out.println("Scores retrieved");
        return scoreDocs;
    }

    public static void main (String[] args) throws IOException, ParseException {

        ArgumentQualityReranker.rerank(
                "./runs/seupd2122-kueri.txt",
                "./runs/",
                "reranked",
                "./document_quality_scores/scores.txt");
    }

}