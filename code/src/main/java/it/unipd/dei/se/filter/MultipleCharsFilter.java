package it.unipd.dei.se.filter;


import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

public class MultipleCharsFilter extends TokenFilter {
    private final CharTermAttribute charTermAttr;

    /**
     * Create new instance of the MultipleCharsFilter
     *
     * @param ts the token stream
     */
    public MultipleCharsFilter(TokenStream ts) {
        super(ts);
        this.charTermAttr = addAttribute(CharTermAttribute.class);
    }

    /**
     * If the current token is not the last token, then check if the current token is a triple character, and if it is,
     * then remove it
     *
     * @return The token is being returned.
     */
    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }

        int length = charTermAttr.length();
        char[] buffer = charTermAttr.buffer();
        char[] newBuffer = new char[length];

        int j = length;
        if (length > 2) {
            newBuffer[0] = buffer[0];
            newBuffer[1] = buffer[1];
            j = 2;

            for (int i = 2; i < length; i++) {
                if (!(buffer[i - 2] == buffer[i - 1] && buffer[i - 1] == buffer[i])) {
                    newBuffer[j] = buffer[i];
                    j++;
                }
            }
        } else {
            newBuffer = buffer.clone();
        }

        charTermAttr.setEmpty();
        charTermAttr.copyBuffer(newBuffer, 0, j);
        return true;
    }
}