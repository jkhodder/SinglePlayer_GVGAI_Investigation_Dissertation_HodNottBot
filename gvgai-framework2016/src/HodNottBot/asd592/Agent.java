package HodNottBot.asd592;

/**
 * Created by albke on 20.06.2017.
 */

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;


/**
 * Created by Adrian on 27.02.2017.
 */
public class Agent extends AbstractPlayer {

    private Controller controller;
    private BFS bfs;
    private GA ga;


    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        initialize(stateObs, elapsedTimer);
    }

    public void initialize(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        this.bfs = new BFS(stateObservation, elapsedCpuTimer, this);
        this.ga = new GA(stateObservation, elapsedCpuTimer);


        //pick the best controller by checking if a game is deterministic

        if (isDeterministic(stateObservation)) {
            controller = bfs;
        } else {
            controller = ga;
        }


    }

    public boolean isDeterministic(StateObservation stateObservation) {
        StateObservation stateObs = stateObservation.copy();
        boolean deterministic = false;
        for (int i = 0; i < 10; i++) {
            stateObs.advance(Types.ACTIONS.ACTION_NIL);
        }
        if (stateObs.getNPCPositions() == null && stateObs.isAvatarAlive() && stateObs.getAvatarPosition().equals(stateObservation.getAvatarPosition())) {
            if (stateObs.getMovablePositions() != null) {

                if (checkForMovement(stateObs, stateObservation)) {
                    deterministic = true;
                } else {
                    deterministic = false;
                }
            } else {
                deterministic = true;
            }

        }

        return deterministic;
    }

    public boolean compareObservations(ArrayList<Observation>[] oldObservations, ArrayList<Observation>[] newObservations) {
        if (oldObservations == null && newObservations != null) {
            return false;
        } else if (newObservations == null && oldObservations != null) {
            return false;
        } else if (oldObservations != null && newObservations != null) {
            if (oldObservations.length != newObservations.length) {
                return false;
            } else {
                for (int i = 0; i < oldObservations.length; i++) {
                    for (int j = 0; j < oldObservations[i].size(); j++) {
                        if (oldObservations[i].size() != newObservations[i].size()) {
                            return false;
                        }
                    }
                }
            }
        }


        return true;
    }

    //check if there are moving objects

    public boolean checkForMovement(StateObservation stateObservation, StateObservation currentStateObs) {
        ArrayList<Observation>[] movables = stateObservation.getMovablePositions();
        ArrayList<Observation>[] currentMovables = currentStateObs.getMovablePositions();

        if (!compareObservations(movables, currentMovables)) {
            return false;
        } else {

            for (int i = 0; i < movables.length; i++) {
                for (int j = 0; j < movables[i].size(); j++) {
                    if (!movables[i].get(j).position.equals(currentMovables[i].get(j).position)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        Types.ACTIONS bestAction;
        bestAction = controller.getAction(stateObs, elapsedTimer);
        return bestAction;

    }

    public void changeController() {
        this.controller = ga;
    }


}

