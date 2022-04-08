package it.unipd.dei.se;

import it.unipd.dei.se.parse.document.DocumentParser;
import it.unipd.dei.se.parse.document.ParsedDocument;
import it.unipd.dei.se.parse.document.Parser;
import it.unipd.dei.se.parse.topic.ParsedTopic;
import it.unipd.dei.se.parse.topic.XMLTopicParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SanityCheck {
    public static void main(String[] args) throws IOException {
        System.out.println("Testing document parser");
        DocumentParser dp = DocumentParser.create(Parser.class, Files.newBufferedReader(Path.of("code/src/main/resource/test-expanded.jsonl"), StandardCharsets.ISO_8859_1));
        if (dp.hasNext()) {
            ParsedDocument parsedDocument = dp.next();
            System.out.println("Parsed document:");
            System.out.println(parsedDocument);
        } else {
            System.out.println("No documents to parse");
        }

        System.out.println("Testing topic parser");
        XMLTopicParser tp = new XMLTopicParser(Files.newBufferedReader(Path.of("code/src/main/resource/topics-task2.xml"), StandardCharsets.ISO_8859_1));
        if (tp.hasNext()) {
            ParsedTopic parsedTopic = tp.next();
            System.out.println("Parsed topic:");
            System.out.println(parsedTopic);
        } else {
            System.out.println("No topics to parse");
        }
    }
}
