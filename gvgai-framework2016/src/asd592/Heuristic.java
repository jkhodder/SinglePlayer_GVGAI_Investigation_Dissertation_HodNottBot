package asd592;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;

/**
 * Created by Adrian on 05.04.2017.
 */
public class Heuristic {
    ArrayList<Observation>[] immovablePositions;
    ArrayList<Observation>[] portalPositions;
    ArrayList<Observation>[] npcPositions;
    Vector2d avatarPosition;
    Map map;

    public Heuristic(StateObservation stateObservation) {
        this.portalPositions = stateObservation.getPortalsPositions();
        this.map = new Map(stateObservation);
    }

    public void update(StateObservation currentStateObs) {
        this.map.update(currentStateObs);
    }

    //calculate the score of an action sequence by evaluating the avatar position with the help of the map and
    //considering other factors like score and resource count

    public int calculateScore(StateObservation stateObservation, StateObservation currentStateObs) {
        int score;
        this.avatarPosition = stateObservation.getAvatarPosition();
        this.npcPositions = stateObservation.getNPCPositions();
        int gameScore = (int) stateObservation.getGameScore() - (int) currentStateObs.getGameScore();
        int healthLoss = currentStateObs.getAvatarHealthPoints() - stateObservation.getAvatarHealthPoints();
        int resourcesGained = stateObservation.getAvatarResources().size() - currentStateObs.getAvatarResources().size();

        if (stateObservation.isGameOver()) {
            if (stateObservation.getGameWinner() == Types.WINNER.PLAYER_WINS) {
                score = 10000 + gameScore * 2;
            } else {
                score = -9999 + gameScore * 2;
            }
        } else {
            score = map.getScore(avatarPosition) + gameScore * 2 - healthLoss * 3 + resourcesGained;

        }

        return score;

}}
