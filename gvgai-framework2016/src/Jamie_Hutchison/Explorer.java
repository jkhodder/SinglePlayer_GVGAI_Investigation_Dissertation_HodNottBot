package Jamie_Hutchison;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tools.pathfinder.Node;
import tools.pathfinder.PathFinder;

public class Explorer {
	
	public ArrayList<Vector2d> visitedPositions;
	public ArrayList<Vector2d> positionsToVisit;
	public boolean allVisited;
	
    private Random m_rnd;
    private Types.ACTIONS[] actions;
    private int num_actions;
    
    private boolean earlyWinnerFound;
    private Vector2d startingPosition;
    private PathFinder pf;
    
    private HashMap<Vector2d, Double> winningPositions;
     
    private boolean retreat;
    private boolean closeToDeath;
    private boolean findExit;
    
    private Vector2d closestUnvisitedPosition;
    
    public Explorer(int num_actions, Types.ACTIONS[] actions, StateObservation stateObs) {
    	    	
    	this.actions = actions;
    	this.num_actions = num_actions;
    	visitedPositions = new ArrayList<>();
    	positionsToVisit = new ArrayList<>();
    	winningPositions = new HashMap<>();
    	
    	earlyWinnerFound = false;
    	closestUnvisitedPosition = null;
    	startingPosition = stateObs.getAvatarPosition();
    	ArrayList<Integer> obstracleTypes = new ArrayList<>();
        obstracleTypes.add(0);
        pf = new PathFinder(obstracleTypes);
        pf.run(stateObs);
		
    }
	
	public int chooseAction(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
		closeToDeath = false;
		m_rnd = new Random();
		allVisited = true;
		
		ArrayList<Integer> unvisitedPositions = new ArrayList<>();
		
        for (int i = 0; i < actions.length; i++) {
        	
            StateObservation stCopy = stateObs.copy();
            stCopy.advance(actions[i]);
            
 
            if(stCopy.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
            	closeToDeath = true;
            	return 0;
            }
            
            if(stCopy.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
            	closeToDeath = true;
            	return 0;
            }
            
            
            if(stCopy.getGameWinner() == Types.WINNER.PLAYER_WINS && stateObs.getGameTick() < 700) {

            	winningPositions.put(stateObs.getAvatarPosition(), stCopy.getGameScore() - stateObs.getGameScore());
            	earlyWinnerFound = true;
            	closestUnvisitedPosition = selectClosestUnvisitedPosition(stateObs);
            	return getActionToDestination(stateObs, stateObs.getAvatarPosition(), closestUnvisitedPosition);
            }
            

            if(!visitedPositions.contains(stCopy.getAvatarPosition()) && stCopy.getGameWinner() != Types.WINNER.PLAYER_WINS) {
            	allVisited = false;
            	unvisitedPositions.add(i);
            }  
        }
		
		
		if(stateObs.getGameTick() > 700 && !winningPositions.isEmpty()) {
			retreat = true;
		}
		
		if(stateObs.getGameTick() > 900) {
			retreat = false;
		}
		
		if(stateObs.getGameTick() > 1500) {
			findExit = true;
		}
		
		
        if(!winningPositions.isEmpty() && retreat) {
        	Vector2d bestExit = Collections.max(winningPositions.entrySet(), Map.Entry.comparingByValue()).getKey();
        	return getActionToDestination(stateObs, stateObs.getAvatarPosition(), bestExit);
        }
		
		if(earlyWinnerFound) {
			return getActionToDestination(stateObs, stateObs.getAvatarPosition(), closestUnvisitedPosition);
		}
		
		if(closestUnvisitedPosition == null || hasPositionBeenVisited(closestUnvisitedPosition)) {
        	closestUnvisitedPosition = selectClosestUnvisitedPosition(stateObs);
		}
         
        if(positionsToVisit.size() != 0 && findExit) {
        allVisited = false;
        return getActionToDestination(stateObs, stateObs.getAvatarPosition(), closestUnvisitedPosition);
        }
        
        if(unvisitedPositions.size() != 0) {
        
        int randomAction = m_rnd.nextInt(unvisitedPositions.size());
        return unvisitedPositions.get(randomAction);
        }
          
        return 0;

	}
	
	private int getActionToDestination(StateObservation stateObs, Vector2d playerPos, Vector2d goalPos){
        Vector2d playerPosition = new Vector2d(playerPos);
        Vector2d goalPosition = new Vector2d(goalPos);
        int blocksize = stateObs.getBlockSize();
        playerPosition.mul(1.0 / blocksize);
        goalPosition.mul(1.0 / blocksize);
        
        ArrayList<Node> path = pf.getPath(playerPosition, goalPosition);
        
        if(path !=null) {
         
        Vector2d nextPath = path.get(0).position;
        return getActionFromPosition(playerPosition, nextPath);
        }
        
        earlyWinnerFound = false;
        retreat = false;
		return 0;
	}
	
    private int getActionFromPosition(Vector2d playerPos, Vector2d goalPos){
    	
        Vector2d moveVector = new Vector2d(goalPos.x - playerPos.x, goalPos.y - playerPos.y);
        ACTIONS move = Types.ACTIONS.fromVector(moveVector);
        for (int action = 0; action < num_actions; action++) {
				if (move == actions[action]) {
					return action;
				}
			}
        return 0;
    }
    
    public boolean highPrioriy() {
    	if(retreat || earlyWinnerFound) {
    		return true;
    	}
    	
    	return false; 
    }
    
    public boolean closeToDeath() {
    	
    	return this.closeToDeath;	
    }
    
    public void comparePositions(ArrayList<Vector2d> unvisitedPositions) {
    	

    	for(int i = 0; i < unvisitedPositions.size(); i++) {
    		if(!positionsToVisit.contains(unvisitedPositions.get(i)) && !visitedPositions.contains(unvisitedPositions.get(i))) {
    			positionsToVisit.add(unvisitedPositions.get(i));
    		}
    	}
    	
    }
    
   public void removeVisitedPositions() {
    	
    	for (int i = 0; i < positionsToVisit.size(); i++) {
    		if(visitedPositions.contains(positionsToVisit.get(i))) {
    			positionsToVisit.remove(i);
    		}
			
		}
    }
    
    private boolean hasPositionBeenVisited(Vector2d position) {
    	
    	if(visitedPositions.contains(position)) {
    		return true;
    	}
    	
    	return false;
    	
    }
    
    private Vector2d selectClosestUnvisitedPosition(StateObservation stateObs) {
    	
    	for(int i = 0; i < positionsToVisit.size() ; i++) {
    		if(!visitedPositions.contains(positionsToVisit.get(i))) {
    			return positionsToVisit.get(i);
    		}
    	}
		return startingPosition;
    	 
    }

}
