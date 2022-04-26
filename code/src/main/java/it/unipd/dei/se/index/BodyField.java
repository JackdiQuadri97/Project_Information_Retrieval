

package it.unipd.dei.se.index;


import it.unipd.dei.se.parse.document.ParsedDocument;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import java.io.Reader;

/**
 * Represents a {@link Field} for containing the body of a document.
 * <p>
 * It is a tokenized field, not stored, keeping only document ids and term frequencies (see {@link
 * IndexOptions#DOCS_AND_FREQS} in order to minimize the space occupation.
 */
public class BodyField extends Field {

    /**
     * The type of the document body field
     */
    private static final FieldType BODY_TYPE = new FieldType();

    static {
        BODY_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        //tokenized
        BODY_TYPE.setTokenized(true);
        //do not store in memory
        BODY_TYPE.setStored(true);
        //storing termvectors
        BODY_TYPE.setStoreTermVectors(true);
    }


    /**
     * Create a new field for the body of a document.
     *
     * @param value the contents of the body of a document.
     */
    public BodyField(final Reader value) {
        super(ParsedDocument.FIELDS.CONTENTS, value, BODY_TYPE);
    }


    /**
     * Create a new field for the body of a document.
     *
     * @param value the contents of the body of a document.
     */
    public BodyField(final String value) {
        super(ParsedDocument.FIELDS.CONTENTS, value, BODY_TYPE);
    }
}