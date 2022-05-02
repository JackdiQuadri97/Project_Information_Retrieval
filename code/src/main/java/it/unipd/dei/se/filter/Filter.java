package it.unipd.dei.se.filter;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class Filter {
    /**
     * It takes a string, splits it into tokens, and then creates a boolean query that requires all of those tokens to be
     * present in the document
     *
     * @param s the string to be tokenized
     * @param queryParser The query parser that will be used to parse the query.
     * @return A BooleanQuery.Builder object
     */
    public static BooleanQuery.Builder filterAnd(String s, QueryParser queryParser) {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        List<String> tokens = getTokensWithCollection(s);
        for (String token : tokens) {
            try {
    /**
     * It takes a string, splits it into tokens, and then creates a boolean query with each token as a clause
     *
     * @param s the string to be tokenized
     * @param queryParser The query parser that will be used to parse the query.
     * @return A BooleanQuery.Builder object
     */
                booleanQuery.add(queryParser.parse(QueryParserBase.escape(token)), BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return booleanQuery;
    }

    /**
     * It takes a string, splits it into tokens, and then creates a boolean query with each token as a clause
     *
     * @param s the string to be tokenized
     * @param queryParser The query parser that will be used to parse the query.
     * @return A BooleanQuery.Builder object
     */
    public static BooleanQuery.Builder filterOr(String s, QueryParser queryParser) {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        List<String> tokens = getTokensWithCollection(s);
        for (String token : tokens) {
            try {
                booleanQuery.add(queryParser.parse(QueryParserBase.escape(token)), BooleanClause.Occur.SHOULD);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return booleanQuery;
    }

    /**
     * It takes a string, removes all commas from it, splits it into tokens, and returns a list of those tokens
     *
     * @param str The string to be tokenized.
     * @return A list of tokens
     */
    public static List<String> getTokensWithCollection(String str) {
        str = str.replaceAll(",", "");
        return Collections.list(new StringTokenizer(str, " ")).stream()
                .map(token -> (String) token)
                .collect(Collectors.toList());
    }
}
