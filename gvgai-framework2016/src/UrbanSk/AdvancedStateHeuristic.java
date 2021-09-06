package UrbanSk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

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
public class AdvancedStateHeuristic extends StateHeuristic{

    public AdvancedStateHeuristic(StateObservation stateObs) {
	visited = new HashMap<Integer, Boolean>();
	visitedLocations=new HashMap<String, Integer>();
    }

    public double evaluateState(StateObservation stateObs) {
	if (stateObs.getGameTick() % 200 == 0){
	    visited.clear();
	    visitedLocations.clear();
	}
	
	
	int visitedLocationPenalty=0;
	if (visitedLocations.containsKey(stateObs.getAvatarPosition().toString())){
	    visitedLocationPenalty=visitedLocations.get(stateObs.getAvatarPosition().toString());
	//unsure if this should be in the agent or in the heuristic
	//    visitedLocations.put(stateObs.getAvatarPosition().toString(), visitedLocationPenalty+1000);
	//    //System.out.println(visitedLocationPenalty);
	} //else {
	    //visitedLocations.put(stateObs.getAvatarPosition().toString(), 1000);
	//}
	if (stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS) {
            return 1000000000;
        } else if (stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
            return -999999999;
        }
	Vector2d avatarPosition = stateObs.getAvatarPosition();
	//gets npc positions and portal positions sorted by proximity to the player
	int index=0;
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPosition);
        ArrayList<Observation>[] resourcePositions = stateObs.getResourcesPositions(avatarPosition);
        int numResources=0;
        HashMap<Integer, Integer> resources = stateObs.getAvatarResources();
        for (Entry <Integer,Integer> e: resources.entrySet()){
            numResources+=e.getValue();
        }
        
        if (portalPositions!=null){
            for (ArrayList<Observation> portals : portalPositions) {
        	for(index=0;index<portals.size();index++){
                    if(portals.size() > index)
                    {
                        if (visited.containsKey(portals.get(index).obsID)){
                    	    continue;
                        }
                        double closestPortalDist=portals.get(index).sqDist / 10000;
                        if (closestPortalDist<0.1){
                            visited.put(portals.get(index).obsID, true);
                           
                        }
                        return  - closestPortalDist + 100*stateObs.getGameScore() + 1000*numResources -  visitedLocationPenalty;   //This is the (square) distance to the closest portal
                    }
        	}
            }
        }
        
        double minDistance = 0;
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPosition);
        if (npcPositions!=null){
            for (ArrayList<Observation> npcs : npcPositions) {
        	for(index=0;index<npcs.size();index++){
        	    if(npcs.size() > 0)
                    {
                        if (visited.containsKey(npcs.get(index).obsID)){
                    	continue;
                        }
                        double closestNPCDist=npcs.get(index).sqDist/10000;
                        //once we are close enough to a NPC, the win/score/lose heuristic will take care of the rest
                        //we don't wan't to get to close by default, since touching them might lose us the game, and
                        //they can move in unpredictable directions
                        if (closestNPCDist<5){
                            visited.put(npcs.get(index).obsID, true);
                            //System.out.println("PUT VISITED" + npcs.get(index).obsID);
                        }
                        
                        return  - closestNPCDist + 100*stateObs.getGameScore()+1000*numResources -  visitedLocationPenalty;   //This is the (square) distance to the closest NPC.
                    }
        	}
            }
        }
        
       
        
        double minResDist = 0;
        if (resourcePositions!=null){
            for (ArrayList<Observation> res : resourcePositions) {
        	for(index=0;index<res.size();index++){
        	    if(res.size() > 0)
                    {
                        if (visited.containsKey(res.get(index).obsID)){
                    	continue;
                        }
                        minResDist=res.get(index).sqDist/10000;
                        if (res.get(index).sqDist<0.1){
                            visited.put(res.get(index).obsID, true);
                          
                        }
                        
                        return  - minResDist + 10*stateObs.getGameScore()+ 100*numResources -  visitedLocationPenalty;   //This is the (square) distance to the closest NPC.
                    }
        	}
            }
        }
        //System.out.println("Using score");
        double score = stateObs.getGameScore();
        /*
         * Importance:
         * 1. Win/Lose
         * 2. Portals
         * 3. Num NPCs
         * 5. Npcs
         * 6. Resources
         * 7. Score
         */
        return score+numResources -  visitedLocationPenalty / 500;
    }


}


