package it.unipd.dei.se.parse.topic;


import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.*;

public class XMLTopicParser extends TopicParser {

    /**
     * The name of the element containing the topic.
     */
    private static final String TOPIC_ELEMENT = "topic";
    /**
     * The name of the element containing the topic number.
     */
    private static final String NUMBER_ELEMENT = "number";
    /**
     * The name of the element containing the topic title.
     */
    private static final String TITLE_ELEMENT = "title";
    /**
     * The name of the element containing the topic objects.
     */
    private static final String OBJECTS_ELEMENT = "objects";
    /**
     * The name of the element containing the topic description.
     */
    private static final String DESCRIPTION_ELEMENT = "description";
    /**
     * The name of the element containing the topic description.
     */
    private static final String NARRATIVE_ELEMENT = "narrative";
    /**
     * The currently parsed XML event
     */
    private XMLEvent event = null;

    /**
     * Creates a new XML document parser.
     *
     * @param in the reader to the document(s) to be parsed.
     * @throws NullPointerException  if {@code in} is {@code null}.
     * @throws IllegalStateException if any error occurs while creating the parser.
     */
    public XMLTopicParser(final Reader in) {
        super(in);
    }

    /**
     * Creates a new trec format topic(s) file according to a provided XML topic(s) file.
     *
     * @param pathFile        the complete path to the XML topic(s) to be parsed.
     * @param tangentSResults the complete path to the Tangent-S results tsv file.
     * @return a string representing the complete path to the trec format topic(s) to be parsed.
     * @throws IllegalStateException if any error occurs while creating the trec format topics file.
     */
    public static String writeTrecTopics(String pathFile, String tangentSResults) {

        try {

            Reader reader = new FileReader(pathFile);
            XMLTopicParser topParser = new XMLTopicParser(reader);

            String path = pathFile.substring(0, pathFile.length() - 4);     // Take the file path without extension
            String txtPath = path + "_trec.txt";                            // Create the whole txt file path

            PrintWriter writer = new PrintWriter(txtPath);

            // Create a map representing the top-k relevant answers for the representative formula of each topic
            Map<String, List<String>> map = getTopKResults(tangentSResults, 500);

            for (ParsedTopic t : topParser) {
                writer.printf("<top>\n\n");
                // Number tag is the topic identifier
                writer.printf("<num> Number: %s\n", t.getNumber());
                // Title tag is the topic title
                writer.printf("<title> %s\n\n", t.getTitle());
                // Description tag is the topic question
                writer.printf("<desc> description:\n");
                writer.printf("%s\n\n", t.getDescription());
                writer.printf("<obj> objects:\n");
                writer.printf("%s\n\n", t.getObjects());
                writer.printf("<narr> narrative:\n");
                writer.printf("%s\n\n", t.getNarrative());
                writer.printf("</top>\n\n");
            }

            writer.close();
            return txtPath;

        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to open the XML topics file.", e);
            throw new IllegalStateException("Unable to open the XML topics file.", e);
        }
    }

    /**
     * Creates a map representing the top-k relevant answers for the representative formula of each topic.
     * The keys are the topic IDs and the values are lists of top k relevant answers for that formula topic.
     *
     * @param tangentSResults the complete path to the Tangent-S results tsv file.
     * @param k               the number of documents to be retrieved in the map.
     * @return a map representing the top-k relevant answers for the representative formula of each topic.
     * @throws IllegalStateException    if any error occurs during the parsing of the file.
     * @throws IllegalArgumentException if the parameter k is out of range (0,1000).
     */
    public static Map<String, List<String>> getTopKResults(String tangentSResults, int k) {

        if (k < 0 || k > 1000)
            throw new IllegalArgumentException("Integer parameter k must be in range (0,1000)");

        try {

            // tangentSResults line PATTERN: topicId docId rank score

            BufferedReader buffer = new BufferedReader(new FileReader(tangentSResults));

            // Data is stored in a map with topicIDs as keys and list of docIDs as values
            Map<String, List<String>> map = new HashMap<>();
            List<String> list = new ArrayList<>();

            StringTokenizer st;
            String line;
            String oldTopic = "";

            while ((line = buffer.readLine()) != null) {

                st = new StringTokenizer(line);
                // Take topicID from the line
                String topic = st.nextToken();

                // If the topic is not already parsed
                if (!topic.equals(oldTopic)) {

                    // Take docID from the line and add it to the array
                    String doc = st.nextToken();
                    list.add(doc);

                    // If we reached the end of the top-k results for that topic, add the entry to the map and create a new empty list
                    if (Integer.parseInt(st.nextToken()) == k) {
                        map.put(topic, list);
                        list = new ArrayList<>();
                        oldTopic = topic;
                    }
                }
            }

            buffer.close();
            return map;

        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to open the top-K results tsv file.", e);
            throw new IllegalStateException("Unable to open the top-K results tsv file.", e);
        } catch (IOException e) {
            LOGGER.error("Unable to parse the top-K results tsv file.", e);
            throw new IllegalStateException("Unable to parse the top-K results tsv file.", e);
        }
    }

    @Override
    public boolean hasNext() {

        try {

            // Assume that there are no more elements
            next = false;

            while (in.hasNext()) {

                // Take the next event
                event = in.nextEvent();

                if (event.isStartElement()) {
                    // Within an open tag
                    StartElement startElement = event.asStartElement();
                    if (startElement.getName().getLocalPart().equals(TOPIC_ELEMENT)) {
                        // Topic open tag, hence there is another topic to parse
                        next = true;
                        break;
                    }
                }
            }

        } catch (XMLStreamException e) {
            LOGGER.error("Unable to parse the XML document.", e);
            throw new IllegalStateException("Unable to parse the XML document.", e);
        }

        return next;
    }

    @Override
    protected final ParsedTopic parse() {

        String number = null;
        String title = null;
        String objects = null;
        String description = null;
        String narrative = null;

        StartElement startElement;
        EndElement endElement;

        try {
            while (in.hasNext()) {
                event = in.nextEvent();
                if (event.isStartElement()) {
                    startElement = event.asStartElement();
                    if (startElement.getName().getLocalPart().equals(NUMBER_ELEMENT)) {
                        event = in.nextEvent();
                        number = event.asCharacters().getData();
                    }
                    if (startElement.getName().getLocalPart().equals(TITLE_ELEMENT)) {
                        event = in.nextEvent();
                        title = event.asCharacters().getData();
                    } else if (startElement.getName().getLocalPart().equals(OBJECTS_ELEMENT)) {
                        event = in.nextEvent();
                        objects = event.asCharacters().getData();
                    } else if (startElement.getName().getLocalPart().equals(DESCRIPTION_ELEMENT)) {
                        event = in.nextEvent();
                        description = event.asCharacters().getData();
                    } else if (startElement.getName().getLocalPart().equals(NARRATIVE_ELEMENT)) {
                        event = in.nextEvent();
                        narrative = event.asCharacters().getData();
                    }
                } else if (event.isEndElement()) { // If we find the closure of tag Topic, we stop parsing
                    endElement = event.asEndElement();
                    if (endElement.getName().getLocalPart().equals(TOPIC_ELEMENT)) break;
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Unable to parse the XML document.", e);
            throw new IllegalStateException("Unable to parse the XML document.", e);
        }

        return new ParsedTopic(number, title, objects, description, narrative);
    }

}