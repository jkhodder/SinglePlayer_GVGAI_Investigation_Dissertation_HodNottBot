/**
 * Original code of YOLOBOT
 * Modified by Saskia Friedrich, 01/12/2017
 */
package combination.SubAgents.bfs;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

import ontology.Types;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;
import tools.ElapsedCpuTimer;
import combination.Agent;
import combination.YoloState;
import combination.SubAgents.SubAgent;
import combination.SubAgents.SubAgentStatus;
import combination.Util.Planner.KnowledgeBasedAStar;
import combination.Util.Wissensdatenbank.YoloKnowledge;
import core.game.Observation;

public class BFS extends SubAgent {
	private static final long MAX_MEMORY = 1600000000;

	private final boolean FORCE_RUN = false;
	
	private boolean sawNPC;
	
    /**
     * Random generator for the agent.
     */
    protected Random randomGenerator;

    /**
     * Observation grid.
     */
    protected ArrayList<Observation> grid[][];

    /**
     * block size
     */
    protected int block_size;

    private HashSet<Long> visited;
    
    private PriorityQueue<OwnHistoryLight> queue;
    
    private OwnHistoryLight targetSolution;
    private OwnHistoryLight winSolution;
    
    private int targetSolutionStep;
    private double currentBranchingFactor;
    private int expandedCount;
    private int branchedCount;
    private int lastDepthStep;
    private int lastDepth;
    private boolean fastForward;
    private int expandSteps, cancelMoves;
    private OwnHistoryLight bestScore;
    private long lastRealStateHash;
    
    private boolean foundSolutionInPrerun;

	private YoloState currentState_ForDrawing;
	private YoloState deepestState, drawState;
	
	private boolean firstSecond = true;
	public int tick;
	public int maybeEndOfTick = 2000;
	
    
    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public BFS(YoloState so, ElapsedCpuTimer elapsedTimer){
    	deepestState = so;
    	foundSolutionInPrerun = false;
    	lastRealStateHash = so.getBfsCheckHash(false);
    	expandedCount = 0;
    	branchedCount = 0;
    	lastDepthStep = 0;
    	expandSteps = 0;
    	cancelMoves = 0;
    	lastDepth = 0;
    	targetSolutionStep = 0;
    	targetSolution = null;
        randomGenerator = new Random();
        grid = so.getObservationGrid();
        block_size = so.getBlockSize();
        visited = new HashSet<Long>();

        Comparator<OwnHistoryLight> c = new Comparator<OwnHistoryLight>() {
        	public int compare(OwnHistoryLight o1, OwnHistoryLight o2) {return (int) Math.signum(o1.getPriority() - o2.getPriority());}
		};
		queue = new PriorityQueue<OwnHistoryLight>(2000,c);
		so.advance(ACTIONS.ACTION_NIL);
        OwnHistoryLight startState = new OwnHistoryLight(so);
        queue.add(startState);
        bestScore = startState;
        sawNPC = so.getNpcPositions() != null && so.getNpcPositions().length > 0;
    }
    
    
    @Override
    public void preRun(YoloState yoloState, ElapsedCpuTimer elapsedTimer) {
        doBreitensuche(elapsedTimer);
        foundSolutionInPrerun = targetSolution != null;
        firstSecond = false;
        if(winSolution != null && !YoloKnowledge.instance.canIncreaseScoreWithoutWinning(winSolution.state)){ 
        	targetSolution = winSolution;
        }	
    }


