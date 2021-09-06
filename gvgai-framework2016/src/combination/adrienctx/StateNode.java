/**
 * Code written by Adrien Couetoux, acouetoux@ulg.ac.be It is free to use,
 * distribute, and modify. User: adrienctx Date: 12/01/2015
 * Modified by Saskia Friedrich, 01/12/2017
 */
package combination.adrienctx;


import java.util.*;

import combination.YoloState;
import tools.Vector2d;

class StateNode {

    private static final double HUGE_NEGATIVE = -10000000.0;

    public final List<YoloState> encounteredStates;

    public final StateNode[] children;

    public final int[] actionNbSimulations;

    private final double[] actionScores;

    private final Random randomGenerator;

    private int numberOfExits;

    private double cumulatedValueOnExit;

    private double passingValue;

    public int numberOfSimulations;

    private double maxScore;

    private final double rawScore;

    //Parent objects:
    public StateNode parentNode;

    public int parentAction;

    private final TreeSearchPlayer parentTree;

    private IntDoubleHashMap[] features;

    private double locationBias;

    private double barycenterBias;
    
    private double valueWhenLastBacktracked;

    private double featureGridBias;

    public boolean[] actionPruned;

    private int gameTick;

    private Vector2d orientation;

    /**
     * Creates a new state node with one single state encountered
     *
     * @param _state , the encountered state
     */
    public StateNode(YoloState _yoloState, Random _random, TreeSearchPlayer _parentTree) {
        encounteredStates = new ArrayList<>();
        encounteredStates.add(_yoloState);
        gameTick = _yoloState.getGameTick();
        orientation = _yoloState.getAvatarOrientation();
        children = new StateNode[AdrienctxAgent.NUM_ACTIONS];
        actionNbSimulations = new int[AdrienctxAgent.NUM_ACTIONS];
        actionScores = new double[AdrienctxAgent.NUM_ACTIONS];
        actionPruned = new boolean[AdrienctxAgent.NUM_ACTIONS];
        randomGenerator = _random;

        parentTree = _parentTree;

        rawScore = _yoloState.getGameScore();

        features = new IntDoubleHashMap[parentTree.nbCategories];
        features = parentTree.getFeaturesFromStateObs(_yoloState);
        locationBias = parentTree.getLocationBias(_yoloState);
        barycenterBias = parentTree.getBarycenterBias(_yoloState);
        featureGridBias = parentTree.getFeatureGridBias(features);
    }

    private StateNode(YoloState _yoloState, Random _random, StateNode _parentNode) {
        encounteredStates = new ArrayList<>();
        encounteredStates.add(_yoloState);
        gameTick = _yoloState.getGameTick();
        orientation = _yoloState.getAvatarOrientation();
        children = new StateNode[AdrienctxAgent.NUM_ACTIONS];
        actionNbSimulations = new int[AdrienctxAgent.NUM_ACTIONS];
        actionScores = new double[AdrienctxAgent.NUM_ACTIONS];
        actionPruned = new boolean[AdrienctxAgent.NUM_ACTIONS];
        randomGenerator = _random;

        parentTree = _parentNode.parentTree;
        parentNode = _parentNode;

        rawScore = _yoloState.getGameScore();

        features = new IntDoubleHashMap[parentTree.nbCategories];
        features = parentTree.getFeaturesFromStateObs(_yoloState);
        locationBias = parentTree.getLocationBias(_yoloState);
        barycenterBias = parentTree.getBarycenterBias(_yoloState);
        featureGridBias = parentTree.getFeatureGridBias(features);
    }

    public IntDoubleHashMap[] getFeatures(){
        return this.features;
    }

    public IntDoubleHashMap[] getCopyOfFeatures(){
        IntDoubleHashMap[] copy = new IntDoubleHashMap[parentTree.nbCategories];

        for (int i=0; i < parentTree.nbCategories; i++){
            copy[i] = new IntDoubleHashMap(features[i]);
        }

        return(copy);
    }

