/**
 * Original code of YOLOBOT
 */
package combination.SubAgents.HandleMCTS;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;

import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import combination.Agent;
import combination.YoloState;
import combination.SubAgents.SubAgent;
import combination.SubAgents.HandleMCTS.RolloutPolicies.EpsilonGreedyBestFirstRolloutPolicy;
import combination.SubAgents.HandleMCTS.RolloutPolicies.HeuristicRolloutPolicy;
import combination.SubAgents.HandleMCTS.RolloutPolicies.RandomNotDeadRolloutPolicy;
import combination.SubAgents.HandleMCTS.RolloutPolicies.RolloutPolicy;
import combination.Util.Heuristics.AStarDistantsHeuristic;
import combination.Util.Heuristics.DeadendHeuristic;
import combination.Util.Heuristics.DistanceToNpcsHeuristic;
import combination.Util.Heuristics.HeatMapHeuristic;
import combination.Util.Heuristics.HeuristicList;
import combination.Util.Heuristics.HeuristicType;
import combination.Util.Heuristics.IModdableHeuristic;
import combination.Util.Heuristics.OneDimensionMoveToMedianHeuristic;
import combination.Util.Heuristics.ScoreHeuristic;
import combination.Util.Heuristics.ScoreLookaheadHeuristic;
import combination.Util.Heuristics.SectorHeatMapHeuristic;
import combination.Util.Heuristics.SimulateDepthHeuristic;
import combination.Util.Heuristics.WinHeuristic;
import combination.Util.TargedChooser.TargetChooser;
import combination.Util.Wissensdatenbank.YoloKnowledge;
import core.game.Observation;

public class MCTHandler extends SubAgent {


	public final static boolean DEBUG_TRACE = true && !Agent.UPLOAD_VERSION;
	private static final int DEFAULT_ROLLOUT_DEPTH = 4;
	static boolean useNonNegativeScore = true;


	public static HeuristicList heuristics = new HeuristicList();
	public static Random rnd;

	public static YoloState rootState;
	public static int ROLLOUT_DEPTH = DEFAULT_ROLLOUT_DEPTH;
	public static double epsilon = 1e-6;
	public static AStarDistantsHeuristic aSDH;
	public static DistanceToNpcsHeuristic npcH;
	public static WinHeuristic winH;
	public static SimulateDepthHeuristic simDH;
	public static DeadendHeuristic deH;
	public static ScoreHeuristic scoreHeuristic;
	public static OneDimensionMoveToMedianHeuristic oneDimenstionMedianHeuristic;
	public static ScoreLookaheadHeuristic scoreLookaheadHeuristic;
	public boolean olNode;
	public final static int closeDistance = 5;
	

	public static RolloutPolicy rolloutPolicy;
	public static RolloutPolicy randomPolicy;
	public static HeuristicRolloutPolicy scoreLookaheadPolicy;

	private TargetChooser targetChooser;

	private int deadendPos;

	public static IModdableHeuristic heuristicToUse;

	public static boolean largerRollouts;


	public MCTHandler(YoloState rootState) {
		olNode = false;
		rnd = new Random();

		scoreHeuristic = new ScoreHeuristic();
		HeatMapHeuristic heatMaph = new HeatMapHeuristic();
		SectorHeatMapHeuristic sectorheatMaph = new SectorHeatMapHeuristic();
		oneDimenstionMedianHeuristic = new OneDimensionMoveToMedianHeuristic(
				rootState);
		scoreLookaheadHeuristic = new ScoreLookaheadHeuristic();
		scoreLookaheadPolicy = new HeuristicRolloutPolicy(true);
		
		winH = new WinHeuristic();
		aSDH = new AStarDistantsHeuristic(null);
		simDH = new SimulateDepthHeuristic();
		npcH = new DistanceToNpcsHeuristic();
		deH = new DeadendHeuristic();

		heuristics.Put(scoreHeuristic);
		heuristics.SetWeight(HeuristicType.ScoreHeuristic, 50);

		heuristics.Put(heatMaph);
		heuristics.SetWeight(HeuristicType.HeatMapHeuristic,
				rootState.isOneDimensionGame() ? 0 : 1);

		heuristics.Put(winH);
		heuristics.SetWeight(HeuristicType.WinHeuristic, 1000);

		heuristics.Put(aSDH);
		heuristics.SetWeight(HeuristicType.AStarDistantsHeuristic,
				rootState.isOneDimensionGame() ? 0 : 50);

		heuristics.Put(npcH);
		heuristics.SetWeight(HeuristicType.DistanceToNpcsHeuristic,
				rootState.isOneDimensionGame() ? 0 : 50);

		heuristics.Put(simDH);
		heuristics.SetWeight(HeuristicType.SimulateDepthHeuristic,
				rootState.isOneDimensionGame() ? 0 : 5);

		heuristics.Put(oneDimenstionMedianHeuristic);
		heuristics.SetWeight(HeuristicType.OneDimensionMoveToMedianHeuristic,
				rootState.isOneDimensionGame() ? 50 : 0);

		if (rootState.isOneDimensionGame())
			ROLLOUT_DEPTH = 10;

		rolloutPolicy = new EpsilonGreedyBestFirstRolloutPolicy();
		randomPolicy = new RandomNotDeadRolloutPolicy();
		targetChooser = new TargetChooser(aSDH, npcH, deH);

		deadendPos = heuristics
				.getIndexOfHeuristic(HeuristicType.DeadendHeuristic);
		
	}

