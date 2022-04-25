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
    public static BooleanQuery.Builder filterAnd(String s, QueryParser queryParser) {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        List<String> tokens = getTokensWithCollection(s);
        for (String token : tokens) {
            try {
                booleanQuery.add(queryParser.parse(QueryParserBase.escape(token)), BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return booleanQuery;
    }

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

    public static List<String> getTokensWithCollection(String str) {
        str = str.replaceAll(",", "");
        return Collections.list(new StringTokenizer(str, " ")).stream()
                .map(token -> (String) token)
                .collect(Collectors.toList());
    }
}
