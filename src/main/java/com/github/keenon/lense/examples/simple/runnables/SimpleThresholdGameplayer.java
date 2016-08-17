package com.github.keenon.lense.examples.simple.runnables;

import com.github.keenon.lense.examples.simple.SimpleStaticBatch;
import com.github.keenon.lense.gameplay.players.GamePlayer;
import com.github.keenon.lense.gameplay.players.GamePlayerThreshold;
import com.github.keenon.lense.human_source.HumanSource;
import com.github.keenon.lense.human_source.ModelTagsHumanSource;

import java.io.IOException;

/**
 * Created by craig on 14/08/2016.
 */
public class SimpleThresholdGameplayer extends SimpleStaticBatch {

    static String destFolder = "src/main/resources/simple/runs";

    public static void main(String[] args) throws IOException {
        if (args.length > 0) sourceFolder = args[0];
        if (args.length > 1) destFolder = args[1];
        new SimpleThresholdGameplayer().run();
    }

    @Override
    public GamePlayer getGamePlayer() {
        GamePlayer gp = new GamePlayerThreshold(); // new GamePlayerNVote(5, false);
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
        return destFolder+"/threshold-gameplayer-data";
    }

    @Override
    public boolean parallelBatchIgnoreRetraining() {
        return false;
    }
}