    private double getNodeValue() {
        if(parentTree.useValueApproximation){
            return rawScore + parentTree.V_approximator.getBasisFunctionLinearApproximation(parentTree.V_approximator.getBasisFunctionsFromFeatures(features), parentTree.V_approximator.getWeights()) + locationBias * parentTree.getLocationBiasWeight() + barycenterBias * parentTree.getBarycenterBiasWeight() + featureGridBias * parentTree.getFeatureGridWeight();
        }
        else{
            return rawScore + locationBias * parentTree.getLocationBiasWeight() + barycenterBias * parentTree.getBarycenterBiasWeight();
        }
    }

    /**
     * Adds a new action node to the children if this state node
     *
     * @param _currentObservation , the current state observation to use in
     *                            the simulator (i.e. the advance method from Forward Model)
     */
    public StateNode addStateNode(YoloState _currentYoloStateObservation, int actionIndex) {

        StateNode newStateNode = new StateNode(_currentYoloStateObservation, randomGenerator, this);

        newStateNode.parentAction = actionIndex;
        children[actionIndex] = newStateNode;
        actionNbSimulations[actionIndex] += 1;

        return newStateNode;
    }

    /**
     * returns the selected action node
     */
    public int selectRandomAction() {
        int bestActionIndex = 0;
        double bestValue = -1;
        double x;
        for (int i = 0; i < children.length; i++) {
            x = randomGenerator.nextDouble();
            if (x > bestValue && children[i] != null) {
                bestActionIndex = i;
                bestValue = x;
            }
        }
        return bestActionIndex;
    }

    /**
     * returns the selected action node
     */
    public int selectAction() {
        int bestActionIndex = 0;
        double bestValue = HUGE_NEGATIVE;
        double x;
        double actionValue;

        for (int i = 0; i < children.length; i++) {
            x = randomGenerator.nextDouble();
            actionValue = this.actionScores[i];
            if ((children[i] != null) && (actionValue + (x / 1000.0) > bestValue)) {
                bestActionIndex = i;
                bestValue = actionValue + (x / 1000.0);
            }
        }
        return bestActionIndex;
    }

    /**
     * Updates the data stored in this state node - TODO: store more than just
     * state observations; should also store instant values
     */
    public void updateData(YoloState _yoloState) {
        encounteredStates.add(_yoloState);
    }

    public int getMostVisitedAction() {
        int bestActionIndex = 0;
        double bestNumberOfVisits = -1;
        for (int i = 0; i < children.length; i++) {
            if ((double) actionNbSimulations[i] > bestNumberOfVisits && children[i] != null) {
                bestActionIndex = i;
                bestNumberOfVisits = (double) actionNbSimulations[i];
            }
        }
        return bestActionIndex;
    }

    public int getHighestScoreAction() {
        int bestActionIndex = 0;
        double bestScore = HUGE_NEGATIVE;
        for (int i = 0; i < children.length; i++) {
            double x = randomGenerator.nextDouble();
            if (((actionScores[i] + (x / 100000.0)) > bestScore) && (children[i] != null)) {
                bestActionIndex = i;
                bestScore = actionScores[i] + x / 100000.0;
            }
        }
        return bestActionIndex;
    }