    private void doBreitensuche(ElapsedCpuTimer elapsedTimer) {
        OwnHistoryLight h, h2;
        
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int remainingLimit = 5;
        int numIters = 0;
        
        while (!queue.isEmpty()) {
        	ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
        	//Ist zeit uebrig?
        	if(!(remaining > 2*avgTimeTaken && remaining > remainingLimit))
        		break;
        	//Queue
        	h = queue.poll();
        	expandedCount++;
        	
        	if(h.tick>deepestState.getGameTick() && YoloKnowledge.instance.playerItypeIsWellKnown(h.state) && YoloKnowledge.instance.agentHasControlOfMovement(h.state))
        		deepestState = h.state;

        	for (ontology.Types.ACTIONS action : h.state.getAvailableActions(true)) {
        		boolean forceExpand = false;
				if(YoloKnowledge.instance.playerItypeIsWellKnown(h.state) && !YoloKnowledge.instance.agentHasControlOfMovement(h.state))
					if(!action.equals(ACTIONS.ACTION_NIL))
						continue;
					else
						forceExpand = true;
        		Long probabilHash = YoloKnowledge.instance.getPropablyHash(h.state, action, true);
        		boolean guessWillCancel = probabilHash != null && visited.contains(probabilHash);
        		if(guessWillCancel && !forceExpand){
        			cancelMoves++;
        			continue;
        		}
        		h2 = new OwnHistoryLight(h, action);
        		
        		if(!sawNPC && h2.state.getNpcPositions() != null && h2.state.getNpcPositions().length>0 ){
        	        sawNPC = true;
        		}
        		
        		long hash = h2.state.getHash(true);
        		
        		if (!forceExpand && visited.contains(hash) && h.tick != 0 && h2.timeSinceAvatarChange != 1){
        			cancelMoves++;
        			continue;
        		}else{
            		expandSteps++;
        		}
        		visited.add(hash);
        		branchedCount++;
        		        		
				if(!h2.state.isGameOver()){
	        		if((h2.score > bestScore.score || !YoloKnowledge.instance.agentHasControlOfMovement(bestScore.state)) && YoloKnowledge.instance.agentHasControlOfMovement(h2.state)){
						bestScore = h2;
					}
					if(!h2.toPrune())
						queue.add(h2);
				}else if(h2.state.getGameWinner() == WINNER.PLAYER_WINS){	
					if (firstSecond || (YoloKnowledge.instance.canIncreaseScoreWithoutWinning(h.state)&&false) && tick+h2.tick < maybeEndOfTick){
						if(winSolution == null || h2.score >= winSolution.score){
							winSolution = h2;
							if(!Agent.UPLOAD_VERSION){
								System.out.println("win Score"+ winSolution.score);
								System.out.println("win tick"+ winSolution.tick);
							}
						}						
					}else{
						if(!Agent.UPLOAD_VERSION){
							System.out.println("Perfect Score"+ h2.score);
							System.out.println("Perfect tick"+ h2.tick);
						}
						targetSolution = h2;
						return;
					}					
				}
				if(fastForward)
					break;
				
			}

            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());

            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
		}

