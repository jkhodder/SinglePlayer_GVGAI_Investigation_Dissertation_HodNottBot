package asd592;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

/**
 * Created by albke on 20.06.2017.
 */
public class BFS extends Controller {

    public LinkedList<Integer> bestSequence;
    public LinkedList<Node> actionQueue;
    private HashMap<Integer, Types.ACTIONS> actionMap = new HashMap<>();
    //    private HashSet<Vector2d> visitedStates = new HashSet<>();
    private ArrayList<Long> visitedStates = new ArrayList<>();
//    private ArrayList<String> visitedStates = new ArrayList<>();
    private int actionNumber;
    private LinkedList<Integer> possibleActions;
    private final int MAX_NODES = 1500;
    private final int MAX_VISITED = 200;
    private final int MAX_STATES = 20000;
    private Agent agent;


    public BFS(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer, Agent agent) {
        this.actionQueue = new LinkedList<>();
        this.bestSequence = new LinkedList<>();
        this.possibleActions = new LinkedList<>();
        this.agent = agent;
        actionNumber = stateObservation.getAvailableActions().size();
        for (int i = 0; i < actionNumber; i++) {
            actionMap.put(i, stateObservation.getAvailableActions().get(i));
            possibleActions.add(i);
        }


        Node firstNode = new Node(stateObservation, new LinkedList<>(), copyLinkedList(possibleActions));
        actionQueue.add(firstNode);
        //      start(elapsedCpuTimer);


    }

    public Types.ACTIONS getAction(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
        start(elapsedCpuTimer);

        if (isStillDeterministic(stateObservation) && stateObservation.getGameTick() < 500) {
            if (bestSequence.size() == 0) {
                return action;
            } else {
                action = returnAction();
            }
        } else {
            this.agent.changeController();
        }


        return action;
    }

    //checks if the game is still deterministic. If not, the controller will be changed

    public boolean isStillDeterministic(StateObservation stateObservation) {
        if (stateObservation.getNPCPositions() != null) {
            return false;
        } else {
            return true;
        }

    }

    public void start(ElapsedCpuTimer elapsedCpuTimer) {
        while (actionQueue.size() > 0) {

            if (elapsedCpuTimer.remainingTimeMillis() < 5) {
                break;
            }
            checkNodeCount();

            Node node = actionQueue.getFirst();
            StateObservation stateObservation = node.getStateObservation().copy();
            int actionId = node.getNextAction();
            Types.ACTIONS action = actionMap.get(actionId);
            stateObservation.advance(action);
            LinkedList<Integer> alreadyVisited = copyLinkedList(node.getAlreadyVisited());
   /*         checkNode(node);
            if (elapsedCpuTimer.remainingTimeMillis() < 5) {
                break;
            }*/

            if (checkState(stateObservation)) {
                checkNode(node);
            } else {
                alreadyVisited.add(actionId);
                Node newNode = new Node(stateObservation, alreadyVisited, copyLinkedList(possibleActions));
                actionQueue.add(newNode);

                if (visitedStates.size() < MAX_STATES) {
                    visitedStates.add(calculateStateId(stateObservation));
                }
                if (stateObservation.isGameOver() && stateObservation.getGameWinner().equals(Types.WINNER.PLAYER_WINS)) {
                    bestSequence = copyLinkedList(newNode.getAlreadyVisited());
                    actionQueue.clear();
                }
                checkNode(node);
            }


        }


    }

