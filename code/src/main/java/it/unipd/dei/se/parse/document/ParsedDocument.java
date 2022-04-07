package it.unipd.dei.se.parse.document;

import com.beust.jcommander.internal.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ParsedDocument {

    /**
     * The contents of the document.
     */
    private final String docT5Query;

    /**
     * The unique document identifier.
     */
    private final String id;
    /**
     * The contents of the document.
     */
    private final String contents;

    /**
     * Creates a new parsed document
     *
     * @param id         the unique document identifier.
     * @param contents   the contents of the document.
     * @param docT5Query the DocT5Query expansion.
     * @throws NullPointerException  if {@code id} and/or {@code body} are {@code null}.
     * @throws IllegalStateException if {@code id} and/or {@code body} are empty.
     */
    public ParsedDocument(final String id, final String contents, @Nullable final String docT5Query) {

        if (id == null) {
            throw new NullPointerException("Document identifier cannot be null.");
        }

        if (id.isEmpty()) {
            throw new IllegalStateException("Document identifier cannot be empty.");
        }

        this.id = id;


        if (contents == null) {
            throw new NullPointerException("Document contents cannot be null.");
        }

        if (contents.isEmpty()) {
            throw new IllegalStateException("Document contents cannot be empty.");
        }

        this.contents = contents;

        this.docT5Query = docT5Query;

    }

    /**
     * Returns the DocT5Query of the document.
     *
     * @return the DocT5Query of the document.
     */
    public String getDocT5Query() {
        return contents;
    }


    /**
     * Returns the unique document identifier.
     *
     * @return the unique document identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the contents of the document.
     *
     * @return the contents of the document.
     */
    public String getContents() {
        return contents;
    }

    @Override
    public final String toString() {

        ToStringBuilder tsb;

        tsb = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("Id", id)
                .append("Contents", contents)
                .append("DocT5Query", docT5Query);

        return tsb.toString();
    }

    public final static class FIELDS {

        /**
         * The name of the element containing the document identifier.
         */
        public static final String ID = "id";

        /**
         * The name of the element containing the document contents.
         */
        public static final String CONTENTS = "contents";

        /**
         * The name of the element containing the DocT5Query expansion.
         */
        public static final String DOC_T5_QUERY = "docT5Query";


    }

    @Override
    public final boolean equals(Object o) {
        return (this == o) || ((o instanceof ParsedDocument) && id.equals(((ParsedDocument) o).id));
    }

    @Override
    public final int hashCode() {
        return 37 * id.hashCode();
    }

}