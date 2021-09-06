package asd592;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

/**
 * Created by albke on 24.06.2017.
 */
public abstract class Controller {

    public abstract Types.ACTIONS getAction(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer);
}
