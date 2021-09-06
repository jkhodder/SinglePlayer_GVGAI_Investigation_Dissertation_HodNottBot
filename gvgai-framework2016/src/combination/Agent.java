/**
 * Original code of YOLOBOT
 * Modified by Saskia Friedrich, 01/12/2017
 */
package combination;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import combination.SubAgents.SubAgent;
import combination.SubAgents.SubAgentStatus;
import combination.SubAgents.HandleMCTS.MCTHandler;
import combination.SubAgents.bfs.BFS;
import combination.Util.Heatmap;
import combination.Util.Heuristics.HeuristicList;
import combination.Util.Wissensdatenbank.YoloKnowledge;
import combination.adrienctx.AdrienctxAgent;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class Agent extends AbstractPlayer {

	public final static boolean UPLOAD_VERSION = true;
	public final static boolean DRAW_TARGET_ONLY = false;
	public final static boolean FORCE_PAINT = false;
	public final static boolean VIEW_ADVANCES = false;
	public static final double PAINT_SCALE = 1;

	private final List<SubAgent> subAgents;
	private SubAgent currentSubAgent;
	private long sum;
	public static ElapsedCpuTimer curElapsedTimer;
	
	//CheckVariablen um ersten Schritt-Bug zu umgehen:
	private int avatarXSpawn = -1, avatarYSpawn = -1;
	private StateObservation lastStateObs;
	public static YoloState currentYoloState;

	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
		curElapsedTimer = elapsedTimer;
		YoloState startYoloState = new YoloState(so);
		avatarXSpawn = startYoloState.getAvatarX();
		avatarYSpawn = startYoloState.getAvatarY();
		//YoloKnowledge und sonstiges Wissen hier generieren
    	YoloKnowledge.instance = new YoloKnowledge(startYoloState);
		Heatmap.instance = new Heatmap(startYoloState);
		HeuristicList.instance = new HeuristicList();

		// Liste von SubAgents wird hier stellt
		BFS bfs = new BFS(startYoloState, elapsedTimer);
		AdrienctxAgent adrienctx = new AdrienctxAgent (startYoloState, elapsedTimer);
		
		subAgents = new LinkedList<>();
		subAgents.add(new MCTHandler(startYoloState));
		subAgents.add(bfs);
		subAgents.add(adrienctx);
		
		bfs.preRun(startYoloState, elapsedTimer);
		adrienctx.preRun(startYoloState, elapsedTimer);

	}

	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		curElapsedTimer = elapsedTimer;
		currentYoloState = new YoloState(stateObs);
		YoloKnowledge.instance.learnStochasticEffekts(currentYoloState);
		
		if(currentYoloState.getGameTick() == 1){
			if(ersterSchrittBugAufgetreten(currentYoloState)){
				return currentYoloState.getAvatarLastAction();
			}
		}

		YoloState.currentGameScore = currentYoloState.getGameScore();
		Heatmap.instance.stepOn(currentYoloState);

		//@SASKIA:
		//ausgabe(currentYoloState);
		
		checkIfAndDoAgentChange();

		if (!Agent.UPLOAD_VERSION)
			System.out.println("Chosen Agent: " + currentSubAgent.getClass().getName());

		ACTIONS chosenAction = currentSubAgent.act(currentYoloState,
				elapsedTimer);

		if (chosenAction == ACTIONS.ACTION_NIL
				&& currentSubAgent.Status == SubAgentStatus.POSTPONED) {
			// Old agent give up!
			if (elapsedTimer.remainingTimeMillis() > 10) {
				// If we have time for another agent run, do so:
				checkIfAndDoAgentChange();
				chosenAction = currentSubAgent.act(currentYoloState,
						elapsedTimer);
			}
		}

		YoloState.advanceCounter = 0;
		
		return chosenAction;
	}

	private void checkIfAndDoAgentChange() {
		// Pruefe, ob ein neuer SubAgent ausgesucht werden muss
		if (currentSubAgent == null
				|| currentSubAgent.Status != SubAgentStatus.IN_PROGRESS) {
			currentSubAgent = ChooseNewIdleSubAgent(currentYoloState);

			// Falls kein Agent bereit ist, setze erneut alle auf Status "IDLE"
			// und suche erneut nach einem neuen Agent
			if (currentSubAgent == null) {
				for (SubAgent subAgent : subAgents) {
					subAgent.Status = SubAgentStatus.IDLE;
				}
				currentSubAgent = ChooseNewIdleSubAgent(currentYoloState);
			}
			
			currentSubAgent.Status = SubAgentStatus.IN_PROGRESS;
		}
	}

	private boolean ersterSchrittBugAufgetreten(YoloState yoloState) {
		boolean error = true;
		error &= yoloState.getGameTick() == 1;
		error &= yoloState.getAvatarOrientation().equals(
				YoloKnowledge.ORIENTATION_NULL);
		error &= yoloState.getAvatarX() == avatarXSpawn;
		error &= yoloState.getAvatarY() == avatarYSpawn;
		return error;

	}

	/**
	 * Finde einen neuen Agent mit Status "IDLE" und maximalem Gewicht
	 */
	private SubAgent ChooseNewIdleSubAgent(YoloState yoloState) {
		SubAgent newAgent = null;

		double maxWeight = -Double.MAX_VALUE;
		for (SubAgent subAgent : subAgents) {
			if (subAgent.Status == SubAgentStatus.IDLE) {
				double subAgentWeight = subAgent.EvaluateWeight(yoloState);

				if (maxWeight < subAgentWeight) {
					maxWeight = subAgentWeight;
					newAgent = subAgent;
				}
			}
		}

		return newAgent;
	}
	
	@Override
	public void draw(Graphics2D g) {
		if(Agent.UPLOAD_VERSION && !FORCE_PAINT)
			return;
	}
	
	
	//@saskia: Ausgabe zur Spielanalyse
	public void ausgabe(YoloState yoloState){
		Vector2d avatarPosition = yoloState.getAvatarPosition();
		ArrayList<Observation>[] npcPositions = yoloState.getNpcPositions(avatarPosition);
		ArrayList<Observation>[] immovablePositions = yoloState.getImmovablePositions(avatarPosition);
		ArrayList<Observation>[] movablePositions = yoloState.getMovablePositions(avatarPosition);
		ArrayList<Observation>[] resourcesPositions = yoloState.getResourcesPositions(avatarPosition);
		ArrayList<Observation>[] portalPositions = yoloState.getPortalsPositions(avatarPosition);
		ArrayList<Observation>[] avatarSpritesPositions = yoloState.getFromAvatarSpritesPositions(avatarPosition);
				
		System.out.println("");
		System.out.println("Actions: " + yoloState.getAvailableActions());
				
		int npcCounter = 0;
		if (npcPositions != null) {
		    for (ArrayList<Observation> npcs : npcPositions) {
		        npcCounter += npcs.size();
		        System.out.println("npc iTyp: " + npcs.get(0).itype + ", Anzahl: " + npcs.size());
		    }
		    System.out.println("  npcs: " + npcCounter);
		} else {
			System.out.println("npcs: -");
		}
				
		int immovableCounter = 0;
		if (immovablePositions != null) {
		    for (ArrayList<Observation> immovable : immovablePositions) {
		    	immovableCounter += immovable.size();
		    	System.out.println("immovable Typ: " + immovable.get(0).itype + ", Anzahl: " + immovable.size());
		    }
		    System.out.println("  immovables: " + immovableCounter);
		} else {
			System.out.println("immovables: -");
		}
		
		int movableCounter = 0;
		if (movablePositions != null) {
		    for (ArrayList<Observation> movable : movablePositions) {
		        movableCounter += movable.size();
		        System.out.println("movable Typ: " + movable.get(0).itype + ", Anzahl: " + movable.size());
		    }
		    System.out.println("  movables: " + movableCounter);
		} else {
			System.out.println("movables: -");
		}
			
		int resourcesCounter = 0;
		if (resourcesPositions != null) {
		    for (ArrayList<Observation> resources : resourcesPositions) {
		    	resourcesCounter += resources.size();
		    	System.out.println("resources Typ: " + resources.get(0).itype + ", Anzahl: " + resources.size());
		    }
		    System.out.println("  resources: " + resourcesCounter);
		} else {
			System.out.println("resources: -");
				}
				
		int portalCounter = 0;
		if (portalPositions != null) {
		    for (ArrayList<Observation> portals : portalPositions) {
		    	portalCounter += portals.size();
		    	System.out.println("portals Typ: " + portals.get(0).itype + ", Anzahl: " + portals.size());
		    }
		    System.out.println("  portals: " + portalCounter);
		} else {
			System.out.println("portals: -");
		}
		
		int avatarSpritesCounter = 0;
		if (avatarSpritesPositions != null) {
		    for (ArrayList<Observation> avatarSprites : avatarSpritesPositions) {
		    	avatarSpritesCounter += avatarSprites.size();
		    	System.out.println("sprites Typ: " + avatarSprites.get(0).itype + ", Anzahl: " + avatarSprites.size());
		    }
		    System.out.println("  avatarSprites: " + avatarSpritesCounter);
		} else {
			System.out.println("avatarSprites: -");
		}
	}

}
