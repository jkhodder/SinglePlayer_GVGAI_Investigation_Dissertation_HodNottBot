package NovelTS;

import core.game.StateObservation;
import core.game.Observation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import tools.Vector2d;
public class NoveltyTable{

    private HashSet<Atom> hashSet;
    private HashMap<Atom, Integer> hashMap;

    public NoveltyTable() {
        hashSet = new HashSet<Atom>();
        hashMap = new HashMap<Atom, Integer>();

    }

    /**
    * Will return 0 if no atom is true
    */
    public double getNovelty(Feature features) {
        int novelty =0;
        for (Atom atom :features.getFeatures()) {
            if (!hashSet.contains(atom)) {
                novelty += 1;
                hashSet.add(atom);
            }

        }

        Atom player = features.getAvatarFeatures();
        if (!hashSet.contains(player)) {
            novelty += 1;
            hashSet.add(player);
        }

        return novelty;
    }


    /**
    * Will return 0 if no atom is true
    */
    public double getNovelty(Feature features, int score) {
        int novelty =0;
        for (Atom atom :features.getFeatures()) {
            if (!hashMap.containsKey(atom)) {
                novelty += 1;
                hashMap.put(atom, score);
            } else {
                int s = hashMap.get(atom);
                if (s < score) {
                    novelty += 1;
                    hashMap.put(atom, score);
                }
            }

        }

        Atom atom = features.getAvatarFeatures();
            if (!hashMap.containsKey(atom)) {
                novelty += 1;
                hashMap.put(atom, score);
            } else {
                int s = hashMap.get(atom);
                if (s < score) {
                    novelty += 1;
                    hashMap.put(atom, score);
                }
            }

        return novelty;
    }
}
