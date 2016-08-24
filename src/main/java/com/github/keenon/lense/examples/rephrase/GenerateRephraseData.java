package com.github.keenon.lense.examples.rephrase;

import com.github.keenon.lense.gameplay.distributions.ContinuousDistribution;
import com.github.keenon.lense.gameplay.distributions.DiscreteSetDistribution;
import com.github.keenon.lense.human_source.MTurkHumanSource;
import com.github.keenon.lense.storage.ModelQueryRecord;
import com.github.keenon.loglinear.model.GraphicalModel;
import com.github.keenon.loglinear.storage.ModelBatch;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by craigsketchley on 08/08/16.
 */
public class GenerateRephraseData {
    public static void main(String[] args) throws IOException {
        String[] rephraseDecisions = new String[]{
                "NO",
                "YES"
        };

        double[] errorRate = new double[] { // Abitrary
                0.1,
                0.1
        };
        List<String> tags = new ArrayList<>();
        for (String t : rephraseDecisions) tags.add(t);


        // Pinch response delays from sentiment
        ModelBatch delayBatch = new ModelBatch("src/main/resources/sentiment/batches/sentiment.ser");
        List<Long> delays = new ArrayList<>();
        outer: for (int j = 10; j < delayBatch.size(); j++) {
            GraphicalModel model = delayBatch.get(j);
            for (int i = 0; i < model.variableMetaData.size(); i++) {
                for (ModelQueryRecord.QueryRecord qr : ModelQueryRecord.getQueryRecordFor(model).getResponses(i)) {
                    delays.add(qr.delay);
                    if (delays.size() > 1000) break outer;
                }
            }
        }
        long[] observed = new long[delays.size()];
        for (int i = 0; i < delays.size(); i++) {
            observed[i] = delays.get(i);
        }
        ContinuousDistribution observedHumanDelays = new DiscreteSetDistribution(observed);

        // Get actual data
        ModelBatch batch = new ModelBatch();
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/rephrase/detections"));

            String line;
            int count = 0;
            int lineCoutn = 0;
            while ((line = br.readLine()) != null) {
                int accepted = Integer.parseInt(line);
                String detection = br.readLine();
                int position = Integer.parseInt(br.readLine());
                String sentence = br.readLine();
                br.readLine(); // Blank space

                GraphicalModel model = new GraphicalModel();

                Map<String,String> metadata = model.getVariableMetaDataByReference(0);

                // Save label
                metadata.put("SENT", sentence);
                metadata.put("DET", detection);
                metadata.put("POS", new Integer(position).toString());
                metadata.put("LABEL", rephraseDecisions[accepted]);

                // Add the query data
                JSONObject queryData = new JSONObject();

                // TODO: Design a more appropriate query
                StringBuilder html = new StringBuilder("Can we remove the word THAT in this sentence and it still makes sense?<br>");
                html.append("<span class=\"content\">");
                html.append(sentence);
                html.append("</span>");

                queryData.put("html", html.toString());

                JSONArray choices = new JSONArray();
                for (String tag : tags) {
                    choices.add(tag);
                }

                queryData.put("choices", choices);

                metadata.put(MTurkHumanSource.QUERY_JSON, queryData.toJSONString());
                batch.add(model);
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Random r = new Random(42); // Repeatable

        Collections.shuffle(batch, r);

        ModelBatch synthetic = new ModelBatch();
        for (GraphicalModel model : batch) {
            GraphicalModel clone = model.cloneModel();

            ModelQueryRecord qr = ModelQueryRecord.getQueryRecordFor(clone);

            int trueLabel = tags.indexOf(model.getVariableMetaDataByReference(0).get("LABEL"));
            if (trueLabel == -1) throw new IllegalStateException("Urmmmm");

            double errRate = errorRate[trueLabel];

            qr.getResponses(0).clear();

            while (qr.getResponses(0).size() < 6) {
                long delay = observedHumanDelays.drawSample(r) + r.nextInt(15) - 7;
                boolean makeError = r.nextDouble() < errRate;
                int humanLabel = trueLabel;
                if (makeError) {
                    humanLabel = (trueLabel + 1) % rephraseDecisions.length;
                }
                qr.recordResponse(0, humanLabel, delay);
            }

            qr.writeBack();

            synthetic.add(clone);
        }

        synthetic.writeToFileWithoutFactors("src/main/resources/rephrase/batches/rephrase-batch.ser");
    }

}
