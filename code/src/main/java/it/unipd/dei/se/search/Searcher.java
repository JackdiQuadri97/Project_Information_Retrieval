

package it.unipd.dei.se.search;

import it.unipd.dei.se.parse.document.ParsedDocument;
import it.unipd.dei.se.parse.topic.ParsedTopic;
import it.unipd.dei.se.parse.topic.XMLTopicParser;
import opennlp.tools.parser.Parse;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Searcher {

    private static final class TOPIC_FIELDS {

        /**
         * The title of a topic.
         */
        public static final String TITLE = "title";

        /**
         * The description of a topic.
         */
        public static final String DESCRIPTION = "description";

        /**
         * The narrative of a topic.
         */
        public static final String NARRATIVE = "narrative";
    }


    /**
     * The identifier of the run
     */
    private final String runID;

    /**
     * The run to be written
     */
    private final PrintWriter run;

    /**
     * The index reader
     */
    private final IndexReader reader;

    /**
     * The index searcher.
     */
    private final IndexSearcher searcher;

    /**
     * The topics to be searched
     */
    private final ParsedTopic[] topics;

    /**
     * The query parser
     */
    private final QueryParser queryParser;

    /**
     * The maximum number of documents to retrieve
     */
    private final int maxDocsRetrieved;

    /**
     * The total elapsed time.
     */
    private long elapsedTime = Long.MIN_VALUE;


    /**
     * Creates a new searcher.
     *
     * @param analyzer         the {@code Analyzer} to be used.
     * @param similarity       the {@code Similarity} to be used.
     * @param indexPath        the directory where containing the index to be searched.
     * @param topicsFile       the file containing the topics to search for.
     * @param expectedTopics   the total number of topics expected to be searched.
     * @param runID            the identifier of the run to be created.
     * @param runPath          the path where to store the run.
     * @param maxDocsRetrieved the maximum number of documents to be retrieved.
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public Searcher(final Analyzer analyzer, final Similarity similarity, final String indexPath,
                    final String topicsFile, final int expectedTopics, final String runID, final String runPath,
                    final int maxDocsRetrieved) {

        if (analyzer == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be read.", indexDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to search the index.",
                    indexDir.toAbsolutePath().toString()));
        }

        try {
            reader = DirectoryReader.open(FSDirectory.open(indexDir));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index reader for directory %s: %s.",
                    indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);

        if (topicsFile == null) {
            throw new NullPointerException("Topics file cannot be null.");
        }

        if (topicsFile.isEmpty()) {
            throw new IllegalArgumentException("Topics file cannot be empty.");
        }

        try {
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
            this.topics = tempList.toArray(new ParsedTopic[0]);


            in.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to process topic file %s: %s.", topicsFile, e.getMessage()), e);
        }

        if (expectedTopics <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of topics to be searched cannot be less than or equal to zero.");
        }

        if (topics.length != expectedTopics) {
            System.out.printf("Expected to search for %s topics; %s topics found instead.", expectedTopics,
                    topics.length);
        }


        Map<String, Float> weights = new HashMap<>();
        weights.put(ParsedDocument.FIELDS.CONTENTS, 1.0f);
        weights.put(ParsedDocument.FIELDS.DOC_T5_QUERY, 1.0f);
        // (when testing with an index that was not produced by this system,
        // put here the name of the field of the document in which you want to search)
        // Defines in which fields of the documents to search.
        // Use MultiFieldQueryParser to search in multiple fields.
        // queryParser = new QueryParser(ParsedDocument.FIELDS.CONTENTS, analyzer);
        queryParser = new MultiFieldQueryParser(
                new String[]{
                        ParsedDocument.FIELDS.CONTENTS,
                        ParsedDocument.FIELDS.DOC_T5_QUERY
                },
                analyzer,
                weights);

        if (runID == null) {
            throw new NullPointerException("Run identifier cannot be null.");
        }

        if (runID.isEmpty()) {
            throw new IllegalArgumentException("Run identifier cannot be empty.");
        }

        this.runID = runID;


        if (runPath == null) {
            throw new NullPointerException("Run path cannot be null.");
        }

        if (runPath.isEmpty()) {
            throw new IllegalArgumentException("Run path cannot be empty.");
        }

        final Path runDir = Paths.get(runPath);
        if (!Files.isWritable(runDir)) {
            throw new IllegalArgumentException(
                    String.format("Run directory %s cannot be written.", runDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(runDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the run.",
                    runDir.toAbsolutePath().toString()));
        }

        Path runFile = runDir.resolve(runID + ".txt");
        try {
            run = new PrintWriter(Files.newBufferedWriter(runFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to open run file %s: %s.", runFile.toAbsolutePath(), e.getMessage()), e);
        }

        if (maxDocsRetrieved <= 0) {
            throw new IllegalArgumentException(
                    "The maximum number of documents to be retrieved cannot be less than or equal to zero.");
        }

        this.maxDocsRetrieved = maxDocsRetrieved;
    }

    /**
     * Returns the total elapsed time.
     *
     * @return the total elapsed time.
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * /** Searches for the specified topics.
     *
     * @throws IOException    if something goes wrong while searching.
     * @throws ParseException if something goes wrong while parsing topics.
     */
    public void search() throws IOException, ParseException {

        System.out.printf("%n#### Start searching ####%n");

        // the start time of the searching
        final long start = System.currentTimeMillis();
        final Set<String> idField = new HashSet<>();
        idField.add(ParsedDocument.FIELDS.ID);

        BooleanQuery.Builder queryBuilder = null;
        Query query = null;
        TopDocs topDocsObject = null;
        ScoreDoc[] topDocs = null;
        String docID = null;

        try {
            // SEARCHING
            for (ParsedTopic topic : topics) {

                System.out.printf("Searching for topic %s.%n", topic.getNumber());

                queryBuilder = new BooleanQuery.Builder();

                // define the terms to put in the query and if they SHOULD or MUST be present
                queryBuilder.add(queryParser.parse(QueryParserBase.escape(topic.getTitle())), BooleanClause.Occur.SHOULD);
                /*queryBuilder.add(queryParser.parse(QueryParserBase.escape(topic.getDescription())),
                        BooleanClause.Occur.SHOULD);
                queryBuilder.add(queryParser.parse(QueryParserBase.escape(topic.getObjects())), BooleanClause.Occur.SHOULD);*/

                query = queryBuilder.build();

                topDocsObject = searcher.search(query, maxDocsRetrieved);

                topDocs = topDocsObject.scoreDocs;

                // OUTPUT
                // adding the retrieved documents for this topic to the run file
                for (int i = 0, n = topDocs.length; i < n; i++) {
                    docID = reader.document(topDocs[i].doc, idField).get(ParsedDocument.FIELDS.ID);

                    run.printf(Locale.ENGLISH, "%s\tQ0\t%s\t%d\t%.6f\t%s%n", topic.getNumber(), docID, i+1, topDocs[i].score,
                            runID);
                    Writer output = new BufferedWriter(new FileWriter(runID));  //clears file every time
                    output.append(String.format(Locale.ENGLISH, "%s\tQ0\t%s\t%d\t%.6f\t%s%n", topic.getNumber(), docID, i+1, topDocs[i].score,
                            runID));
                    output.close();
                }

                run.flush();

            }
        } finally {
            run.close();

            reader.close();
        }

        elapsedTime = System.currentTimeMillis() - start;

        System.out.printf("%d topic(s) searched in %d seconds.%n", topics.length, elapsedTime / 1000);

        System.out.printf("#### Searching complete ####%n");
    }

    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        final String topics = "code/src/main/resource/topics-task2.xml";

        final String indexPath = "experiment/index";

        final String runPath = "experiment";

        final String runID = "seupd2122-kueri";

        final int maxDocsRetrieved = 1000;

        final Analyzer analyzer = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).build();

        Searcher s = new Searcher(analyzer, new BM25Similarity(), indexPath, topics, 50, runID, runPath, maxDocsRetrieved);

        s.search();


    }

}
