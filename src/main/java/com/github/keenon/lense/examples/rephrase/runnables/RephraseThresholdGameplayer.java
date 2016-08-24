package com.github.keenon.lense.examples.rephrase.runnables;

import com.github.keenon.lense.examples.rephrase.RephraseStaticBatch;
import com.github.keenon.lense.gameplay.players.GamePlayer;
import com.github.keenon.lense.gameplay.players.GamePlayerThreshold;
import com.github.keenon.lense.human_source.HumanSource;
import com.github.keenon.lense.human_source.ModelTagsHumanSource;

import java.io.IOException;


/**
 * Created by craig on 14/08/2016.
 */
public class RephraseThresholdGameplayer extends RephraseStaticBatch {

    static String destFolder = "src/main/resources/rephrase/runs";

    public RephraseThresholdGameplayer() throws IOException {
        super();
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) sourceFolder = args[0];
        if (args.length > 1) destFolder = args[1];
        new RephraseThresholdGameplayer().run();
    }

    @Override
    public GamePlayer getGamePlayer() {
        GamePlayer gp = new GamePlayerThreshold(); // new GamePlayerNVote(5, false);
        return gp;
    }

    @Override
    public String getBatchFileLocation(){
        return sourceFolder+"/rephrase-batch.ser";
    }

    @Override
    public HumanSource getHumanSource() {
        return new ModelTagsHumanSource(namespace, observedHumanDelays);
    }

    @Override
    public String getPerformanceReportFolder() {
        return destFolder+"/threshold-gameplayer-data";
    }

    @Override
    public boolean parallelBatchIgnoreRetraining() {
        return false;
    }
}
