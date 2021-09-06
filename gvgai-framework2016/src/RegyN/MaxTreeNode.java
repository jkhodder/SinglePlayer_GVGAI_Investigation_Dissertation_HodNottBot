package RegyN;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Utils;
import RegyN.Utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MaxTreeNode implements ITreeNode {
    MaxTreeNode parent;
    int depth;
    int maxDepth;
    List<MaxTreeNode> children;
    List<Types.ACTIONS> childActions;
    int numTests = 0;
    float localScore = 0;

    /// Ocena stanu w obecnym węźle
    float stateScore = 0;
    /// Ocena symulowanego stanu
    float simulationScore = 0;
    /// Współczynnik wpływu symulacji na wynik wyznaczony dla liścia
    float simCoefficient = 0.8f;
    int uninitiatedChildren;
    Random generator;
    double K = 1.41;
    StateObservation state = null; // NULL dla wszystkich poza rootem

    MaxTreeNode(){
        this.maxDepth = 10;
        this.depth = 0;
        this.parent = null;
        this.generator = new Random();
    }

    MaxTreeNode(int maxDepth, StateObservation obs) {
        // Dla korzenia nie robię symulacji
        this.maxDepth = AgentParameters.GetInstance().maxDepth;
        this.depth = 0;
        this.parent = null;
        this.generator = new Random();
        this.state = obs;
        this.stateScore = Utilities.EvaluateState(obs);
        this.localScore = stateScore;
    }

    MaxTreeNode(MaxTreeNode parent, StateObservation obs) {
        AgentParameters params = AgentParameters.GetInstance();
        this.maxDepth = params.maxDepth;
        this.depth = parent.depth + 1;
        this.parent = parent;
        this.generator = parent.generator;
        PositionHistory pos = PositionHistory.GetInstance();
        this.stateScore = Utilities.EvaluateState(obs) + pos.getLocationBias(obs); // TODO: Dodać możliwość włączania i wyłączania locationBias
        int result;
        if(params.rollSimulationCleverly && GameKnowledge.getInstance().type == GameType.Planar2D) {
            result = RollSimulationCleverly(obs, generator, true);
        }
        else{
            result = RollSimulation(obs, generator);
        }
        this.simulationScore = result;
        this.localScore = stateScore + simulationScore * simCoefficient;
        UpdateScoreUpwards(this.localScore);
    }
    
    private int RollSimulation(StateObservation obs, Random generator) {
        ArrayList<Types.ACTIONS> actions = obs.getAvailableActions();
        int d = depth;
        for (; d < maxDepth; d++) {
            if (obs.isGameOver())
                break;
            int choice = generator.nextInt(actions.size());
            obs.advance(actions.get(choice));
        }
        return Utilities.EvaluateState(obs, d);
    }

    // Symulacje prowadzone w taki sposób, że jeśli została wylosowana akcja prowadząca w ścianę, to losowane jest jeszcze raz
    // Możliwe jest, że za drugim razem wylosowane będzie to samo
    private int RollSimulationCleverly(StateObservation obs, Random generator){
        ArrayList<Types.ACTIONS> actions = obs.getAvailableActions();
        GameKnowledge knowledge = GameKnowledge.getInstance();
        int d = depth;
        for (; d < maxDepth; d++) {
            if (obs.isGameOver())
                break;
            int choice = generator.nextInt(actions.size());
            Position2D avatarPos = Position2D.GetAvatarPosition(obs);

            // Sprawdzam, co znajduje się w kierunku, który wybrał generator
            ArrayList<Observation> obstacles = new ArrayList<>();
            if(actions.get(choice) == Types.ACTIONS.ACTION_UP){
                obstacles = Position2D.GetObservations(obs, avatarPos.x, avatarPos.y-knowledge.avatarSpeed);
            }
            else if(actions.get(choice) == Types.ACTIONS.ACTION_DOWN){
                obstacles = Position2D.GetObservations(obs, avatarPos.x, avatarPos.y+knowledge.avatarSpeed);
            }
            else if(actions.get(choice) == Types.ACTIONS.ACTION_LEFT){
                obstacles = Position2D.GetObservations(obs, avatarPos.x-knowledge.avatarSpeed, avatarPos.y);
            }
            else if(actions.get(choice) == Types.ACTIONS.ACTION_RIGHT){
                obstacles = Position2D.GetObservations(obs, avatarPos.x+knowledge.avatarSpeed, avatarPos.y);
            }
            // Jeśli są tam jakieś ściany, to losuję jeszcze raz
            if(knowledge.CheckForWalls(obstacles)){
                choice = generator.nextInt(actions.size());
            }
            obs.advance(actions.get(choice));
        }
        return Utilities.EvaluateState(obs, d);
    }

    private int RollSimulationCleverly(StateObservation obs, Random generator, boolean useHistory){
        int result = RollSimulationCleverly(obs, generator);
        if(useHistory){
            result += PositionHistory.GetInstance().getLocationBias(obs);
        }
        return result;
    }

    public void Expand(StateObservation obs) {
        if(this.IsRoot()){
            obs = obs.copy();
        }
        ArrayList<Types.ACTIONS> actions = obs.getAvailableActions();
        if(actions.size() <= 0){  // Czasami z powodu niedeterminizmu gra kończy się wcześniej niż by się można spodziewać. Wtedy wracamy do korzenia.
            //int result = Utilities.EvaluateState(obs);
            UpdateUpwardsNoScore();
            //System.out.print(".");
            return;
        }

        if (children == null) {
            int choice = generator.nextInt(actions.size());
            obs.advance(actions.get(choice));
            children = new ArrayList<>();
            childActions = new ArrayList<>();
            for(int i=0; i< actions.size(); i++){
                children.add(null);
                childActions.add(actions.get(i));
            }
            children.set(choice, new MaxTreeNode(this, obs));
            uninitiatedChildren = actions.size() - 1;
        }
        else if (uninitiatedChildren > 0) {    // Ten węzeł ma nierozwinięte dzieci
            int choice = GetNthUninitialized(generator.nextInt(UninitiatedLeft())); // TODO: Sprawdzić, czy dałoby się UninitiatedLeft() zamienić na uninitiatedChildren
            obs.advance(actions.get(choice));
            children.set(choice, new MaxTreeNode(this, obs));
            uninitiatedChildren--;
        }
        else {
            int choice = ChooseChildToExpandUct(obs);
            obs.advance(actions.get(choice));
            children.get(choice).Expand(obs);
        }
    }

    // Ekspansja w taki sposób, żeby w ogóle nie brać pod uwagę ruchów idących prosto w ściany
    public void ExpandIntelligently(StateObservation obs) {
        if(this.IsRoot()){
            obs = obs.copy();
        }
        ArrayList<Types.ACTIONS> actions = obs.getAvailableActions();
        if(actions.size() <= 0){  // Czasami z powodu niedeterminizmu gra kończy się wcześniej niż by się można spodziewać. Wtedy wracamy do korzenia.
            //int result = Utilities.EvaluateState(obs);
            UpdateUpwardsNoScore();
            //System.out.print(".");
            return;
        }

        GameKnowledge knowledge = GameKnowledge.getInstance();
        Position2D avatarPos = Position2D.GetAvatarPosition(obs);
        if (children == null) {
            children = new ArrayList<>();
            childActions = new ArrayList<>();
            for(int i=0; i< actions.size(); i++){
                if(actions.get(i) == Types.ACTIONS.ACTION_UP){
                    if(knowledge.CheckForWalls(Position2D.GetObservations(obs, avatarPos.x, avatarPos.y-knowledge.avatarSpeed))){
                        continue;
                    }
                }
                else if(actions.get(i) == Types.ACTIONS.ACTION_DOWN){
                    if(knowledge.CheckForWalls(Position2D.GetObservations(obs, avatarPos.x, avatarPos.y+knowledge.avatarSpeed))){
                        continue;
                    }
                }
                else if(actions.get(i) == Types.ACTIONS.ACTION_LEFT){
                    if(knowledge.CheckForWalls(Position2D.GetObservations(obs, avatarPos.x-knowledge.avatarSpeed, avatarPos.y))){
                        continue;
                    }
                }
                else if(actions.get(i) == Types.ACTIONS.ACTION_RIGHT){
                    if(knowledge.CheckForWalls(Position2D.GetObservations(obs, avatarPos.x+knowledge.avatarSpeed, avatarPos.y))){
                        continue;
                    }
                }
                children.add(null);
                childActions.add(actions.get(i));
            }
            if(childActions.size() <= 0){   // TODO: Sprawdzić kiedy się tak dzieje
                //var result = Utilities.EvaluateState(obs);
                UpdateUpwardsNoScore();
                //System.out.print(",");
                return;
            }
            int choice = generator.nextInt(childActions.size());
            obs.advance(childActions.get(choice));
            children.set(choice, new MaxTreeNode(this, obs));
            uninitiatedChildren = childActions.size() - 1;
        }
        else if (uninitiatedChildren > 0) {    // Ten węzeł ma nierozwinięte dzieci
            int choice = GetNthUninitialized(generator.nextInt(UninitiatedLeft())); // TODO: Sprawdzić, czy dałoby się UninitiatedLeft() zamienić na uninitiatedChildren
            obs.advance(childActions.get(choice));
            children.set(choice, new MaxTreeNode(this, obs));
            uninitiatedChildren--;
        }
        else {
            if(childActions.size() <= 0){   // TODO: Sprawdzić kiedy się tak dzieje
                //var result = Utilities.EvaluateState(obs);
                UpdateUpwardsNoScore();
                //System.out.print(",");
                return;
            }
            int choice = ChooseChildToExpandUct(obs);
            obs.advance(childActions.get(choice));
            children.get(choice).ExpandIntelligently(obs);
        }
    }
    
    /**
     * Zwraca liczbę niezainicjalizowanych dzieci obecnego węzła
     */
    protected int UninitiatedLeft(){
        int left = 0;
        for (MaxTreeNode child : children) {
            if (child == null) {
                left++;
            }
        }
        return left;
    }
    
    /**
     * Zwraca indeks n-tego niezainicjalizowanego (==null) dziecka.
     * @param n numer niezainicjalizowanego dziecka
     * @return indeks n-tego niezainicjalizowanego dziecka
     */
    protected int GetNthUninitialized(int n){
        int i=0;
        for(; i<children.size(); i++){
            if(children.get(i) == null){
                if(n == 0) {
                    break;
                }
                n--;
            }
        }
        return i;
    }

    private float GetNodeValue(){
//        if(this.IsLeaf()){
//            return stateScore + simulationScore * simCoefficient;
//        }
//        return GetMaxChildScore();
        return localScore;
    }

    private float GetMaxChildScore(){
        float score = -Float.MAX_VALUE;
        for(MaxTreeNode c : children){
            if(c != null && c.GetNodeValue() > score){
                score = c.GetNodeValue();
            }
        }
        return score;
    }
    
    public int ChooseChildToExpandUct(StateObservation obs) {
        double maxScore = Double.MIN_VALUE;
        double minScore = Double.MAX_VALUE;
        for (MaxTreeNode child : this.children) {
            if (child.GetNodeValue() > maxScore)
                maxScore = child.GetNodeValue();
            if (child.GetNodeValue() < minScore)
                minScore = child.GetNodeValue();
        }
        int choice = 0;
        double chosenScore = Double.MIN_VALUE;
        for(int i = 0; i < children.size(); i++){
            double normalized = Utilities.NormalizeScore(children.get(i).GetNodeValue(), maxScore, minScore);
            double disturbed = Utils.noise(normalized, 0.000001d, generator.nextDouble());
            double score = disturbed / children.get(i).numTests + K * Math.sqrt(Math.log(this.numTests)/Math.log(children.get(i).numTests));
            if(score > chosenScore) {
                choice = i;
                chosenScore = score;
            }
        }
        // TODO: Sprawdzić, czy odkomentowanie tej sekcji ma sens (raczej nie)
//        Position2D avatarPos = Position2D.GetAvatarPosition(obs);
//        var knowledge = GameKnowledge.getInstance();
//        ArrayList<Observation> obstacles = new ArrayList<>();
//        if(childActions.get(choice) == Types.ACTIONS.ACTION_UP){
//            obstacles = Position2D.GetObservations(obs, avatarPos.x, avatarPos.y-knowledge.avatarSpeed);
//        }
//        else if(childActions.get(choice) == Types.ACTIONS.ACTION_DOWN){
//            obstacles = Position2D.GetObservations(obs, avatarPos.x, avatarPos.y+knowledge.avatarSpeed);
//        }
//        else if(childActions.get(choice) == Types.ACTIONS.ACTION_LEFT){
//            obstacles = Position2D.GetObservations(obs, avatarPos.x-knowledge.avatarSpeed, avatarPos.y);
//        }
//        else if(childActions.get(choice) == Types.ACTIONS.ACTION_RIGHT){
//            obstacles = Position2D.GetObservations(obs, avatarPos.x+knowledge.avatarSpeed, avatarPos.y);
//        }
//        // Jeśli są tam jakieś ściany, to losuję jeszcze raz
//        if(knowledge.CheckForWalls(obstacles)){
//            choice = generator.nextInt(childActions.size());
//        }
        return choice;
    }
    
    public void UpdateScoreUpwards(float score) {
        this.numTests++;
        if(this.localScore < score) {
            localScore = score;
        }
        if (!this.IsRoot()) {
            parent.UpdateScoreUpwards(score);
        }
    }

    public void UpdateUpwardsNoScore() {
        this.numTests++;
        if (!this.IsRoot()) {
            parent.UpdateUpwardsNoScore();
        }
    }
    
    @Override
    public boolean IsRoot() {
        return parent == null;
    }

    public boolean IsLeaf(){
        return children == null;
    }

    public boolean IsUnfinished(){
        return !IsLeaf() && UninitiatedLeft() != 0;
    }


    @Override
    public Types.ACTIONS GetBestScoreAction(boolean useHistory) {
        return childActions.get(GetBestScoreIndex(useHistory));
    }

    @Override
    public  int GetBestScoreIndex(boolean useHistory){
        if(!useHistory || !IsRoot()){
            return GetBestScoreIndex();
        }
        PositionHistory history = PositionHistory.GetInstance();
        double correctionFactor = 0.8;
        Position2D avatarPos = Position2D.GetAvatarPosition(state);
        float max = -Float.MAX_VALUE;
        int maxIndex = 0;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == null) {
                continue;
            }
            float score = (float)Utilities.DisturbScore(children.get(i).GetNodeValue());
            if(score > max){
                Types.ACTIONS action = childActions.get(i);
                Position2D newPosition = Position2D.ModifyPosition(avatarPos, action);
                score = score - history.Count(newPosition);
                if(history.Contains(newPosition)){
                    score = score > 0 ? (int)(score * correctionFactor) : (int)(score / correctionFactor);
                }
                if(score > max){
                    max = score;
                    maxIndex = i;
                }
            }
        }
        return maxIndex;
    }

    @Override
    public int GetBestScoreIndex() {
        float max = -Float.MAX_VALUE;
        int maxIndex = 0;
        for (int i = 0; i < children.size(); i++) {
            if(children.get(i) == null){
                continue;
            }
            float score = (float)Utilities.DisturbScore(children.get(i).GetNodeValue());
            if (score > max) {
                max = score;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    // Najlepszy średni wynik to pojęcie które nie istnieje dla MaxTreeNode, ponieważ zapisuje jedynie maksymalny wynik.
    // Funkcje związane ze średnim wynikiem zwracają zamiast tego po prostu najlepszy wynik.
    @Override
    public Types.ACTIONS GetBestAverageAction(boolean useHistory) {
        if (childActions.size() == 0) {
            System.out.println("Waste 5 seconds to avoid error and disqualify run");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            return Types.ACTIONS.ACTION_NIL;
        }
        return childActions.get(GetBestScoreIndex(useHistory));
    }

    @Override
    public int GetBestAverageIndex(boolean useHistory) {
        return GetBestScoreIndex(useHistory);
    }

    @Override
    public int GetBestAverageIndex() {
        return GetBestScoreIndex();
    }


    @Override
    public Types.ACTIONS GetMostVisitedAction(boolean useHistory) {
        return childActions.get(GetMostVisitedIndex(useHistory));
    }

    @Override
    public int GetMostVisitedIndex(boolean useHistory){
        if(!useHistory || !IsRoot()){
            return GetBestScoreIndex();
        }
        PositionHistory history = PositionHistory.GetInstance();
        Position2D avatarPos = Position2D.GetAvatarPosition(state);
        double max = -Double.MAX_VALUE;
        int maxIndex = 0;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == null) {
                continue;
            }
            double score = Utilities.DisturbScore(children.get(i).numTests);
            if(score > max){
                Types.ACTIONS action = childActions.get(i);
                Position2D newPosition = Position2D.ModifyPosition(avatarPos, action);
                score = score - history.Count(newPosition);
                if(score > max){
                    max = score;
                    maxIndex = i;
                }
            }
        }
        return maxIndex;
    }

    @Override
    public int GetMostVisitedIndex() {
        int max = Integer.MIN_VALUE;
        int maxIndex = 0;
        for (int i = 0; i < children.size(); i++) {
            if(children.get(i) == null){
                continue;
            }
            if (children.get(i).numTests > max) {
                max = children.get(i).numTests;
                maxIndex = i;
            }
        }
        return maxIndex;
    }
}
