package it.unipd.dei.se;

import it.unipd.dei.se.parse.document.DocumentParser;
import it.unipd.dei.se.parse.document.ParsedDocument;
import it.unipd.dei.se.parse.document.Parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SanityCheck {
    public static void main(String[] args) throws IOException {
        System.out.println("Seems alright!");
        System.out.println("Testing parser");

        DocumentParser dp = DocumentParser.create(Parser.class, Files.newBufferedReader(Path.of("code/src/main/resources/test-expanded.jsonl"), StandardCharsets.ISO_8859_1));
        if (dp.hasNext()) {
            ParsedDocument parsedDocument = dp.next();
            System.out.println("Parsed document:");
            System.out.println(parsedDocument);
        } else {
            System.out.println("No documents to parse");
        }
    }
}
