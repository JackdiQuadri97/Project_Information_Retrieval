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

        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        argsList.add("code/src/main/resource/qrels/example.txt");

        List<List<Map<String, Integer>>> termFreq = getTermFrequencies(argsList);

        search(termFreq);
        System.out.println(termFreq);

    }

    public static void search(List<List<Map<String, Integer>>> termFreq) throws IOException, ParseException {

        System.out.printf("%n#### Start searching ####%n");

        // the start time of the searching
        final long start = System.currentTimeMillis();
        final Set<String> idField = new HashSet<>();
        idField.add(ParsedDocument.FIELDS.ID);

        BooleanQuery.Builder queryBuilder;
        Query query;
        TopDocs topDocsObject;
        ScoreDoc[] topDocs;
        String docID;

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("experiment/index")));
        IndexSearcher searcher = new IndexSearcher(reader);
        Writer output = new BufferedWriter(new FileWriter("runs/RF.txt"));  //clears file every time

        try {
            // SEARCHING
            for (int i = 0; i < termFreq.size(); i++) {

                System.out.printf("Searching for topic %s.%n", i);
                List<Map<String, Integer>> topicRelevance = termFreq.get(i);
                queryBuilder = new BooleanQuery.Builder();
                Analyzer analyzer = new StandardAnalyzer();
                QueryParser queryParser = new QueryParser("contents", analyzer);

                for (int j = 0; j < topicRelevance.size(); j++) {
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
                    output.append(String.format(Locale.ENGLISH, "%s\tQ0\t%s\t%d\t%.6f\t%s%n", i, docID, m + 1, topDocs[m].score,
                            "RF"));
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

    private static List<List<Map<String, Integer>>> getTermFrequencies(List<String> runs) throws IOException {
        List<List<Map<String, Integer>>> topicsRfs = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            List<Map<String, Integer>> relevance = new ArrayList<>();
            for (int j = 0; j < 4; j++)
                relevance.add(new HashMap<>());
            topicsRfs.add(relevance);
        }

        for (int i = 0; i < runs.size(); i++) {
            BufferedReader br = new BufferedReader(new FileReader(runs.get(0)));
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
                    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("experiment/index")));
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

        }

        return topicsRfs;
    }
}