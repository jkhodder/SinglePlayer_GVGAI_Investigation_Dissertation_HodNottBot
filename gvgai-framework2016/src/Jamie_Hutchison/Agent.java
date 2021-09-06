package Jamie_Hutchison;

import java.util.ArrayList;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

public class Agent extends AbstractPlayer {

    public int NUM_ACTIONS;
    public Types.ACTIONS[] actions;
    
    private DecisionMaker dm;
    
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {

        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for(int i = 0; i < actions.length; ++i)
        {
            actions[i] = act.get(i);
        }
        NUM_ACTIONS = actions.length;
        
    	dm = new DecisionMaker(NUM_ACTIONS, actions, so, this);

    }

    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
    	
    	dm.addVisitedPosition(stateObs.getAvatarPosition());
    	dm.update(stateObs, elapsedTimer);
    	
    	int action = dm.selectAction();
    	
    	return actions[action];
    }


}
