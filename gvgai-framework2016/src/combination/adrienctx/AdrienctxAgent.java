/**
 * Code written by Adrien Couetoux, acouetoux@ulg.ac.be It is free to use,
 * distribute, and modify. User: adrienctx Date: 12/01/2015
 * Modified by Saskia Friedrich, 01/12/2017
 */
package combination.adrienctx;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;

import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.Random;

import combination.YoloState;
import combination.SubAgents.SubAgent;
import combination.SubAgents.SubAgentStatus;


public class AdrienctxAgent extends SubAgent {

    public static int NUM_ACTIONS;
    public static Types.ACTIONS[] actions;

    private static boolean isAdrienctxGame;
    
    
    /**
     * Random generator for the agent.
     */
    private final TreeSearchPlayer treeSearchPlayer;

    /**
     * Public constructor with state observation and time due.
     *
     * @param so           state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public AdrienctxAgent(YoloState ys, ElapsedCpuTimer elapsedTimer) {
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = ys.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for (int i = 0; i < actions.length; ++i) {
            actions[i] = act.get(i);
        }
        NUM_ACTIONS = actions.length;
        isAdrienctxGame = false;

        //Create the player.
        treeSearchPlayer = new TreeSearchPlayer(new Random());
    }

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     *
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    @Override
	public ACTIONS act(YoloState yoloState, ElapsedCpuTimer elapsedTimer) {
		
        //Determine the action using MCTS...
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;
        int remainingLimit = 5;

        treeSearchPlayer.init(yoloState);

        while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            treeSearchPlayer.iterate();
            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
            avgTimeTaken = acumTimeTaken / numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }

        return actions[treeSearchPlayer.returnBestAction()];
    }

	

    /*
	 * @author: saskia
	 */
	@Override
	public double EvaluateWeight(YoloState yoloState) {
		if (isAdrienctxGame){
			return 100;
		}
		return -100;
	}

	
	/* 
	 * @author: saskia
	 */
	@Override
	public void preRun(YoloState yoloState, ElapsedCpuTimer elapsedTimer) {
		if (yoloState.getNpcPositions() != null && yoloState.getNpcPositions().length != 0) {
			isAdrienctxGame = true;
			if (yoloState.getImmovablePositions() == null || yoloState.getImmovablePositions().length == 0){
				isAdrienctxGame = false;
				return;
			}
			
			if (!yoloState.getAvailableActions().contains(ACTIONS.ACTION_DOWN) && !yoloState.getAvailableActions().contains(ACTIONS.ACTION_UP)){
				isAdrienctxGame = false;
				return;
			}
			
			if (yoloState.getAvailableActions().contains(ACTIONS.ACTION_USE)){
				// check if sprite type is id 4 after use
				YoloState folgezustand = yoloState.copy();
				folgezustand.advance(ACTIONS.ACTION_USE);
				ArrayList<Observation>[] avatarSpritesPositions = folgezustand.getFromAvatarSpritesPositions();
				if (avatarSpritesPositions != null){
					for (ArrayList<Observation> avatarSprites : avatarSpritesPositions) {
				    	if (avatarSprites.get(0).itype == 4){
				    		isAdrienctxGame = false;
							return;
				    	}
				    }
				}
			}
		}
		return;	
	}
		
}

