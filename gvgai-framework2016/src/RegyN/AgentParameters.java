package RegyN;

enum BestActionFinder{
    BestScore,
    MostVisits,
    BestAverage
}

public class AgentParameters{
    int maxDepth = 10;
    boolean useHistoryOnExit = false;
    boolean useHistoryInValuation = true;
    boolean useGameKnowledge = true;
    boolean rollSimulationCleverly = true;
    boolean expandIntelligently = true;
    BestActionFinder finder = BestActionFinder.BestAverage;

    private static AgentParameters instance;

    private AgentParameters(){}

    public static AgentParameters GetInstance(){
        return GetInstance(false);
    }

    public static AgentParameters GetInstance(boolean forceNew){
        if(forceNew || instance == null){
            instance = new AgentParameters();
        }
        return instance;
    }
}
