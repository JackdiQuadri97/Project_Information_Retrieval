package it.unipd.dei.se.argument_quality;

/* (C) Copyright IBM Corp. 2020. */

import com.ibm.hrl.debater.clients.DebaterApi;
import com.ibm.hrl.debater.clients.SentenceTopicPair;
import com.ibm.hrl.debater.clients.argument_quality.ArgumentQualityClient;
import it.unipd.dei.se.parse.document.DocumentParser;
import it.unipd.dei.se.parse.document.ParsedDocument;
import it.unipd.dei.se.parse.document.Parser;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ArgumentQualityVerifier {

    private final String apiKey;
    private final ArgumentQualityClient argumentQualityClient;

    public ArgumentQualityVerifier(String apiKeyPath, String apiKeyPropertyName) throws IOException {
        // read secret
        File f = new File(apiKeyPath);
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        Properties prop = new Properties();
        prop.load(is);


        // set properties
        this.apiKey = prop.getProperty(apiKeyPropertyName);
        this.argumentQualityClient =  DebaterApi.builder().build().getArgumentQualityClient();
    }

    public ArgumentQualityVerifier() throws IOException {
        this("./secrets/secrets.txt", "DEBATER_API_KEY");
    }
    public List<Float> computeScores(List<SentenceTopicPair> sentenceTopicPairs) throws IOException {
        return this.argumentQualityClient.getScores(sentenceTopicPairs, this.apiKey);
    }
    public static void main (String[] args) throws IOException {

        String Topic = "";
        final String docsPath = "code/src/main/resource/corpus_folder";
        final Path docsDir = Paths.get(docsPath);
        final String extension = "jsonl";
        final Class<? extends DocumentParser> dpCls = Parser.class;
        final String charsetName = "ISO-8859-1";
        final Charset cs = Charset.forName(charsetName);
        final String runPath = "document_quality_scores";
        final Path runDir = Paths.get(runPath);
        Path runFile = runDir.resolve("scores.txt");
        final PrintWriter run = new PrintWriter(Files.newBufferedWriter(runFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE));


        Files.walkFileTree(docsDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(extension)) {

                    DocumentParser dp = DocumentParser.create(dpCls, Files.newBufferedReader(file, cs));
                    int i=0;
                    List<SentenceTopicPair> sentenceTopicPairs = new ArrayList<>();
                    List<String> Id = new ArrayList<>();
                    for (ParsedDocument pd : dp) {
                        Id.add(pd.getId());
                        sentenceTopicPairs.add(new SentenceTopicPair(pd.getContents().replace("\\", ""),Topic));
                        i++;
                        if(i%100==0){
                            System.out.printf("Inputted doc number: %d%n", i);
                        }
                        if (i%1000==0){
                            ArgumentQualityVerifier argumentQualityVerifier = new ArgumentQualityVerifier();
                            List<Float> scores = argumentQualityVerifier.computeScores(sentenceTopicPairs);
                            for (int j=0;j<1000;j++){
                                run.printf(Locale.ENGLISH, "%s\t%.6f%n", Id.get(j), scores.get(j));
                            }
                            run.flush();
                            sentenceTopicPairs = new ArrayList<>();
                            Id = new ArrayList<>();
                        }
                    }
                    ArgumentQualityVerifier argumentQualityVerifier = new ArgumentQualityVerifier();
                    List<Float> scores = argumentQualityVerifier.computeScores(sentenceTopicPairs);
                    for (int j = 0; j<i%1000; j++) {
                        run.printf(Locale.ENGLISH, "%s\t%.6f%n", Id.get(j), scores.get(j));
                    }
                    run.flush();
                } else {
                    //here i notify if i skip a file
                    System.out.printf("Ignoring file: %s", file.getFileName());


                }

                return FileVisitResult.CONTINUE;
            }
        });
        run.close();
    }

}