	private void randomize(YoloState state) {
		state.setNewSeed((int) Math.random() * 10000);
	}

	public ACTIONS act(YoloState rootState, ElapsedCpuTimer elapsedTimer) {

		if (rootState.isOneDimensionGame())
			ROLLOUT_DEPTH = 10;
		else{
			if(largerRollouts){
				ROLLOUT_DEPTH = 10;
			}else{
				ROLLOUT_DEPTH = DEFAULT_ROLLOUT_DEPTH;
			}
		}

		if (YoloKnowledge.instance.haveEverGotScoreWithoutWinning()){
			heuristics.SetWeight(HeuristicType.ScoreHeuristic, 50);
			useNonNegativeScore = false;
		}else{
			heuristics.SetWeight(HeuristicType.ScoreHeuristic, 250);
			useNonNegativeScore = true;
		}
		MCTHandler.rootState = rootState;
		if (!rootState.isOneDimensionGame())
			targetChooser.handle(rootState);
		else
			oneDimenstionMedianHeuristic.setFixMedian(rootState);

		heuristicToUse = null;
		if (MCTHandler.aSDH.isActive())
			heuristicToUse = MCTHandler.aSDH;
		else if (MCTHandler.npcH.isActive())
			heuristicToUse = MCTHandler.npcH;

		olNode = true;
		List<Observation>[] npcPositions = rootState.getNpcPositions(rootState
				.getAvatarPosition());
		if (npcPositions != null) {
			for (List<Observation> oneTypeNpcs : npcPositions) {
				if (oneTypeNpcs.size() > 0
						&& (Math.sqrt(oneTypeNpcs.get(0).sqDist) / rootState
								.getBlockSize()) < closeDistance) {
					olNode = true;
					break;
				}
			}
		}

		double avgTimeTaken = 0;
		long remaining = elapsedTimer.remainingTimeMillis();
		int numIters = 0;

		int remainingLimit = 5;

		MCTNode rootNode;

		if (olNode) {
			OLNode.currentState = rootState.copy();
			rootNode = new OLNode(ACTIONS.ACTION_NIL);
			if (!Agent.UPLOAD_VERSION)
				System.out.println("Using OLNode");
		} else {
			rootNode = new CLNode(ACTIONS.ACTION_NIL, rootState);
			if (!Agent.UPLOAD_VERSION)
				System.out.println("Using CLNode");
		}

		MCTNode node;
		while (remaining > 2 * avgTimeTaken && remaining > remainingLimit
				|| remaining > 10) {

			node = rootNode;

			if (olNode) {
				OLNode.currentState = rootState.copy();
				randomize(OLNode.currentState);
			}

			// Expand or Select
			if (DEBUG_TRACE)
				System.out.print("Expand/Select: \n\t");
			node = node.expandOrSelect();

			// Simulate
			if (DEBUG_TRACE)
				System.out.print("\nSimulate: \n\t");
			boolean useScoreLookahead = false;//numIters == 1;
			YoloState endState = node.simulate(useScoreLookahead, numIters == 0);
			if (DEBUG_TRACE)
				System.out.print("\nScore: "
						+ (endState.getGameScore() - rootState.getGameScore())
						+ "\n");

			double[] heuristicValues = heuristics.EvaluateAll(endState);

			if (deadendPos != -1) {
				if (rootNode.lastSelectedChild != null)
					heuristicValues[deadendPos] = deH
							.Evaluate(rootNode.lastSelectedChild.action);
			}

			while (node != null) {
				node.update(heuristicValues);
				node = node.getParent();
			}

			// Hier wird die Verbleibende Zeit Aktualisiert
			numIters++;
			avgTimeTaken = elapsedTimer.elapsedMillis() / numIters;
			remaining = elapsedTimer.remainingTimeMillis();
		}
		if (DEBUG_TRACE)
			System.out.println("\nDone!");

		ACTIONS actionToDo = rootNode.bestAction();
		return actionToDo;

	}

	@Override
	public double EvaluateWeight(YoloState yoloState) {
		return 0;
	}

	@Override
	public void preRun(YoloState yoloState, ElapsedCpuTimer elapsedTimer) {
		// Nothing to do
	}

	

}
