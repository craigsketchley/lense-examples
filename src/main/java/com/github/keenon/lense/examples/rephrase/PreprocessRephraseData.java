package com.github.keenon.lense.examples.rephrase;

import com.github.keenon.lense.gameplay.distributions.ContinuousDistribution;
import com.github.keenon.lense.gameplay.distributions.DiscreteSetDistribution;
import com.github.keenon.lense.human_source.MTurkHumanSource;
import com.github.keenon.lense.storage.ModelQueryRecord;
import com.github.keenon.loglinear.model.GraphicalModel;
import com.github.keenon.loglinear.storage.ModelBatch;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by craigsketchley on 08/08/16.
 */
public class PreprocessRephraseData {
    public static void main(String[] args) throws IOException {

        Pattern pattern = Pattern.compile("^([01]) \"(([^\"\\\\]|\\\\\"|\\\\)+)\" \"(([^\"\\\\]|\\\\\"|\\\\)+)\"( \"(([^\"\\\\]|\\\\\"|\\\\)+)\")?$");

        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/rephrase/that-removal"));
            BufferedWriter bw = new BufferedWriter(new FileWriter("src/main/resources/rephrase/detections"));

            String line;
            while ((line = br.readLine()) != null) {

                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    boolean accepted = matcher.group(1).equals("1");

                    String detection = matcher.group(2).replaceAll("\\\\\"", "\"")
                            .replaceAll("^([^A-Za-z0-9]|\\s)*", "");

                    String context = matcher.group(4).replaceAll("^\\.\\.\\.[^ ]* ", "")
                            .replaceAll(" [^ ]*\\.\\.\\.$", "")
                            .replaceAll("\\\\\"", "\"");

                    int position = context.indexOf(detection);

                    // Naive: Go from found index fore and back, looking for bounding sentence.
                    int sentenceStart = 0, sentenceEnd = context.length();
                    for (int i = position; i >= 0; i--) {
                        if (context.charAt(i) == '.' || context.charAt(i) == '?' || context.charAt(i) == '!') {
                            sentenceStart = i + 1;
                            break;
                        }
                    }

                    for (int i = position; i < context.length(); i++) {
                        if (context.charAt(i) == '.' || context.charAt(i) == '?' || context.charAt(i) == '!') {
                            sentenceEnd = i + 1;
                            break;
                        }
                    }

                    context = context.substring(sentenceStart, sentenceEnd).replaceAll("^([^A-Za-z0-9]|\\s)*", "");

                    position = context.indexOf(detection);

                    bw.write(accepted ? "1" : "0");
                    bw.write("\n");
                    bw.write(detection);
                    bw.write("\n");
                    bw.write(new Integer(position).toString());
                    bw.write("\n");
                    bw.write(context);
                    bw.write("\n");
                    bw.write("\n");

                } else {
                    System.out.println("NO MATCH");
                    continue;
                }

            }
            br.close();
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
