package it.unipd.dei.se.rf;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class RF {
    public static void main(String[] args) throws IOException {

        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        argsList.add("code/src/main/resource/qrels/example.txt");

        //TODO: Divide rfs by relevance too: (topics->rel->terms&freq or rel->topics->terms&freq)
        List<Map<String, Double>> rfs = new ArrayList<>();
        for (int i = 0; i < 101; i++)
            rfs.add(new HashMap<>());

        for (int i = 0; i < argsList.size(); i++) {
            BufferedReader br = new BufferedReader(new FileReader(argsList.get(0)));
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
                    BytesRef term = null;
                    PostingsEnum postings = null;
                    while((term = iterator.next()) != null){
                        String termText = term.utf8ToString();
                        postings = iterator.postings(postings, PostingsEnum.FREQS);
                        postings.nextDoc();
                        int freq = postings.freq();
                        System.out.println(termText +" : "+ freq);
                        /*TODO: add to the hashmap, for each topic and according to relevance, the termText and the
                           respective sum of freq (the times the term appears in all the relevant documents for
                            that topic); this is done similarmly to rrf*/
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                } finally {

                }
            }

        }
    }
}