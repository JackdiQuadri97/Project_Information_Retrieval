

package it.unipd.dei.se.index;


import it.unipd.dei.se.parse.document.ParsedDocument;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

/**
 * Represents a {@link Field} for containing the body of a document.
 * <p>
 * It is a tokenized field, not stored, keeping only document ids and term frequencies (see {@link
 * IndexOptions#DOCS_AND_FREQS} in order to minimize the space occupation.
 */
public class DocT5QueryField extends Field {

    /**
     * The type of the document body field
     */
    private static final FieldType DOCT5QUERY_TYPE = new FieldType();

    static {
        DOCT5QUERY_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        //tokenized
        DOCT5QUERY_TYPE.setTokenized(true);
        //do not store in memory
        DOCT5QUERY_TYPE.setStored(false);
    }


    /**
     * Create a new field for the body of a document.
     *
     * @param value the contents of the body of a document.
     */
    /*public DocT5QueryField(final Reader value) {
        super(ParsedDocument.FIELDS.DOCT5QUERY, value, DOCT5QUERY_TYPE);
    }*/

    /**
     * Create a new field for the body of a document.
     *
     * @param value the contents of the body of a document.
     */
    public DocT5QueryField(final String value) {
        super(ParsedDocument.FIELDS.DOC_T5_QUERY, value, DOCT5QUERY_TYPE);
    }

}