    public void backPropagateData(YoloState _yoloState, ArrayList<IntDoubleHashMap[]> _visitedFeatures, ArrayList<Double> _visitedScores) {
        boolean gameOver = _yoloState.isGameOver();
        double _rawScore = _yoloState.getGameScore();

        if (parentTree.useValueApproximation) {
            double cumulatedDiscountedScores = 0.0;
            for (int i = 0; i < _visitedScores.size() - 1; i++) {
                cumulatedDiscountedScores = parentTree.discountFactor * cumulatedDiscountedScores + (_visitedScores.get(i) - _visitedScores.get(i + 1));
            }
        }

        if (gameOver) {
            this.numberOfExits += 1;
            cumulatedValueOnExit += parentTree.getValueOfState(_yoloState);
        }

        StateNode currentNode = this;

        while (currentNode != null) {
            currentNode.numberOfSimulations += 1;
            if (!currentNode.isLeaf()) {
            	double bestScore = HUGE_NEGATIVE;
                for (int i = 0; i < currentNode.children.length; i++) {
                    if (currentNode.children[i] != null) {
                        if (currentNode.actionScores[i] > bestScore) {
                            bestScore = currentNode.actionScores[i];
                        }
                    }
                }
                currentNode.maxScore = bestScore;
            }

            if (currentNode.parentNode != null) {
                currentNode.valueWhenLastBacktracked = currentNode.getNodeValue();
                currentNode.passingValue = currentNode.getNodeValue() - currentNode.parentNode.getNodeValue() + parentTree.discountFactor * currentNode.maxScore;
                int _actionIndex = currentNode.parentAction;
                currentNode.parentNode.actionScores[_actionIndex] = (currentNode.cumulatedValueOnExit / (double) currentNode.numberOfSimulations) + ((double) (currentNode.numberOfSimulations - currentNode.numberOfExits) / (double) currentNode.numberOfSimulations) * currentNode.passingValue;
            }

            currentNode = currentNode.parentNode;
        }
    }

    public void puzzleBackProp() {
        YoloState _yoloState = this.encounteredStates.get(0);
        boolean gameOver = _yoloState.isGameOver();
        double _rawScore = _yoloState.getGameScore();
        StateNode currentNode = this;

        if (gameOver) {
            this.numberOfExits += 1;
            cumulatedValueOnExit += parentTree.getValueOfState(_yoloState);
            currentNode.maxScore = parentTree.getValueOfState(_yoloState);
        }
        else{
            currentNode.maxScore = _rawScore;
        }


        while (currentNode != null) {
            currentNode.numberOfSimulations += 1;
            if (!currentNode.isLeaf()) {
                double bestScore = HUGE_NEGATIVE;
                for (int i = 0; i < currentNode.children.length; i++) {
                    if (currentNode.children[i] != null) {
                        if (currentNode.actionScores[i] > bestScore) {
                            bestScore = currentNode.actionScores[i];
                        }
                    }
                }
                if(bestScore > currentNode.maxScore){
                    currentNode.maxScore = 1.0 * bestScore;
                }
            }

            if (currentNode.parentNode != null) {
                int _actionIndex = currentNode.parentAction;
                currentNode.parentNode.actionScores[_actionIndex] = currentNode.maxScore;
                if(!currentNode.notFullyExpanded()){
                    currentNode.parentNode.actionPruned[_actionIndex] = currentNode.allChildrenPruned();
                }
            }

            currentNode = currentNode.parentNode;
        }
    }
    /**
     * returns true if and only if the
     */
    public boolean notFullyExpanded() {
        for (StateNode children1 : children) {
            if (children1 == null) {
                return true;
            }
        }
        return false;
    }

    public boolean allChildrenPruned(){
        for (boolean b : actionPruned){
            if(b == false){
                return false;
            }
        }
        return true;
    }

    /**
     * returns true if and only if the
     */
    private boolean isLeaf() {
        for (StateNode children1 : children) {
            if (children1 != null) {
                return false;
            }
        }
        return true;
    }

    public void printTree(int depth) {
        System.out.format("%n ##### Printing tree at depth %d and nbsims %d %n", depth, numberOfSimulations);
        for (int i = 0; i < children.length; i++) {
            if (children[i] != null) {
                System.out.format("%n ~~~~~~~~~~~~ action number %d :", i);
                System.out.format("%n nbSims, score, avgOnExit, PassingV, maxScore, nbExists, nodeValue, tick: %d, %f, %f, %f, %f, %d, %f, %d", actionNbSimulations[i], actionScores[i], children[i].cumulatedValueOnExit / (double) children[i].numberOfExits, children[i].passingValue, children[i].maxScore, children[i].numberOfExits, children[i].getNodeValue(), children[i].encounteredStates.get(0).getGameTick());
            }
        }
    }

    public double getActionScore(int index){
        return actionScores[index];
    }

}
