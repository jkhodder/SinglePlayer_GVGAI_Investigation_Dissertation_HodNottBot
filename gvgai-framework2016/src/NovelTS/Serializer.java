package NovelTS;
import tools.Vector2d;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import core.game.Observation;
import core.game.StateObservation;


/**
* The purpose of the serializer is to give a temperary aim for the Agent
* to achieve, this aim is depended on the current state and
* the previous knowleged based on the aim.
*
* The serializer will keep on getting feedback from the searching state
* and tries to estimate the best goal to reach
*
* Because of the time limitation, we will assume events could only
* be related if they next to each other.
*/
public class Serializer {
    private HashMap<Integer, Integer> totalReward;
    private HashMap<Integer, Integer> count;
    private int numVisit;
    private int maxDistance;
    private ArrayList<Integer> ids;
    private int safeZone;
    // EPSILON to normalize score
    private static final double EPSILON = 0.0001;

    public Serializer(StateObservation state) {
        totalReward = new HashMap<Integer, Integer>();
        count = new HashMap<Integer, Integer>();
        ids = new ArrayList<Integer>();
        ArrayList<Observation>[][] obs =  state.getObservationGrid();
        maxDistance = obs.length + obs[0].length;
        safeZone = 4;
    }


    /**
    * Will check whether the object close by is interesting or not
    * interesting object are objects that cause reward
    *
    * it takes in a fully expaned node
    */
    public void updateHeuristic(Feature feature, double reward) {
        this.numVisit += 1;
        int r = normalizeReward(reward);
        for (Atom atom: feature.getCloseObject()) {
            int obj = atom.getData3();
            if (!totalReward.containsKey(obj)) {
                ids.add(obj);
                count.put(obj, 0);
                totalReward.put(obj, r);
            } else {
                int prevReward = totalReward.get(obj);
                int prevVisit = count.get(obj);
                totalReward.put(obj, prevReward + r);
                count.put(obj, prevVisit + 1);
            }
        }
    }

    public double getScore(Feature feature) {
        int bestID = bestRewarding();
        double h = 1000;
        Atom player = feature.getAvatarFeatures();
        for (Atom atom: feature.getFeatures()) {
            int obj = atom.getData3();
            if (!totalReward.containsKey(obj)){
                totalReward.put(obj, 0);
                count.put(obj, 1);
                ids.add(obj);
            }
            if (obj == bestID) {
                int d = Utils.compareDistance(atom, player)
                        / IWPlayer.blocksize;
                if (d < h) {
                    h = d;
                }
            }
        }
        if (h == 1000) {
            count.put(bestID, 1);
            totalReward.put(bestID, 0);
            return getScore(feature);
        }
        System.out.println(h);
        return (maxDistance - h) * EPSILON;
    }

    /**
    * This is a simple totalReward that calculate the minimum moving dis(tance
    * to an interesting object.
    */
    // public double getScore(Feature feature) {
    //     System.out.println("total Reward: " + totalReward);
    //     System.out.println("Count: " + count);
    //     int closeReward = maxDistance;
    //     int closeDanger = maxDistance;
    //     Atom player = feature.getAvatarFeatures();
    //     for (Atom atom: feature.getFeatures()) {
    //         int obj = atom.getData3();
    //         if (!totalReward.containsKey(obj)){
    //             totalReward.put(obj, 1);
    //             count.put(obj, 0);
    //             ids.add(obj);
    //         }
    //
    //         int numVisit = count.get(obj);
    //         int h;
    //         if (numVisit > 0) {
    //             h = totalReward.get(obj) / count.get(obj);
    //             h = normalizeReward(h);
    //         } else {
    //             h = 1;
    //         }
    //
    //         int d = Utils.compareDistance(atom, player)
    //                 / IWPlayer.blocksize;
    //
    //         if (h >= 1) {
    //             if (closeReward > d && d != 0) {
    //                 closeReward = d;
    //             }
    //         }
    //         else if (h < 0) {
    //             if (closeDanger > d && d != 0) {
    //                 closeDanger = d;
    //             }
    //         }
    //     }
    //     if (closeDanger < safeZone) {
    //         return (-closeDanger) * EPSILON;
    //     }
    //     return (maxDistance - closeReward) * EPSILON;
    // }

    private int normalizeReward(double reward) {
        if (Math.abs(reward ) < 0.1) {
            return 0;
        }
        if (reward > 0) {
            return 1;
        }
        return -1;
    }

    public void reset() {
        for (Integer id: ids) {
            if (totalReward.get(id) == 0) {
                totalReward.put(id, 1);
            }
        }
    }

    public int bestRewarding() {
        double max = -1;
        double h = -1;
        int bestID = -1;
        for (int id: ids) {
            h = totalReward.get(id) / (double) count.get(id);

            if (h > max) {
                max = h;
                bestID = id;
            }
        }
        return bestID;
    }

    @Override
    public String toString() {
        String s = "";
        for (Integer id: ids) {
            s += "(" + id +", " + totalReward.get(id) + ")";
        }
        return s + " " + numVisit;
    }
}
