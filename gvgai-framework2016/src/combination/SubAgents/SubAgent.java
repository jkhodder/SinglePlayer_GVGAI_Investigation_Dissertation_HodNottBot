/**
 * Original code of YOLOBOT
 * Modified by Saskia Friedrich, 01/12/2017
 */
package combination.SubAgents;

import ontology.Types;
import tools.ElapsedCpuTimer;
import combination.YoloState;

public abstract class SubAgent {
	public SubAgentStatus Status;
	
	public SubAgent() {
		Status = SubAgentStatus.IDLE;
	}
	
	public abstract Types.ACTIONS act(YoloState yoloState, ElapsedCpuTimer elapsedTimer);
	
	public abstract double EvaluateWeight(YoloState yoloState);

	public abstract void preRun(YoloState yoloState, ElapsedCpuTimer elapsedTimer);
	
}
