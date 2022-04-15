package it.unipd.dei.se.rrf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class RRF {
    public static void main(String[] args) {
        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        int k = 30;
        List<Map<String, Double>> rrfs = new ArrayList<>();
        for (int i = 0; i < 101; i++)
            rrfs.add(new HashMap<>());

        argsList.add("experiment/seupd2122-kueri.txt");
        argsList.add("experiment/seupd2122-kueri2.txt");
        argsList.forEach(run -> {
            try (BufferedReader br = new BufferedReader(new FileReader(run))) {
                for (String document; (document = br.readLine()) != null; ) {
                    System.out.println(document);
                    ArrayList<String> tokens = new ArrayList<>(Arrays.asList(document.split("\t")));
                    Map<String, Double> map = rrfs.get(Integer.parseInt(tokens.get(0)));
                    map.put(tokens.get(2), rff(k, Double.parseDouble(tokens.get(3))) + map.getOrDefault(tokens.get(2), 0.0));
                }
                // line is not visible here.
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        List<List<KeyScorePair>> scores = new ArrayList<>();
        for (int i = 0; i < 101; i++)
            scores.add(new ArrayList<>());

        for(int i=0; i<rrfs.size(); i++ ){
            List<KeyScorePair> ofTopic = scores.get(i);
            for (Map.Entry<String, Double> entry : rrfs.get(i).entrySet()) {
                ofTopic.add(new KeyScorePair(entry.getKey(), entry.getValue()));
            }
            ofTopic.sort(new CustomComparator());
        }
        System.out.println(scores);
    }

    private static double rff(double k, double rank) {
        return 1 / (k + rank);
    }

    private static class KeyScorePair {
        private String key;
        private Double score;

        public KeyScorePair(String key, Double score) {
            this.key = key;
            this.score = score;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }

    public static class CustomComparator implements Comparator<KeyScorePair> {
        @Override
        public int compare(KeyScorePair o1, KeyScorePair o2) {
            return o2.getScore().compareTo(o1.getScore());
        }
    }
}