        currentBranchingFactor = (double)branchedCount/(double)expandedCount;
	}



	/**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(YoloState yoloState, ElapsedCpuTimer elapsedTimer) {    	
    	if(!Agent.UPLOAD_VERSION)
    		System.out.println("\t\t\t\t\t\t SIZE: " + queue.size());
    	
    	tick = yoloState.getGameTick();
    	
    	if(maybeEndOfTick == 2000){
	    	deepestState.advance(ACTIONS.ACTION_NIL);
	    	if(deepestState.isGameOver()){
	    		maybeEndOfTick = deepestState.getGameTick()-1;
	    	}else{
	    		deepestState.advance(ACTIONS.ACTION_NIL);
		    	if(deepestState.isGameOver()){
		    		maybeEndOfTick = deepestState.getGameTick()-1;
		    	}
	    	}
    	}

    	long currentHash = yoloState.getHash(false);
    	if(tick == 0)
    		drawState = yoloState;
    	if(tick > 2 && targetSolution == null && yoloState.getAvatarLastAction() == ACTIONS.ACTION_NIL && lastRealStateHash != currentHash){
    		if(!FORCE_RUN){
    			Status = SubAgentStatus.POSTPONED;
    			if(!Agent.UPLOAD_VERSION)
    				System.out.println("BFS will nich mehr!");
    		}
    	}
    	lastRealStateHash = currentHash;
    	
    	ACTIONS todo = ACTIONS.ACTION_NIL;    	
    	
    	
    	if(winSolution != null && tick+winSolution.tick == maybeEndOfTick  && targetSolution == null){
    		if(!Agent.UPLOAD_VERSION)
    			System.out.println("BFS execute winSolution because end to close at tick " +tick );
    		targetSolution = winSolution;
    		queue.clear();
    		
    	}			
    	
    	if(targetSolution== null && tick+bestScore.tick > maybeEndOfTick*0.75){
    		if(!Agent.UPLOAD_VERSION)
    			System.out.println("BFS execute bestScore because end(" + (maybeEndOfTick*0.75)+ ") detected at tick " +tick );
    		if(winSolution != null)
    			targetSolution = winSolution;
    		else
    			targetSolution = bestScore;
    		queue.clear();
    	}
    	
    	lastDepthStep++;
        
    	if(targetSolution == null){
    		doBreitensuche(elapsedTimer);
    		if(tick%20 == 0)
    			Runtime.getRuntime().gc();
    	}
    	if(targetSolution != null){
    		if(targetSolutionStep == 0){
				if(!Agent.UPLOAD_VERSION){
        			System.out.println("Ausfuehrung gestartet mit " + targetSolution.actions.size() + " Schritten!");
				}
				if(targetSolution.actions.isEmpty()){
					if(!Agent.UPLOAD_VERSION)
	        			System.out.println("BFS abstellen, da nichts weiter gefunden wurde!");
        			Status = SubAgentStatus.POSTPONED;
					
				}else{
		    		queue.clear();
		    		visited.clear();
					Runtime.getRuntime().gc();
				}
    		}
    		targetSolutionStep++;
    		
    		if(targetSolution.actions.isEmpty()){
    			queue.add(new OwnHistoryLight(targetSolution.state));
    			targetSolution = null;
    			targetSolutionStep = 0;
    		}else{
    			if(!surviveInFuture(yoloState, targetSolution)){
        			Status = SubAgentStatus.POSTPONED;
    				todo = ACTIONS.ACTION_NIL;
        		}else
        			todo = targetSolution.actions.removeFirst();
        		
        		if(targetSolution.actions.size() == 2 && mctsCouldSolve(yoloState)){
        			targetSolution = null;
        			targetSolutionStep = 0;
        			Status = SubAgentStatus.POSTPONED;
        		}
    		}
    	}
    	long memoryUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
    	
    	if(!Agent.UPLOAD_VERSION)
    		System.out.println("Memory Remaining: " + (MAX_MEMORY-memoryUsed));
    	//MemoryRemaining-Check:
    	if(targetSolution == null && (memoryUsed > MAX_MEMORY || queue.isEmpty())){
    		if(winSolution != null && !mctsCouldSolve(bestScore.state)){
    			targetSolution = winSolution;	
    		}else {
    			targetSolution = bestScore;
    		}
    		bestScore = targetSolution;
    		winSolution = null;	
    	}
    	if(Status == SubAgentStatus.POSTPONED){
    		queue.clear();
    	}
		return todo;
    }


    private boolean surviveInFuture(YoloState yoloState, OwnHistoryLight executeSolution) {
    	int advanceChecks = Math.min(2, executeSolution.actions.size());
    	YoloState checkState = yoloState.copy();
    	for (int i = 0; i < advanceChecks; i++) {
			ACTIONS action = executeSolution.actions.get(i);
			checkState.advance(action);
			if(checkState.isGameOver() && checkState.getGameWinner() != WINNER.PLAYER_WINS)
				return false;
		}
    	return true;
	}
    
    
	private boolean mctsCouldSolve(YoloState state) {
		return false;
	}
    
	
	@Override
	public double EvaluateWeight(YoloState yoloState) {
		if(FORCE_RUN || targetSolution != null || winSolution != null)
			return 10000;	
		if(!sawNPC)
			return 11;
		return -11;
	}
}
