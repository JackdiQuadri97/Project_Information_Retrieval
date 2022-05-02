package it.unipd.dei.se.rf;

import it.unipd.dei.se.parse.document.ParsedDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class RF {

    public static void main(String[] args) throws IOException, ParseException {


    }

    /**
     * It takes the index directory path, the output path, the run id, and the qrels file path, and it returns a list of
     * lists of maps of strings to integers
     *
     * @param indexDirectoryPath The path to the directory where the index is stored.
     * @param outputPath The path to the output file.
     * @param runId The name of the run. This will be used to name the output file.
     * @param qrelsFilePath The path to the qrels file.
     */
    public static void doSearch(String indexDirectoryPath, String outputPath, String runId, String qrelsFilePath) throws IOException, ParseException {
        List<List<Map<String, Integer>>> termFreq = getTopicTermFrequencies(indexDirectoryPath, qrelsFilePath);

        search(indexDirectoryPath, outputPath, runId, termFreq);
    }

    /**
     * It takes in a list of lists of maps, where each list of maps represents a topic, and each map represents a term and
     * its frequency in that topic. It then searches the index for each topic, and outputs the results to a file
     *
     * @param indexDirectoryPath the path to the directory where the index is stored
     * @param outputPath the path to the folder where the run file will be saved
     * @param runId the name of the run file
     * @param termFreq a list of lists of maps. Each list of maps represents a topic. Each map represents a term and its
     * frequency.
     */
    public static void search(String indexDirectoryPath, String outputPath, String runId, List<List<Map<String, Integer>>> termFreq) throws IOException, ParseException {

        System.out.printf("%n#### Start searching ####%n");

        // the start time of the searching
        final long start = System.currentTimeMillis();
        final Set<String> idField = new HashSet<>();
        idField.add(ParsedDocument.FIELDS.ID);

        BooleanQuery.setMaxClauseCount(16384);
        BooleanQuery.Builder queryBuilder;
        Query query;
        TopDocs topDocsObject;
        ScoreDoc[] topDocs;
        String docID;

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Writer output = new BufferedWriter(new FileWriter(outputPath + "/" + runId + "_RF.txt"));  //clears file every time

        try {
            // SEARCHING
            for (int i = 0; i < termFreq.size(); i++) {

                System.out.printf("Searching for topic %s.%n", i);
                List<Map<String, Integer>> topicRelevance = termFreq.get(i);
                queryBuilder = new BooleanQuery.Builder();
                Analyzer analyzer = new StandardAnalyzer();
                QueryParser queryParser = new QueryParser("contents", analyzer);

                for (int j = 1; j < topicRelevance.size(); j++) {
                    Map<String, Integer> frequencies = topicRelevance.get(j);
                    for (String key : frequencies.keySet()) {
                        queryBuilder.add(queryParser.parse(key + "^" + (frequencies.get(key) * Math.pow(j, 2))), BooleanClause.Occur.SHOULD);
                    }
                }

                query = queryBuilder.build();
                topDocsObject = searcher.search(query, 1000);

                topDocs = topDocsObject.scoreDocs;


                // OUTPUT
                // adding the retrieved documents for this topic to the run file
                for (int m = 0, n = topDocs.length; m < n; m++) {
                    docID = reader.document(topDocs[m].doc, idField).get(ParsedDocument.FIELDS.ID);
                    output.append(String.format(Locale.ENGLISH, "%s Q0 %s %d %.6f %s%n", i, docID, m + 1, topDocs[m].score,
                            runId+"RF"));
                }

                output.flush();

            }
        } finally {
            output.close();
            reader.close();
        }

        long elapsedTime = System.currentTimeMillis() - start;

        System.out.printf("%d topic(s) searched in %d seconds.%n", termFreq.size(), elapsedTime / 1000);

        System.out.printf("#### Searching complete ####%n");
    }

    /**
     * It takes the index directory path and the qrels file path as input, and returns a list of lists of maps. The outer
     * list has 101 elements, one for each topic. The inner list has 4 elements, one for each relevance level. The map has
     * the term as the key and the term frequency as the value
     *
     * @param indexDirectoryPath The path to the directory where the index is stored.
     * @param qrelsFilePath The path to the qrels file.
     * @return A list of lists of maps. Each list of maps represents a topic. Each map represents a relevance level. Each
     * map contains the term frequencies for that relevance level.
     */
    private static List<List<Map<String, Integer>>> getTopicTermFrequencies(String indexDirectoryPath, String qrelsFilePath) throws IOException {
        List<List<Map<String, Integer>>> topicsRfs = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            List<Map<String, Integer>> relevance = new ArrayList<>();
            for (int j = 0; j < 4; j++)
                relevance.add(new HashMap<>());
            topicsRfs.add(relevance);
        }

        BufferedReader br = new BufferedReader(new FileReader(qrelsFilePath));
        for (String document; (document = br.readLine()) != null; ) {
            System.out.println(document);
            ArrayList<String> tokens = new ArrayList<>(Arrays.asList(document.split(" ")));
            int topicId = Integer.parseInt(tokens.get(0));
            String documentID = tokens.get(2);
            int relevance = Integer.parseInt(tokens.get(3));

            try {
                Analyzer analyzer = new WhitespaceAnalyzer();
                System.out.printf("Searching for topic %s.%n", topicId);

                BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
                QueryParser queryParser = new QueryParser("id", analyzer);

                queryBuilder.add(queryParser.parse(QueryParserBase.escape(String.valueOf(documentID))), BooleanClause.Occur.MUST);

                BooleanQuery query = queryBuilder.build();
                IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
                IndexSearcher searcher = new IndexSearcher(reader);

                TopDocs topDocsObject = searcher.search(query, 1);

                ScoreDoc[] topDocs = topDocsObject.scoreDocs;
                int doc;
                if (topDocs[0] != null)
                    doc = topDocs[0].doc;
                else continue;


                Terms termVector = reader.getTermVector(doc, "contents");
                TermsEnum iterator = termVector.iterator();
                BytesRef term;
                PostingsEnum postings = null;
                while ((term = iterator.next()) != null) {
                    String termText = term.utf8ToString();
                    postings = iterator.postings(postings, PostingsEnum.FREQS);
                    postings.nextDoc();
                    int freq = postings.freq();
                    topicsRfs.get(topicId).get(relevance).put(termText, freq);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


        return topicsRfs;
    }
}