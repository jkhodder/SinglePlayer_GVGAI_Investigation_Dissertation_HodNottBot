package HodNottBot.UrbanSk;

import java.util.ArrayList;
import java.util.HashMap;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;


/**
 * Basic heuristic function for evaluating state
 * TODO: prefer states that are closer to objects than further away
 * @author urban.skvorc
 *
 */
public class CustomStateHeuristic extends StateHeuristic {

    public CustomStateHeuristic(StateObservation stateObs) {

    }

    public double evaluateState(StateObservation stateObs) {
	Vector2d avatarPosition = stateObs.getAvatarPosition();
	if (stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS) {
            return 1000000000;
        } else if (stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
            return -999999999;
        } else {
            return stateObs.getGameScore();
        }
    }


}


