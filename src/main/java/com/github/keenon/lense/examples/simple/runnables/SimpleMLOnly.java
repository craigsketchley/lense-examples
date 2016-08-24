package com.github.keenon.lense.examples.simple.runnables;

import com.github.keenon.lense.examples.simple.SimpleStaticBatch;
import com.github.keenon.lense.gameplay.players.GamePlayer;
import com.github.keenon.lense.gameplay.players.GamePlayerNVote;
import com.github.keenon.lense.human_source.HumanSource;
import com.github.keenon.lense.human_source.ModelTagsHumanSource;
import com.github.keenon.loglinear.learning.LogLikelihoodDifferentiableFunction;

import java.io.IOException;

/**
 * Created by craig on 10/10/16.
 */
public class SimpleMLOnly extends SimpleStaticBatch {
    private int getTagIndex(String tag) {
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals(tag)) {
                return i;
            }
        }
        return -1;
    }

    public SimpleMLOnly() {
        this.overrideSetTrainingLabels = (model) -> {
            for (int i = 0; i < model.getVariableSizes().length; i++) {
                if (model.getVariableMetaDataByReference(i).containsKey("LABEL")) {
                    int tagIndex = getTagIndex(model.getVariableMetaDataByReference(i).get("LABEL"));
                    assert(tagIndex >= 0);
                    model.getVariableMetaDataByReference(i).put(
                            LogLikelihoodDifferentiableFunction.VARIABLE_TRAINING_VALUE,
                            ""+tagIndex);
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    static String destFolder = "src/main/resources/simple/runs";

    public static void main(String[] args) throws IOException {
        if (args.length > 0) sourceFolder = args[0];
        if (args.length > 1) destFolder = args[1];
        new SimpleMLOnly().run();
    }

    @Override
    public GamePlayer getGamePlayer() {
        GamePlayer gp = new GamePlayerNVote(0, false);
        return gp;
    }

    @Override
    public String getBatchFileLocation(){
        return sourceFolder+"/simple-batch.ser";
    }

    @Override
    public HumanSource getHumanSource() {
        return new ModelTagsHumanSource(namespace, observedHumanDelays);
    }

    @Override
    public String getPerformanceReportFolder() {
        return destFolder+"/ml-only-data";
    }

    @Override
    public boolean parallelBatchIgnoreRetraining() {
        return false;
    }
}