    public void checkNodeCount() {
        if (actionQueue.size() > MAX_NODES) {
            int bestScore = -999999999;
            int mostEvents = 0;
            Iterator<Node> iterator = actionQueue.iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                int nodeScore = (int) node.getStateObservation().getGameScore();
                if (bestScore < nodeScore) {
                    bestScore = nodeScore;
                }
                int nodeEvent = node.getStateObservation().getEventsHistory().size();
                if (mostEvents < nodeEvent) {
                    mostEvents = nodeEvent;
                }
/*                if (node.getAlreadyVisited().size() > MAX_VISITED) {
                    node.clear();
                    iterator.remove();}*/
                else if (bestScore > nodeScore && mostEvents > nodeEvent) {
                    node.clear();
                    iterator.remove();
                }

            }
            //          System.out.println(actionQueue.size());
            while (actionQueue.size() > MAX_NODES) {
                actionQueue.removeLast();

            }
        }
    }

    public void checkNode(Node node) {

        if (node.getStillToVisit().size() == 0) {
            if (actionQueue.size() == 1) {
                bestSequence = copyLinkedList(node.getAlreadyVisited());
                actionQueue.remove(node);
                actionQueue.clear();
                node.clear();

            } else {
                actionQueue.remove(node);
                node.clear();
            }


        }

    }

    public Types.ACTIONS returnAction() {
        int actionNumber = bestSequence.getFirst();
        Types.ACTIONS action = actionMap.get(actionNumber);
        bestSequence.removeFirst();
        return action;
    }

    public boolean checkState(StateObservation stateObservation) {
        boolean checkState = false;

        if (stateObservation.isGameOver() && stateObservation.getGameWinner().equals(Types.WINNER.PLAYER_LOSES)) {

            checkState = true;
        } else if (visitedStates.contains(calculateStateId(stateObservation))) {
            checkState = true;
        }
        return checkState;

    }

    //create StateId to identify previously seen states

    public String createStateId(StateObservation stateObservation) {
        String id = "id: ";
        id += "ap: " + stateObservation.getAvatarPosition().x + "," + stateObservation.getAvatarPosition().y;
        id += "gs: " + stateObservation.getGameScore();
        id += "ao: " + stateObservation.getAvatarOrientation();
 //       id += "es: " + stateObservation.getEventsHistory().size();


        ArrayList<Observation>[][] observGrid = stateObservation.getObservationGrid();

        for (int y = 0; y < observGrid[0].length; y++) {
            for (int x = 0; x < observGrid.length; x++) {
                for (int i = 0; i < observGrid[x][y].size(); i++) {
                    Observation observ = observGrid[x][y].get(i);
                    id += y + "," + x;
                    id += "t: " + observ.itype;
                    id += "c: " + observ.category;
                }
            }
        }
        if (stateObservation.getMovablePositions() != null) {

            for (ArrayList<Observation> movables : stateObservation.getMovablePositions()) {
                for (Observation movable : movables) {
                    id += "im: " + movable.position.x + "," + movable.position.y + ";";
                }
            }
        }


        return id;
    }

    //method created by number27

    public long calculateStateId(StateObservation stateObs) {
        long h = 1125899906842597L;
        ArrayList<Observation>[][] observGrid = stateObs.getObservationGrid();

        for (int y = 0; y < observGrid[0].length; y++) {
            for (int x = 0; x < observGrid.length; x++) {
                for (int i = 0; i < observGrid[x][y].size(); i++) {
                    Observation observ = observGrid[x][y].get(i);

                    h = 31 * h + x;
                    h = 31 * h + y;
                    h = 31 * h + observ.category;
                    h = 31 * h + observ.itype;
                }
            }
        }

        h = 31 * h + (int) (stateObs.getAvatarPosition().x / stateObs.getBlockSize());
        h = 31 * h + (int) (stateObs.getAvatarPosition().y / stateObs.getBlockSize());
        h = 31 * h + stateObs.getAvatarType();
        h = 31 * h + stateObs.getAvatarResources().size();
        h = 31 * h + (int) (stateObs.getGameScore() * 100);

        return h;

    }


    public LinkedList<Integer> copyLinkedList(LinkedList<Integer> linkedList) {
        LinkedList<Integer> copiedLinkedList = new LinkedList<>();
        for (int i : linkedList) {
            copiedLinkedList.add(i);
        }
        return copiedLinkedList;
    }


}
