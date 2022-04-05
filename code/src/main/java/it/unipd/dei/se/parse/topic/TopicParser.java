package it.unipd.dei.se.parse.topic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class TopicParser implements Iterator<ParsedTopic>, Iterable<ParsedTopic> {

    /**
     * A LOGGER available for all the subclasses.
     */
    protected static final Logger LOGGER = LogManager.getLogger(TopicParser.class);

    /**
     * Indicates whether there is another {@code ParsedTopic} to return.
     */
    protected boolean next;

    /**
     * The reader to be used to parse document(s).
     */
    protected XMLEventReader in;


    /**
     * Creates a new document parser.
     *
     * @param in the reader to the document(s) to be parsed.
     * @throws NullPointerException if {@code in} is {@code null}.
     */
    protected TopicParser(final Reader in) {

        if (in == null) {
            throw new NullPointerException("Reader cannot be null.");
        }

        XMLInputFactory XIF = XMLInputFactory.newInstance();
        XIF.setProperty(XMLInputFactory.IS_COALESCING, true);

        try {
            this.in = XIF.createXMLEventReader(in);
        } catch (XMLStreamException e) {
            LOGGER.error("Unable to instantiate the XML document parser.");
            throw new IllegalStateException("Unable to instantiate the XML document parser.");
        }
    }

    @Override
    public final Iterator<ParsedTopic> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return next;
    }

    @Override
    public final ParsedTopic next() {

        if (!next) {
            throw new NoSuchElementException("No more topics to parse.");
        }

        try {
            return parse();
        } finally {
            try {
                // we reached the end of the file
                if (!next) {
                    in.close();
                }
            } catch (XMLStreamException e) {
                LOGGER.error("Unable to close the XML document.", e);
                throw new IllegalStateException("Unable to close the reader.", e);
            }
        }

    }

    /**
     * Performs the actual parsing of the topic.
     *
     * @return the parsed topic.
     */
    protected abstract ParsedTopic parse();

}