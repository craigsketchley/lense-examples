package com.github.keenon.lense.examples.simple;

import com.github.keenon.lense.convenience.StaticBatchLense;
import com.github.keenon.lense.examples.util.GNUPlot;
import com.github.keenon.lense.gameplay.Game;
import com.github.keenon.lense.gameplay.utilities.UncertaintyUtilityWithoutTime;
import com.github.keenon.lense.storage.ModelQueryRecord;
import com.github.keenon.loglinear.model.ConcatVector;
import com.github.keenon.loglinear.model.GraphicalModel;
import com.github.keenon.loglinear.storage.ModelBatch;
import com.github.keenon.lense.human_source.MTurkHumanSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by craig on 10/08/15.
 *
 * Runs a simple example based on the ad data set
 */
public abstract class SimpleStaticBatch extends StaticBatchLense {
    public String[] tags = new String[]{
            "ad",
            "nonad"
    };

    public static String sourceFolder = "src/main/resources/simple/batches";

    @Override
    public String getModelDumpFileLocation() {
        return "src/main/resources/simple-dump.txt";
    }

    @Override
    public ModelBatch createInitialModelBatch() {

        // NOTE: This should never be used for this toy example data set. More like an example of how this will be written for actual data.

        ModelBatch batch = new ModelBatch();

        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/ad.data"));

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

        return batch;
    }


    @Override
    public void featurize(GraphicalModel model) {
        String[] strVals = model.getVariableMetaDataByReference(0).get("VALUES").split("\\s*,\\s*");
        double[] values = new double[strVals.length];
        for (int i = 0; i < strVals.length; i++) {
            values[i] = Double.parseDouble(strVals[i]);
        }

        model.addFactor(new int[]{0}, new int[]{2}, (assn) -> {
            ConcatVector vector = new ConcatVector(0);
            String tag = tags[assn[0]];
            namespace.setDenseFeature(vector, tag + "BIAS", new double[]{1.0});
            for (int i = 0; i < values.length; i++) {
                if (values[i] > 0) {
                    namespace.setSparseFeature(vector, tag + "VALUE:" + i, i + "", values[i]);
                }
            }

            return vector;
        });
    }

    UncertaintyUtilityWithoutTime utility = new UncertaintyUtilityWithoutTime();

    @Override
    public double utility(Game game) {
        UncertaintyUtilityWithoutTime.humanQueryCost = 0.02;
        return utility.apply(game);
    }

    @Override
    public void dumpGame(GameRecord gameRecord, BufferedWriter bw) throws IOException {
        Game game = gameRecord.game;

        bw.write("Game Results:\n\n");
        int[] guesses = game.getMAP();
        int[] queries = new int[guesses.length];
        for (Game.Event e : game.stack) {
            if (e instanceof Game.QueryLaunch) {
                Game.QueryLaunch ql = (Game.QueryLaunch)e;
                queries[ql.variable]++;
            }
        }
        bw.append("VALUES\tLABEL\tGUESS\nQUERIES\n-------------\n");
        for (int i = 0; i < game.model.getVariableSizes().length; i++) {
            if (!game.model.getVariableMetaDataByReference(i).containsKey("VALUES")) break;
            bw.append(game.model.getVariableMetaDataByReference(i).get("VALUES"));
            bw.append("\t");
            bw.append(game.model.getVariableMetaDataByReference(i).get("LABEL"));
            bw.append("\t");
            bw.append(tags[guesses[i]]);
            bw.append("\t");
            bw.append(Integer.toString(queries[i]));
            bw.append("\n");
        }

        bw.write("\nGame History:\n\n");
        List<Game.Event> events = new ArrayList<>();
        events.addAll(game.stack);
        game.resetEvents();

        DecimalFormat df = new DecimalFormat("0.000");

        for (Game.Event e : events) {
            bw.write("-----------\n");
            bw.append("VALUES\tDIST\tLABEL\tGUESS\n-------------\n");
            double[][] marginals = game.getMarginals();
            int[] map = game.getMAP();
            for (int i = 0; i < game.model.getVariableSizes().length; i++) {
                if (!game.model.getVariableMetaDataByReference(i).containsKey("VALUES")) break;
                String token = game.model.getVariableMetaDataByReference(i).get("VALUES");
                bw.write(token);
                bw.write("\t[");
                for (int j = 0; j < marginals[i].length; j++) {
                    if (j != 0) bw.write(",");
                    bw.write(tags[j]);
                    bw.write("=");
                    bw.write(df.format(marginals[i][j]));
                }
                bw.write("]\t");
                bw.append(game.model.getVariableMetaDataByReference(i).get("LABEL"));
                bw.append("\t");
                bw.write(tags[map[i]]);
                bw.append("\n");
            }
            bw.write("\n-----------");
            bw.write("\nCurrent job postings un-answered: "+game.jobPostings.size());
            bw.write("\nCurrent available humans: "+game.availableHumans.size());
            bw.write("\nCurrent in-flight queries: ");
            for (Game.QueryLaunch ql : game.inFlightRequests) {
                bw.write("\n\t"+game.model.getVariableMetaDataByReference(ql.variable).get("VALUES"));
            }
            if (e.isGameplayerInitiated()) bw.write("\n-----------\nMove choice:\n");
            else bw.write("\n-----------\nNext event:\n");
            // Debug the actual event
            if (e instanceof Game.QueryLaunch) {
                Game.QueryLaunch ql = (Game.QueryLaunch)e;
                bw.write("Launch query on values "+ql.variable+" \""+game.model.getVariableMetaDataByReference(ql.variable).get("VALUES")+"\"");
            }
            else if (e instanceof Game.HumanJobPosting) {
                bw.write("Making job posting");
            }
            else if (e instanceof Game.QueryResponse) {
                Game.QueryResponse qr = (Game.QueryResponse)e;
                Game.QueryLaunch ql = qr.request;
                bw.write("Got response on values "+ql.variable+" \""+game.model.getVariableMetaDataByReference(ql.variable).get("VALUES")+"\" = "+tags[qr.response]);
            }
            else {
                bw.write(e.toString());
            }
            bw.write("\n");
            e.push(game);
        }
    }

