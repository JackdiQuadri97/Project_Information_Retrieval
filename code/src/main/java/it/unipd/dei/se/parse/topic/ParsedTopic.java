package it.unipd.dei.se.parse.topic;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ParsedTopic {

    /**
     * The unique topic identifier.
     */
    private final String number;

    /**
     * The title of the topic.
     */
    private final String title;

    /**
     * The objects of the topic.
     */
    private final String objects;

    /**
     * The description of the topic.
     */
    private final String description;

    /**
     * The narrative of the topic.
     */
    private final String narrative;


    /**
     * Creates a new parsed topic
     *
     * @param number      the unique topic identifier.
     * @param title       the title of the document.
     * @param objects     the objects of the topic.
     * @param description the description of the topic.
     * @param narrative   the narrative of the topic.
     * @throws NullPointerException  if {@code id} and/or {@code body} are {@code null}.
     * @throws IllegalStateException if {@code id} and/or {@code body} are empty.
     */
    public ParsedTopic(final String number, final String title, final String objects, final String description, final String narrative) {

        if (number == null) {
            throw new NullPointerException("Topic identifier cannot be null.");
        }

        if (number.isEmpty()) {
            throw new IllegalStateException("Topic identifier cannot be empty.");
        }

        this.number = number;


        if (title == null) {
            throw new IllegalStateException("Topic title cannot be null.");
        }

        if (title.isEmpty()) {
            throw new IllegalStateException("Topic title cannot be empty.");
        }

        this.title = title;


        if (objects == null) {
            throw new NullPointerException("Topic objects cannot be null.");
        }

        if (objects.isEmpty()) {
            throw new IllegalStateException("Topic objects cannot be empty.");
        }

        this.objects = objects;


        if (description == null) {
            throw new NullPointerException("Topic description cannot be null.");
        }

        if (description.isEmpty()) {
            throw new IllegalStateException("Topic description cannot be empty.");
        }

        this.description = description;


        if (narrative == null) {
            throw new NullPointerException("Topic narrative cannot be null.");
        }

        if (narrative.isEmpty()) {
            throw new IllegalStateException("Topic narrative cannot be empty.");
        }

        this.narrative = narrative;

    }


    /**
     * Returns the unique topic identifier.
     *
     * @return the unique topic identifier.
     */
    public String getNumber() {
        return number;
    }

    /**
     * Returns the title of the topic.
     *
     * @return the title of the topic.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the objects of the topic.
     *
     * @return the objects of the topic.
     */
    public String getObjects() {
        return objects;
    }

    /**
     * Returns the description of the topic.
     *
     * @return the description of the topic.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the narrative of the topic.
     *
     * @return the narrative of the topic.
     */
    public String getNarrative() {
        return narrative;
    }


    @Override
    public final String toString() {

        ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("Id", number)
                .append("Title", title)
                .append("objects", objects)
                .append("description", description)
                .append("narrative", narrative);

        return tsb.toString();
    }

    @Override
    public final boolean equals(Object o) {
        return (this == o) || ((o instanceof ParsedTopic) && number.equals(((ParsedTopic) o).number));
    }

    @Override
    public final int hashCode() {
        return 37 * number.hashCode();
    }

}