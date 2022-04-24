package it.unipd.dei.se.analyze;

import it.unipd.dei.se.filter.MultipleCharsFilter;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.IOException;
import java.io.Reader;

import static it.unipd.dei.se.analyze.AnalyzerUtil.consumeTokenStream;

public class MainAnalyzer extends Analyzer {

    /**
     * Creates a new instance of the analyzer.
     */
    public MainAnalyzer() {
        super();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {

        final Tokenizer source = new StandardTokenizer();

        TokenStream tokens = new LowerCaseFilter(source);

        tokens = new EnglishPossessiveFilter(tokens);
        tokens = new MultipleCharsFilter(tokens);

        tokens = new StopFilter(tokens, AnalyzerUtil.loadStopList("atire.text"));

        return new TokenStreamComponents(source, tokens);
    }

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        // return new HTMLStripCharFilter(reader);

        return super.initReader(fieldName, reader);
    }

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
        consumeTokenStream(new MainAnalyzer(), text);


    }
}
