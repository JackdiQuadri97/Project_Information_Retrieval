package it.unipd.dei.se.analyze;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.IOException;
import java.io.Reader;

import static it.unipd.dei.se.analyze.AnalyzerUtil.consumeTokenStream;

public class BaselineAnalyzer extends Analyzer {
    public BaselineAnalyzer() {
        super();
    }

    protected TokenStreamComponents createComponents(String fieldName) {

        final Tokenizer source = new StandardTokenizer();

        TokenStream tokens = new LowerCaseFilter(source);

        tokens = new StopFilter(tokens, AnalyzerUtil.loadStopList("lucene.txt"));

        return new TokenStreamComponents(source, tokens);
    }

    /**
     * If you want to strip HTML tags from your text, uncomment the line that returns the HTMLStripCharFilter.
     *
     * @param fieldName The name of the field being indexed.
     * @param reader the Reader object that is being used to read the contents of the file
     * @return The reader is being returned.
     */
    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        // return new HTMLStripCharFilter(reader);

        return super.initReader(fieldName, reader);
    }

    /**
     * If the field is 'title', then return a new LowerCaseFilter(in)
     *
     * @param fieldName The name of the field being analyzed.
     * @param in The token stream to be filtered.
     * @return A new LowerCaseFilter object.
     */
    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }

    /**
     * Main method of the class.
     *
     * @param args command line arguments.
     * @throws IOException if something goes wrong while processing the text.
     */
    public static void main(String[] args) throws IOException {

        // text to analyze
        final String text = "I now live in Rome where I met my wife Alice back in 2010 during a beautiful afternoon. " + "Occasionally, I fly to New York to visit the United Nations where I would like to work. The last " + "time I was there in March 2019, the flight was very inconvenient, leaving at 4:00 am, and expensive," + " over 1,500 dollars.";

        //final String text = "This is my simple test."+"I'm testing it.";
        // use the analyzer to process the text and print diagnostic information about each token
        consumeTokenStream(new BaselineAnalyzer(), text);


    }
} 
