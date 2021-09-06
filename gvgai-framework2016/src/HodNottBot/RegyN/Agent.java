package HodNottBot.RegyN;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Random;

public class Agent extends AbstractPlayer {
    private Random generator;
    private int depth;
    private long remaining;
    private long startTime;
    private double avgTimeTaken;
    private double acumTimeTaken;
    private int numIters;
    private ArrayList<Types.ACTIONS> actions;
    ITreeNode root;
    int turnNumber;
    GameKnowledge knowledge;
    PositionHistory history;
    AgentParameters params;


    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        generator = new Random();

        params = AgentParameters.GetInstance(true);
        params.maxDepth = 10;
        params.expandIntelligently = true;
        params.rollSimulationCleverly = true;
        params.useGameKnowledge = true;
        params.useHistoryInValuation = true;
        params.useHistoryOnExit = false;
        params.finder = BestActionFinder.BestAverage;

        actions = stateObs.getAvailableActions();
        depth = 10;
        turnNumber = 0;
        knowledge = GameKnowledge.GetNew();
        history = PositionHistory.GetNew();
        if(params.useGameKnowledge) {
            ExtractKnowledge(stateObs, elapsedTimer);
        }
    }

    private void ExtractKnowledge(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        knowledge.GatherStaticInfo(stateObs);
        if(knowledge.type == GameType.Planar2D){
            knowledge.GatherSpriteInfoRandomly(stateObs, elapsedTimer);
        }
    }

    private void ResetTimers(ElapsedCpuTimer elapsedTimer) {
        numIters = 0;
        avgTimeTaken = 0;
        acumTimeTaken = 0;
        startTime = elapsedTimer.remainingTimeMillis();
        remaining = startTime;
    }

    private void UpdateTimers(ElapsedCpuTimer elapsedTimer) {
        numIters++;
        acumTimeTaken = elapsedTimer.remainingTimeMillis() - startTime;
        avgTimeTaken = acumTimeTaken / numIters;
        remaining = elapsedTimer.remainingTimeMillis();
    }

    private Types.ACTIONS ChooseBestAction(ITreeNode root) {
        if(params.finder == BestActionFinder.BestAverage) {
            return root.GetBestAverageAction(params.useHistoryOnExit);
        }
        else if(params.finder == BestActionFinder.BestScore){
            return root.GetBestScoreAction(params.useHistoryOnExit);
        }
        else{
            return root.GetMostVisitedAction(params.useHistoryOnExit);
        }
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        ResetTimers(elapsedTimer);
        history.Add(Position2D.GetAvatarPosition(stateObs));

        root = new MaxTreeNode(depth, stateObs);
//        String breakReason;
        int remainingLimit = 5;
        while (true) {
            if (remaining <= 2 * avgTimeTaken) {
                //breakReason = "avgTimeTaken";
                break;
            } else if (remaining < remainingLimit) {
                //breakReason = "remainingLimit";
                break;
            }
            if(params.expandIntelligently) {
                root.ExpandIntelligently(stateObs);
            }
            else{
                root.Expand(stateObs);
            }
            UpdateTimers(elapsedTimer);
        }
//        System.out.println(numIters + " " + breakReason);

//        PĘTLA DO DEBUGOWANIA BEZ OGRANICZEN CZASOWYCH
//        int iterations = 20;
//        for(int i = 0; i<iterations; i++){
//            if(params.expandIntelligently) {
//                root.ExpandIntelligently(stateObs);
//            }
//            else{
//                root.Expand(stateObs);
//            }
//            UpdateTimers(elapsedTimer);
//        }
//        System.out.println(avgTimeTaken);
        turnNumber++;
//        System.out.println(numIters);
        Types.ACTIONS chosen = ChooseBestAction(root);
//        System.out.println(chosen.name());
        return chosen;
    }
}
