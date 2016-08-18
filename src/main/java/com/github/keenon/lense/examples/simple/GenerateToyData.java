package com.github.keenon.lense.examples.simple;

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
public class GenerateToyData {
    public static void main(String[] args) throws IOException {
        String[] adTags = new String[]{
                "ad",
                "nonad"
        };

        double[] errorRate = new double[] { // Abitrary
                0.3,
                0.3
        };
        List<String> tags = new ArrayList<>();
        for (String t : adTags) tags.add(t);


        // Pinch response delays from NER
        ModelBatch delayBatch = new ModelBatch("src/main/resources/ner/batches/ner-batch.ser");
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
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/simple/ad.data"));

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s*,\\s*");
                String[] strVals = new String[parts.length - 1];
                double[] values = new double[parts.length - 1];
                for (int i = 0; i < parts.length - 1; i++) {
                    strVals[i] = parts[i];
                    values[i] = Double.parseDouble(parts[i]);
                }
                String label = parts[parts.length - 1];

                GraphicalModel model = new GraphicalModel();

                Map<String,String> metadata = model.getVariableMetaDataByReference(0);

                // Save label
                metadata.put("VALUES", String.join(",", strVals));
                metadata.put("LABEL", label);

                // Add the query data
                JSONObject queryData = new JSONObject();

                StringBuilder html = new StringBuilder("What kind of thing is this vector?<br>");
                html.append("<span class=\"content\">");
                html.append(String.join(",", strVals));
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

        Collections.shuffle(batch);

        Random r = new Random(42); // Repeatable

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
                    while (humanLabel == trueLabel) humanLabel = r.nextInt(adTags.length);
                }
                qr.recordResponse(0, humanLabel, delay);
            }

            qr.writeBack();

            synthetic.add(clone);
        }

        synthetic.writeToFileWithoutFactors("src/main/resources/simple/batches/simple-batch.ser");
    }

}
