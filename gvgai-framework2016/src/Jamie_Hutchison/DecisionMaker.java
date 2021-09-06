package Jamie_Hutchison;

import java.util.Random;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class DecisionMaker {
	
	 public int num_actions;
	 public Types.ACTIONS[] actions;
	 boolean allVisited;
	 private int MCTSaction;
	 private int explorerAction;
	 
	 private Explorer ex;
	 private SingleMCTSPlayer mctsPlayer;
	 
	 
	 public StateObservation so;
	 
	public DecisionMaker(int num_actions, Types.ACTIONS[] actions, StateObservation stateObs, Agent a){
		
        initialise(num_actions, actions, stateObs, a);
        
	}

    
    public void initialise(int num_actions, Types.ACTIONS[] actions, StateObservation stateObs, Agent a) {
    	
    	mctsPlayer = new SingleMCTSPlayer(new Random(), a);
    	ex = new Explorer(num_actions, actions, stateObs);

    }
    
    public void update(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
    	
    	mctsPlayer.init(stateObs);
    	
    	MCTSaction = mctsPlayer.run(elapsedTimer);
    	
    	ex.comparePositions(mctsPlayer.getUnvisitedPositions());
    	ex.removeVisitedPositions();
        explorerAction = ex.chooseAction(stateObs, elapsedTimer);
        allVisited = ex.allVisited;
    }
    
    public int selectAction() {
    	
    	if(ex.closeToDeath()) {
    //		System.out.println("MCTS death");
     		return MCTSaction;
    	}
    	
    	if(ex.highPrioriy()) {
  //  		System.out.println("high priority explorer");
    		return explorerAction;
    	}
    	
    	if(allVisited ) {
  //  		System.out.println("MCTS");
     		return MCTSaction;
    	}
    	
 //   	System.out.println("normal explorer   ");
    	return explorerAction;
    		
    }
    
	public void addVisitedPosition(Vector2d position) {
		if(!ex.visitedPositions.contains(position)) {
		ex.visitedPositions.add(position);
		}
	}
	
}
