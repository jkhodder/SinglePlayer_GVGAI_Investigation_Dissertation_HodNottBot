package asd592;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Created by Adrian on 27.02.2017.
 */
public class GA extends Controller {


    public Random randomGenerator;
    private int populationSize = 50;
    private int[][] population;
    private HashMap<Integer, Types.ACTIONS> actionMap;
    private int depth = 8;
    ElapsedCpuTimer elapsedCpuTimer;
    private long BREAK_MS = 2;
    private int availableActionsSize;
    private Heuristic heuristic;
    private StateObservation currentStateObs;
    private int[] bestScore;
    private int[] losePenalty;
    private int[] endScore;
    private int[] immediateReward;
    private ArrayList<Integer> bestMoves;

    public GA(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.elapsedCpuTimer = elapsedTimer;
        randomGenerator = new Random();
        actionMap = new HashMap<>();
        this.heuristic = new Heuristic(stateObs);
        for (int i = 0; i < stateObs.getAvailableActions().size(); i++) {
            actionMap.put(i, stateObs.getAvailableActions().get(i));
        }
        availableActionsSize = stateObs.getAvailableActions().size();
        this.bestScore = new int[availableActionsSize];
        this.losePenalty = new int[availableActionsSize];
        this.endScore = new int[availableActionsSize];
        this.immediateReward = new int[availableActionsSize];
        this.bestMoves = new ArrayList<>();
        initPopulation();


    }

    /**
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS getAction(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.elapsedCpuTimer = elapsedTimer;
        this.currentStateObs = stateObs;
        Types.ACTIONS bestAction;
        movePopulation();
        heuristic.update(stateObs);
        resetValues();
        try {
            evaluate();
        } catch (Exception e) {

        }
        while (elapsedTimer.remainingTimeMillis() > BREAK_MS) {
            select();

        }
        bestAction = getBest();
        return bestAction;

    }

    public void initPopulation() {
        population = new int[populationSize][depth+1];
        for (int i = 0; i < populationSize; i++) {
            for (int j = 0; j < depth; j++) {
                population[i][j] = randomGenerator.nextInt(availableActionsSize);
            }

        }


    }

    //cut the first action of every sequence and move every action one step towards the front
    //add a random action at the end

    public void movePopulation() {
        for (int[] ints : population) {
            int j = ints[ints.length - 1];
            for (int i = ints.length - 2; i >= 0; i--) {
                int a = ints[i];
                ints[i] = j;
                j = a;

            }
            ints[depth - 1] = randomGenerator.nextInt(availableActionsSize);
        }
    }


    public void select() {
        ArrayList<int[]> rouletteWheel = rouletteWheelSelect();

        int k = randomGenerator.nextInt(rouletteWheel.size());
        int j = randomGenerator.nextInt(rouletteWheel.size());

        while (k == j) {
            j = randomGenerator.nextInt(rouletteWheel.size());
        }

        int[] parent1 = rouletteWheel.get(j);
        int[] parent2 = rouletteWheel.get(k);

        int parent1position = Arrays.asList(population).indexOf(parent1);
        int parent2position = Arrays.asList(population).indexOf(parent2);

        int[] child1 = recombine(parent1, parent2);
        int[] child2 = recombine(parent1, parent2);

        mutate(child1);
        mutate(child2);

        try {
            simulate(child1);
            simulate(child2);
        }
        catch (TimeoutException e){

        }

        if (child1[depth] > parent1[depth]) {
            population[parent1position] = child1;
        }
        if (child2[depth] > parent2[depth]) {
            population[parent2position] = child2;
        }

    }

    public void simulate(int[] sequence) throws TimeoutException {
        StateObservation stateObservation = currentStateObs.copy();
        if (elapsedCpuTimer.remainingTimeMillis() < BREAK_MS) {
            throw new TimeoutException("TimeOut");
        }
        for (int i = 0; i < depth; i++) {

            stateObservation.advance(actionMap.get(sequence[i]));
            if (i == 0) {
                immediateReward[sequence[0]] = heuristic.calculateScore(stateObservation, currentStateObs);
                if (stateObservation.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {
                    losePenalty[sequence[0]] = 5000;
                }

            }
        }
        int score = heuristic.calculateScore(stateObservation, currentStateObs);
        sequence[depth] = score;

    }

    //create an ArrayList with the members of the population. The better the fitness of a sequence the better the chance to be
    //picked for recombination

    public ArrayList<int[]> rouletteWheelSelect() {
        ArrayList<int[]> list = new ArrayList<>();
        for (int[] ints : population) {
            int count = ints[depth];
            if (count < 1) {
                count = 1;


            } else if (count * 3 > 50) {
                count = 50;
            } else {
                count = count * 3;

            }
            for (int i = 0; i < count; i++) {
                list.add(ints);

            }

        }
        return list;
    }

    //use uniform crossover to recombine two sequences

    public int[] recombine(int[] int1, int[] int2) {
        int[] ints = new int[depth + 1];
        for (int i = 0; i < ints.length; i++) {
            if (Math.random() < 0.5) {
                ints[i] = int1[i];
            } else {
                ints[i] = int2[i];
            }

        }
        return ints;

    }

    public void mutate(int[] sequence) {
        for (int i = 0; i < depth; i++) {
            if (Math.random() > 0.95) {
                sequence[i] = randomGenerator.nextInt(availableActionsSize);
            }
        }


    }

    public void evaluate() throws TimeoutException {
        for (int[] ints : population) {
            simulate(ints);
        }
    }

    public void resetValues() {
        bestMoves.clear();
        for (int i = 0; i < availableActionsSize; i++) {
            bestScore[i] = -999999999;
            losePenalty[i] = 0;
            endScore[i] = 0;
            immediateReward[i] = 0;
        }


    }

    //get the best action
    //if there is random movement, actions that are less likely to result in death are preferred

    public Types.ACTIONS getBest() {
        Types.ACTIONS action;

        int randomFactor = 0;
        for (int i = 0; i < availableActionsSize; i++) {
            randomFactor += losePenalty[i];
        }

        if (randomFactor != 0) {
            int score;
            for (int[] ints : population) {
                int actionNumber = ints[0];

                if (ints[depth] > bestScore[actionNumber]) {

                    bestScore[actionNumber] = ints[depth];

                }
            }
            for (int i = 0; i < availableActionsSize; i++) {
                endScore[i] = bestScore[i] - losePenalty[i] + immediateReward[i];

            }
            score = endScore[0];
            for (int i = 0; i < availableActionsSize; i++) {
                if (endScore[i] > score) {
                    bestMoves.clear();
                    bestMoves.add(i);
                    score = endScore[i];
                }
                if (endScore[i] == score) {
                    bestMoves.add(i);
                }
            }
            int randomNumber = randomGenerator.nextInt(bestMoves.size());
            int actionNumber = bestMoves.get(randomNumber);

            action = actionMap.get(actionNumber);
        } else {
            int score = -999999999;
            int actionNumber = -1;
            for (int[] sequence : population) {
                if (sequence[depth] > score) {
                    score = sequence[depth];
                    actionNumber = sequence[0];
                }
            }
            action = actionMap.get(actionNumber);
        }
        return action;
    }


}