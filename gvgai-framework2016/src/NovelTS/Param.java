package NovelTS;

public class Param {
    public enum Search {
        BFS, RANDOM_WALK, OPEN_LOOP_BFS, OPEN_LOOP_RANDOM_WALK
    }
    public enum Feature {
        DISTANCE, ITYPE, CATEGORY
    }

    public static final Feature featureSelection = Feature.ITYPE;
    public static final Search search = Search.OPEN_LOOP_BFS;
    public static final Boolean iwPrunning = true;
    public static final Boolean dangerPrunning = true;
    public static final Boolean distanceHeuristic = true;
    public static final Boolean goalSerializer = false;
    public static final double exploreExploitRate = 0.5;
    public static final Boolean deathPrunning = true;
    public static final Boolean randomAction = true;
    public static final Boolean rewardNovelty = false;
    public static final Integer closeDistance = 2;
    public static final Boolean debug = false;
    public static final Boolean noveltyPreference = true;
}
