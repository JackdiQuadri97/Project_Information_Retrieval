package it.unipd.dei.se.parse.document;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;


public class Parser extends DocumentParser {

    private ParsedDocument document = null;

    private static final int BODY_SIZE = 1024 * 8;

    private final ObjectMapper objectMapper;
    private final JsonParser jsonParser;

    private boolean startedReading = false;

    public Parser(Reader in) {
        super(new BufferedReader(in));
        objectMapper = new ObjectMapper();
        try {
            jsonParser = objectMapper.createParser(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read from Reader", e);
        }
    }

    @Override
    protected ParsedDocument parse() {
        return document;
    }

    @Override
    public boolean hasNext() {
        try {
//            if (!startedReading) {
//                startedReading = true;
//                while (jsonParser.nextToken() != JsonToken.FIELD_NAME || !jsonParser.getCurrentName().equals("arguments"))
//                    ; //empty while body
//                if (jsonParser.nextToken() != JsonToken.START_ARRAY)
//                    throw new IllegalArgumentException("should be an array");
//                jsonParser.nextToken();
//            } else if (jsonParser.nextToken() == JsonToken.END_ARRAY) {
//                // end of the documents for this file
//                jsonParser.close();
//                in.close();
//                return false;
//            }

            if (jsonParser.getCurrentName() == null)
                jsonParser.nextToken();

            if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
                System.err.printf("--> %s%n", jsonParser.currentToken());
                throw new IllegalArgumentException("should be an object");
            }


            //now we are at the start of the documents array
            //and the current token is '{'

            final JsonNode root = objectMapper.readTree(jsonParser);
            if (root == null) throw new IllegalArgumentException("Not valid json");

            final String id;
            if (root.hasNonNull("id")) id = root.get("id").asText();
            else throw new IllegalArgumentException("No valid id");

            String contents = "";
            if (root.hasNonNull("contents")) contents = root.get("contents").asText();
            else throw new IllegalArgumentException("No valid contents");

            String chatNoirUrl = "";
            if (root.hasNonNull("chatNoirUrl")) chatNoirUrl = root.get("chatNoirUrl").asText();
            else throw new IllegalArgumentException("No valid chatNoirUrl");

            document = new ParsedDocument(id, contents, chatNoirUrl);

            return true;
        } catch (IOException e) {
            throw new IllegalArgumentException("Read failed", e);
        }
    }
}