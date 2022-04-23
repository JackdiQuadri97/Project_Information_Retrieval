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

    private final ObjectMapper objectMapper;
    private final JsonParser jsonParser;

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

            if (jsonParser.getCurrentName() == null)
                jsonParser.nextToken();

            if (jsonParser.currentToken() != null && jsonParser.currentToken() != JsonToken.START_OBJECT) {
                System.err.printf("--> %s%n", jsonParser.currentToken());
                throw new IllegalArgumentException("should be an object");
            }


            //now we are at the start of the documents array
            //and the current token is '{'

            final JsonNode root = objectMapper.readTree(jsonParser);

            if (root == null || root.isEmpty())
                return false;

            final String id;
            if (root.hasNonNull("id")) id = root.get("id").asText();
            else throw new IllegalArgumentException("No valid id");

            String contents;
            if (root.hasNonNull("contents")) contents = root.get("contents").asText();
            else throw new IllegalArgumentException("No valid contents");

            String docT5Query = null;
            if (contents.contains("<query>")) {
                String[] contentsSplit = contents.split("<query>");
                contents = contentsSplit[0].strip();
                docT5Query = contentsSplit[1].replace("</query>", "").strip();
            }

            document = new ParsedDocument(id, contents, docT5Query);
            return true;

        } catch (IOException e) {
            throw new IllegalArgumentException("Read failed", e);
        }
    }
}