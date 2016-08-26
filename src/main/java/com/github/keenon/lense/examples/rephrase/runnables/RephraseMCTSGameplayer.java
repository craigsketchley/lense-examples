package com.github.keenon.lense.examples.rephrase.runnables;

import com.github.keenon.lense.examples.rephrase.RephraseStaticBatch;
import com.github.keenon.lense.examples.simple.SimpleStaticBatch;
import com.github.keenon.lense.gameplay.Game;
import com.github.keenon.lense.gameplay.players.GamePlayer;
import com.github.keenon.lense.gameplay.players.GamePlayerMCTS;
import com.github.keenon.lense.gameplay.utilities.UncertaintyUtilityWithoutTime;
import com.github.keenon.lense.human_source.HumanSource;
import com.github.keenon.lense.human_source.ModelTagsHumanSource;

import java.io.IOException;

/**
 * Created by craig on 08/12/16.
 */
public class RephraseMCTSGameplayer extends RephraseStaticBatch {

    static String destFolder = "src/main/resources/rephrase/runs";

    public RephraseMCTSGameplayer() throws IOException {
        super();
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) sourceFolder = args[0];
        if (args.length > 1) destFolder = args[1];
        new RephraseMCTSGameplayer().run();
    }

    @Override
    public GamePlayer getGamePlayer() {
        GamePlayer gp = new GamePlayerMCTS();
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
        return destFolder+"/mcts-gameplayer";
    }

    @Override
    public boolean parallelBatchIgnoreRetraining() {
        return false;
    }

    UncertaintyUtilityWithoutTime utility = new UncertaintyUtilityWithoutTime();

    @Override
    public double utility(Game game) {
        UncertaintyUtilityWithoutTime.humanQueryCost = 0.01;
        return utility.apply(game);
    }
}