    private static class CheckpointRecord {
        double avgF1;
        double avgQueries;
        Map<String,Double> tagF1 = new HashMap<>();
        Map<String,Double> tagQueries = new HashMap<>();
    }

    List<CheckpointRecord> checkpointRecords = new ArrayList<>();

    @Override
    public void checkpoint(List<GameRecord> gamesFinished) {
        System.err.println("Finished "+gamesFinished.size()+" games");

        String subfolder = getThisRunPerformanceReportSubFolder();

        Map<String,Double> correctLabels = new HashMap<>();
        Map<String,Double> foundCorrect = new HashMap<>();
        Map<String,Double> foundGuessed = new HashMap<>();
        double correct = 0.0;
        double total = 0.0;

        Map<String,Double> queriesPerType = new HashMap<>();
        double queries = 0.0;
        double delays = 0.0;
        int totalValues = 0;

        synchronized (gamesFinished) {
            for (GameRecord record : gamesFinished) {
                int[] guesses = record.result;
                String[] labelGuesses = new String[guesses.length];
                for (int i = 0; i < guesses.length; i++) {
                    labelGuesses[i] = tags[guesses[i]];
                    String trueValue = record.game.model.getVariableMetaDataByReference(i).get("LABEL");
                    if (labelGuesses[i].equals(trueValue)) {
                        correct++;
                        correctLabels.put(labelGuesses[i], correctLabels.getOrDefault(labelGuesses[i], 0.) + 1);
                    }
                    total++;
                    foundCorrect.put(trueValue, foundCorrect.getOrDefault(trueValue, 0.) + 1);
                    foundGuessed.put(labelGuesses[i], foundGuessed.getOrDefault(labelGuesses[i], 0.) + 1);
                }

                for (Game.Event e : record.game.stack) {
                    if (e instanceof Game.QueryLaunch) {
                        queries++;
                        Game.QueryLaunch ql = (Game.QueryLaunch) e;
                        String trueValue = record.game.model.getVariableMetaDataByReference(ql.variable).get("LABEL");
                        queriesPerType.put(trueValue, queriesPerType.getOrDefault(trueValue, 0.0) + 1.0);
                    }
                }

                delays += record.game.timeSinceGameStart;

                totalValues += labelGuesses.length;
            }
        }

        CheckpointRecord checkpointRecord = new CheckpointRecord();

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(subfolder+"/results.txt"));
            bw.write("Finished "+gamesFinished.size()+" games\n");

            bw.write("\nSystem results:\n");

            bw.write("\nAccuracy: " + (correct / total) + "\n");

            double avgF1 = 0.0;

