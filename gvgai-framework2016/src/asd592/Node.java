package asd592;

import core.game.StateObservation;

import java.util.LinkedList;

/**
 * Created by albke on 20.06.2017.
 */
public class Node {

    private StateObservation stateObservation;
    private LinkedList<Integer> alreadyVisited;
    private LinkedList<Integer> stillToVisit;


    public Node(StateObservation stateObservation, LinkedList<Integer> linkedList, LinkedList<Integer> stillToVisit){
        this.stateObservation = stateObservation;
        this.alreadyVisited = linkedList;
        this.stillToVisit = stillToVisit;




    }
    public int getNextAction(){
        int i = stillToVisit.getFirst();
        stillToVisit.removeFirst();
        return i;
    }

    public void clear(){
        stillToVisit.clear();
        alreadyVisited.clear();
        stateObservation = null;



    }

    public StateObservation getStateObservation() {
        return stateObservation;
    }

    public LinkedList<Integer> getAlreadyVisited() {
        return alreadyVisited;
    }

    public LinkedList<Integer> getStillToVisit() {
        return stillToVisit;
    }
}
