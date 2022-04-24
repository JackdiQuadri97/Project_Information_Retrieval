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

    static BooleanQuery filterAnd(String s) {
        BooleanQuery booleanQuery = null;
        List<String> tokens = getTokensWithCollection(s);
        for (String token : tokens) {
            TermQuery tokenQuery = new TermQuery(new Term(token, token));
            booleanQuery = new BooleanQuery.Builder().add(new BooleanClause(tokenQuery, BooleanClause.Occur.MUST)).build();
        }
        return booleanQuery;
    }

    static BooleanQuery filterOr(String s) {
        BooleanQuery booleanQuery = null;
        List<String> tokens = getTokensWithCollection(s);
        for (String token : tokens) {
            TermQuery tokenQuery = new TermQuery(new Term(token, token));
            booleanQuery = new BooleanQuery.Builder().add(new BooleanClause(tokenQuery, BooleanClause.Occur.MUST)).build();
        }
        return booleanQuery;
    }

    public static List<String> getTokensWithCollection(String str) {
        return Collections.list(new StringTokenizer(str, " ")).stream()
                .map(token -> (String) token)
                .collect(Collectors.toList());
    }
}
