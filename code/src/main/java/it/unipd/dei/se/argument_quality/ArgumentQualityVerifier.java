package it.unipd.dei.se.argument_quality;

/* (C) Copyright IBM Corp. 2020. */

import com.ibm.hrl.debater.clients.DebaterApi;
import com.ibm.hrl.debater.clients.SentenceTopicPair;
import com.ibm.hrl.debater.clients.argument_quality.ArgumentQualityClient;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Class to obtain scores from Project Debater API
 */
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


        List<SentenceTopicPair> sentenceTopicPairs = Arrays.asList(
                new SentenceTopicPair("Cars should only provide assisted driving, not complete autonomy.","We should further explore the development of autonomous vehicles"),
                new SentenceTopicPair("Cars cars cars cars who cares","We should further explore the development of autonomous vehicles"),
                new SentenceTopicPair("that he given sun roads sea","We should further explore the development of autonomous vehicles"));

        ArgumentQualityVerifier argumentQualityVerifier = new ArgumentQualityVerifier();

        List<Float> scores = argumentQualityVerifier.computeScores(sentenceTopicPairs);

        System.out.println("Pairs of (sentence, topic) and their argument-quality scores:\n");
        for (int i = 0; i < sentenceTopicPairs.size(); i++){
            System.out.println(String.format("Sentence: %s\nTopic: %s\nScore: %.4f\n",
                    sentenceTopicPairs.get(i).getSentence(), sentenceTopicPairs.get(i).getTopic(),scores.get(i)));
        }
    }

}