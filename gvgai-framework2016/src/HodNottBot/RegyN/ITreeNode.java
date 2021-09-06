package HodNottBot.RegyN;

import core.game.StateObservation;
import ontology.Types;

public interface ITreeNode {
    boolean IsRoot();

    Types.ACTIONS GetBestScoreAction(boolean useHistory);

    Types.ACTIONS GetBestAverageAction(boolean useHistory);

    Types.ACTIONS GetMostVisitedAction(boolean useHistory);

    int GetBestScoreIndex(boolean useHistory);

    int GetBestScoreIndex();

    int GetBestAverageIndex(boolean useHistory);

    int GetBestAverageIndex();

    int GetMostVisitedIndex();

    int GetMostVisitedIndex(boolean useHistory);

    void Expand(StateObservation obs);

    void ExpandIntelligently(StateObservation obs);
}