            for (String tag : tags) {
                double precision = foundGuessed.getOrDefault(tag, 0.0) == 0 ? 0.0 : correctLabels.getOrDefault(tag, 0.0) / foundGuessed.get(tag);
                double recall = foundCorrect.getOrDefault(tag, 0.0) == 0 ? 0.0 : correctLabels.getOrDefault(tag, 0.0) / foundCorrect.get(tag);
                double f1 = (precision + recall == 0.0) ? 0.0 : (precision * recall * 2) / (precision + recall);
                bw.write("\n"+tag+" ("+foundCorrect.getOrDefault(tag, 0.0).intValue()+")");
                bw.write("\n\tP:" + precision + " (" + correctLabels.getOrDefault(tag, 0.0).intValue() + "/" + foundGuessed.getOrDefault(tag, 0.0).intValue() + ")");
                bw.write("\n\tR:"+recall+" ("+correctLabels.getOrDefault(tag, 0.0).intValue()+"/"+foundCorrect.getOrDefault(tag, 0.0).intValue()+")");
                bw.write("\n\tF1:" + f1);

                checkpointRecord.tagF1.put(tag, f1);

                if (!tag.equals("O")) {
                    avgF1 += f1;
                }
            }
            avgF1 /= 3;
            bw.write("\n\nAvg F1: "+avgF1);

            checkpointRecord.avgF1 = avgF1;
            checkpointRecord.avgQueries = (queries/totalValues);

            bw.write("\n\nQueries: ");
            bw.write("\nQ/tok: "+(queries/totalValues)+" ("+queries+"/"+totalValues+")");
            for (String tag : tags) {
                bw.write("\n"+tag+" ("+foundCorrect.getOrDefault(tag, 0.0).intValue()+")");
                double qs = foundCorrect.getOrDefault(tag, 0.0) == 0 ? 0.0 : queriesPerType.getOrDefault(tag, 0.0) / foundCorrect.get(tag);
                bw.write("\n\tQ/tok: "+qs+" ("+queriesPerType.getOrDefault(tag, 0.0)+"/"+foundCorrect.get(tag)+")");

                checkpointRecord.tagQueries.put(tag, qs);
            }

            bw.write("\n\nDelays: ");
            bw.write("\nms/tok: "+(delays/totalValues)+" ("+delays+"/"+totalValues+")");

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        checkpointRecords.add(checkpointRecord);

        GNUPlot queriesPlot = new GNUPlot();
        queriesPlot.title = "Queries/vect vs time, by class";
        queriesPlot.xLabel = "iterations";
        queriesPlot.yLabel = "Queries/vect";

        for (String tag : tags) {
            double[] x = new double[checkpointRecords.size()];
            double[] y = new double[checkpointRecords.size()];
            for (int i = 0; i < x.length; i++) {
                x[i] = i;
                y[i] = checkpointRecords.get(i).tagQueries.get(tag);
            }

            queriesPlot.addLine(tag, x, y);
        }

        double[] xAvg = new double[checkpointRecords.size()];
        double[] yAvg = new double[checkpointRecords.size()];
        for (int i = 0; i < xAvg.length; i++) {
            xAvg[i] = i;
            yAvg[i] = checkpointRecords.get(i).avgQueries;
        }

        queriesPlot.addLine("AVG", xAvg, yAvg);

        try {
            queriesPlot.saveAnalysis(subfolder+"/queries/");
        } catch (IOException e) {
            e.printStackTrace();
        }

        GNUPlot f1Plot = new GNUPlot();
        f1Plot.title = "F1 vs time, by class";
        f1Plot.xLabel = "iterations";
        f1Plot.yLabel = "F1";

        for (String tag : tags) {
            double[] x = new double[checkpointRecords.size()];
            double[] y = new double[checkpointRecords.size()];
            for (int i = 0; i < x.length; i++) {
                x[i] = i;
                y[i] = checkpointRecords.get(i).tagF1.get(tag);
            }

            f1Plot.addLine(tag, x, y);
        }

        double[] x = new double[checkpointRecords.size()];
        double[] y = new double[checkpointRecords.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            y[i] = checkpointRecords.get(i).avgF1;
        }

        f1Plot.addLine("AVG", x, y);

        try {
            f1Plot.saveAnalysis(subfolder + "/f1/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getL2Regularization() {
        return 0.001;
    }

    @Override
    public String dumpModel(GraphicalModel model) {
        StringBuilder sb = new StringBuilder();

        ModelQueryRecord qr = ModelQueryRecord.getQueryRecordFor(model);
        for (int i = 0; i < model.getVariableSizes().length; i++) {
            if (!model.getVariableMetaDataByReference(i).containsKey("VALUES")) break;
            sb.append(model.getVariableMetaDataByReference(i).get("VALUES"));
            sb.append("\t");
            sb.append(model.getVariableMetaDataByReference(i).get("LABEL"));
            for (ModelQueryRecord.QueryRecord rec : qr.getResponses(i)) {
                sb.append("\t");
                sb.append("(");
                sb.append(tags[rec.response]);
                sb.append(",");
                sb.append(Long.toString(rec.delay));
                sb.append(")");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
