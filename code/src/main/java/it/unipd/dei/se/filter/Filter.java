package it.unipd.dei.se.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class Filter {

    public static BooleanQuery.Builder filterAnd(String s) {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        List<String> tokens = getTokensWithCollection(s);
        for (String token : tokens) {
            TermQuery tokenQuery = new TermQuery(new Term(token, token));
            booleanQuery.add(new BooleanClause(tokenQuery, BooleanClause.Occur.MUST));
        }
        return booleanQuery;
    }

    public static BooleanQuery.Builder filterOr(String s) {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        List<String> tokens = getTokensWithCollection(s);
        for (String token : tokens) {
            TermQuery tokenQuery = new TermQuery(new Term(token, token));
            booleanQuery.add(new BooleanClause(tokenQuery, BooleanClause.Occur.SHOULD));
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
