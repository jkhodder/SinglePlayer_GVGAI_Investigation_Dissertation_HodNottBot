package UrbanSk;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;

import java.awt.GraphicsDevice.WindowTranslucency;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class Agent extends AbstractPlayer {
    public static class Pair<F, S> {
	public F first;
	public S second;

	Pair(F f, S s) {
	    first = f;
	    second = s;
	}
    }

    /**
     * The best currently found action Will be returned when we run out of time
     */
    Types.ACTIONS bestAction;

    /**
     * The maximum amount of time the main loop can take to find the solution We
     * have a total of 40ms to return an answer. This is set a little lower to
     * give the class some time to actually return the value
     */
    static final int BREAK_TIME_MS = 25;

    // genetic algorithm parameters
    static final int LOOKAHEAD_DEPTH = 10;
    static final int POPULATION_SIZE = 12;
    static final float MUTATION_RATE = 0.1f;
    int numActions;

    static enum SELECTION_TYPES {
	// tournament selection with tournament size=2, pick the best out of a
	// pair
	TOURNAMENT_SELECTION_SIMPLE
    };

    SELECTION_TYPES SELECTION_METHOD = SELECTION_TYPES.TOURNAMENT_SELECTION_SIMPLE;

    // Tournament selection parameters
    static final int TOURNAMENT_SIZE = 2;

    Random randomGenerator;
    StateHeuristic stateHeuristic;

    static enum GAME_TYPES {
	REALTIME, PUZZLE, UNKNOWN
    };

    private GAME_TYPES gameType = GAME_TYPES.UNKNOWN;
    int stepsToDetectGameType = 5;
    Deque<Types.ACTIONS> bestPuzzleSolution = null;
    boolean startedSearch = false;

    // to save time, initialize things that carry over between steps in the
    // constructor,
    // which has a 1 second time limit, instead of 40ms per step
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
	randomGenerator = new Random();
	numActions = stateObs.getAvailableActions().size();
	// stateHeuristic = new CustomStateHeuristic(stateObs);
	stateHeuristic = new AdvancedStateHeuristic(stateObs);
    }

    /**
     * 
     * The agent used to controll the game Uses a genetic algorithm
     * 
     * @param stateObs
     *            Observation of the current state.
     * @param elapsedTimer
     *            Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs,
	    ElapsedCpuTimer elapsedTimer) {

	bestAction = null;
	try {
	    run(stateObs, elapsedTimer);
	} catch (TimeoutException e) {
	    StateObservation sObsCopy = stateObs.copy();
	    sObsCopy.advance(bestAction);

	    if (stateHeuristic.visitedLocations.containsKey(stateObs
		    .getAvatarPosition().toString())) {
		int visitedLocationPenalty = stateHeuristic.visitedLocations
			.get(stateObs.getAvatarPosition().toString());
		stateHeuristic.visitedLocations.put(stateObs
			.getAvatarPosition().toString(),
			visitedLocationPenalty + 200);
		// System.out.println(visitedLocationPenalty);
	    } else {
		stateHeuristic.visitedLocations.put(stateObs
			.getAvatarPosition().toString(), 200);
	    }
	    /*final sanity check: don't lose with an action
	    this can happen if the genetic algorithms generates all population members
	    that start with a state that loses the game.
	    An example is the game frogs, where the player will ussually lose if he moves in any direction but one.
	    */
	    if (sObsCopy.getGameWinner() == Types.WINNER.PLAYER_LOSES){
		ArrayList<ACTIONS> actions = stateObs.getAvailableActions();
		for (ACTIONS action : actions){
		    sObsCopy=stateObs.copy();
		    sObsCopy.advance(action);
		    if(sObsCopy.getGameWinner()!=Types.WINNER.PLAYER_LOSES){
			return action;
		    }
		}
	    }
	    
	    return bestAction;
	}
	// will never be reached
	return bestAction;
    }

    /**
     * The main loop of the genetic algorithm. Updates best action each loop
     * using a genetic algorithm
     * 
     * @param stateObs
     * @param elapsedTimer
     * @throws TimeoutException
     *             When we are close to running out of time
     */
    @SuppressWarnings("deprecation")
    private void run(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
	    throws TimeoutException {
	final StateObservation fStateObs = stateObs.copy();
	Thread searchThread = new Thread(new Runnable() {
	    @Override
	    public void run() {
		breadthFirstSearch(fStateObs);
	    }
	});
	//TODO: How to determine if a game is a puzzle game?
	// Most games without NPCS are, but some (frogs), aren't
	//maybe check if distances to objects don't change over a time period
	if(stateObs.getNPCPositions()==null && false){
	    gameType=GAME_TYPES.PUZZLE;
	} else {
	    gameType=GAME_TYPES.REALTIME;
	}
	if (gameType == GAME_TYPES.PUZZLE) {
	    
	    // start search on a new thread
	    if (!startedSearch) {
		startedSearch = true;
		searchThread.start();
	    }
	    // no solution found yet, wait for time
	    if (bestPuzzleSolution == null) {
		while (true) {
		    bestAction = Types.ACTIONS.ACTION_NIL;
		    checkTimeThrowException(elapsedTimer);
		}
	    } else {
		bestAction = bestPuzzleSolution.pollFirst();
		throw new TimeoutException();
	    }
	} else if (gameType == GAME_TYPES.REALTIME) {
	    
	    // We generate a new initail population each step, since the game
	    // state changes with each step
	    ArrayList<Types.ACTIONS[]> population = generatePopulation(
		    stateObs, POPULATION_SIZE);
	    bestAction = population.get(0)[0];
	    int stepNumber = 0;
	    // main loop
	    while (true) {
		// System.out.println("Begginning step: " +
		// elapsedTimer.elapsedMillis());
		/*
		 * 1. Selection 2. Cross Over 3. Mutation
		 */
		// System.out.println("a");
		// System.out.println("Run step: " + stepNumber);
		stepNumber++;
		// check if we are about to run out of time at different steps
		// in the loop
		checkTimeThrowException(elapsedTimer);

		// 1. Selection
		// System.out.println("b");
		if (SELECTION_METHOD == SELECTION_TYPES.TOURNAMENT_SELECTION_SIMPLE) {
		    Pair<ArrayList<ACTIONS[]>, ACTIONS> returned = tournamentSelectionSimple(
			    stateObs, population, elapsedTimer);
		    population = returned.first;
		    bestAction = returned.second;
		    // if(bestAction!=null)
		    // System.out.println("best action " + bestAction.name());
		}
		// System.out.println("c");
		// System.out.println("Selection done: " +
		// elapsedTimer.elapsedMillis());

		// check if we are about to run out of time at different steps
		// in the loop
		checkTimeThrowException(elapsedTimer);

		// 2. Cross over
		// we need to repopulate back to population size
		// since tournament selection halved it, this will double it
		// we could also use a better function that randomly selects
		// parents until we have enough children
		// System.out.println("d");
		ArrayList<Types.ACTIONS[]> populationCrossOver = new ArrayList<Types.ACTIONS[]>();
		for (int i = 0; i < population.size(); i++) {
		    checkTimeThrowException(elapsedTimer);
		    ACTIONS[] first;
		    ACTIONS[] second;
		    // System.out.println("e");
		    if (i == population.size() - 1) {
			first = population.get(i);
			second = population.get(0);
		    } else {
			first = population.get(i);
			second = population.get(i + 1);
		    }
		    // System.out.println("f");
		    ACTIONS[] firstChild = new ACTIONS[first.length];
		    ACTIONS[] secondChild = new ACTIONS[first.length];
		    int crossoverPoint = randomGenerator.nextInt(first.length);
		    for (int j = 0; j < first.length; j++) {
			if (j > crossoverPoint) {
			    firstChild[j] = second[j];
			    secondChild[j] = first[j];
			} else {
			    firstChild[j] = second[j];
			    secondChild[j] = first[j];
			}
		    }
		    // System.out.println("g");
		    populationCrossOver.add(firstChild);
		    populationCrossOver.add(secondChild);
		}
		// System.out.println("h");
		population = populationCrossOver;
		// System.out.println("Crossover done: " +
		// elapsedTimer.elapsedMillis());
		// check if we are about to run out of time at different steps
		// in the loop
		checkTimeThrowException(elapsedTimer);

		// 3. Mutation
		// System.out.println("i");
		for (ACTIONS[] actions : population) {
		    checkTimeThrowException(elapsedTimer);
		    for (int i = 0; i < actions.length; i++) {
			float roll = randomGenerator.nextFloat();
			if (roll < MUTATION_RATE) {
			    int rollInt = randomGenerator.nextInt(numActions);
			    actions[i] = stateObs.getAvailableActions().get(
				    rollInt);
			}
		    }
		}
		// System.out.println("Mutation done: " +
		// elapsedTimer.elapsedMillis());
		// check if we are about to run out of time at different steps
		// in the loop
		checkTimeThrowException(elapsedTimer);

	    }

	}
    }

    /**
     * Basic tournament sselection with tournament size 2 Selects n/2 members,
     * rounded up
     * 
     * Since this calculates actual state scores, it will also return the best
     * action found This means we won't have to check evaluation scores twice
     * (since evaluating each state is time consuming, depending on the
     * heuristic and number of actions)
     * 
     * Runs in parallel
     * 
     * @param stateObs
     * @param population
     * @return
     * @throws TimeoutException
     */
    private Pair<ArrayList<ACTIONS[]>, ACTIONS> tournamentSelectionSimple(
	    StateObservation stateObs, ArrayList<ACTIONS[]> population,
	    ElapsedCpuTimer elapsedTimer) throws TimeoutException {

	double bestScore = -9999999999.0;
	Pair<ArrayList<ACTIONS[]>, ACTIONS> ret = new Pair<ArrayList<ACTIONS[]>, ACTIONS>(
		new ArrayList<ACTIONS[]>(), null);

	for (int i = 0; i < population.size(); i += 2) {
	    // System.out.println("Beggining selection step: " +
	    // elapsedTimer.elapsedMillis());
	    checkTimeThrowException(elapsedTimer);
	    if (i == population.size() - 1) {
		ret.first.add(population.get(i));
		break;
	    } else {
		ACTIONS[] first = population.get(i);
		ACTIONS[] second = population.get(i + 1);
		// System.out.println("Got actions: " +
		// elapsedTimer.elapsedMillis());
		// create two state copies to evaluate
		StateObservation firstState = stateObs.copy();
		StateObservation secondState = stateObs.copy();
		checkTimeThrowException(elapsedTimer);
		// System.out.println("Copied state: " +
		// elapsedTimer.elapsedMillis());
		// play out the actions in the population members so we can
		// evaluate their final states.
		for (int j = 0; j < first.length; j++) {
		    firstState.advance(first[j]);
		    secondState.advance(second[j]);
		}
		checkTimeThrowException(elapsedTimer);
		// System.out.println("Advanced state: " +
		// elapsedTimer.elapsedMillis());
		double scoreFirst = stateHeuristic.evaluateState(firstState);
		// System.out.println("Evaluated first state: " +
		// elapsedTimer.elapsedMillis());
		if (scoreFirst > bestScore) {
		    bestScore = scoreFirst;
		    ret.second = first[0];
		}
		checkTimeThrowException(elapsedTimer);
		double scoreSecond = stateHeuristic.evaluateState(secondState);
		// System.out.println("Evaluated sceond state: " +
		// elapsedTimer.elapsedMillis());
		if (scoreSecond > bestScore) {
		    bestScore = scoreSecond;
		    ret.second = second[0];
		}
		if (scoreFirst > scoreSecond) {
		    ret.first.add(first);
		} else {
		    ret.first.add(second);
		}
		checkTimeThrowException(elapsedTimer);
	    }
	}
	return ret;
    }

    /**
     * Throws an exception if we are about to run out of time
     * 
     * @param elapsedTimer
     */
    private void checkTimeThrowException(ElapsedCpuTimer elapsedTimer)
	    throws TimeoutException {
	if (elapsedTimer.elapsedMillis() > BREAK_TIME_MS) {
	    throw new TimeoutException();
	}
    }

    /**
     * Generates a random initial population every step
     * 
     * @param stateObs
     * @param numToGenerate
     * @return
     */
    private ArrayList<ACTIONS[]> generatePopulation(StateObservation stateObs,
	    int numToGenerate) {
	ArrayList<ACTIONS[]> ret = new ArrayList<ACTIONS[]>();
	for (int subject = 0; subject < numToGenerate; subject++) {
	    ACTIONS[] actionArray = new ACTIONS[LOOKAHEAD_DEPTH];
	    for (int action = 0; action < LOOKAHEAD_DEPTH; action++) {
		int roll = randomGenerator.nextInt(numActions);
		actionArray[action] = stateObs.getAvailableActions().get(roll);
	    }
	    ret.add(actionArray);
	}
	return ret;
    }

    /**
     * Attempts to find a solution to puzzle games using breadth first search
     * Simple method that should perform better than GA in pure puzzle games (no
     * state changes with time)
     * 
     * @param stateObs
     * @return
     */

    private void breadthFirstSearch(StateObservation stateObs) {
	Queue<TreeNode> Q = new ArrayDeque<TreeNode>();
	Q.add(new TreeNode(stateObs, null));
	TreeNode winState = null;
	while (Q.size() > 0) {
	    if(gameType == GAME_TYPES.REALTIME) return;
	    TreeNode current = Q.poll();
	    for (TreeNode child : current.generateChildren()) {
		if (child.content.getGameWinner() == Types.WINNER.PLAYER_WINS) {
		    // found the winning node
		    winState = child;
		    break;
		} else {
		    Q.add(child);
		}
	    }
	}
	// reconstruct the winning sequence
	Deque<Types.ACTIONS> winSequence = new ArrayDeque<Types.ACTIONS>();
	while (winState != null) {
	    winSequence.addFirst(winState.content.getAvatarLastAction());
	    winState = winState.parent;
	}
	bestPuzzleSolution = winSequence;
    }

}
