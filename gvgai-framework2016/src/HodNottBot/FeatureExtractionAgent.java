package HodNottBot;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class FeatureExtractionAgent extends AbstractPlayer {
    public static String gameName;
    public static int gameLevel;

    public FeatureExtractionAgent(StateObservation so, ElapsedCpuTimer elapsedTimer)  throws IOException {
        ArrayList<Observation>[] resourcePositions = so.getResourcesPositions();

        int hasResources = (resourcePositions != null) ? 1 : 0;
        int resourceTypes;
        if(resourcePositions!= null) {
            resourceTypes = resourcePositions.length;
        } else{
            resourceTypes = 0;
        }

        HashMap<Integer, Integer> avatarResources = so.getAvatarResources(); // hashmap with (itemID: amount) key pairs
        int avatarHasResources = (!avatarResources.isEmpty()) ? 1 : 0;
        int numAvatarResources = avatarResources.keySet().toArray().length;

        ArrayList<Observation>[] NPCPositions = so.getNPCPositions();
        int NPCTypes = 0;
        int numNPCs = 0;
        if (NPCPositions != null) {
            for (ArrayList<Observation> NPCType : NPCPositions) {
                NPCTypes += 1;
                numNPCs += NPCType.size();
            }
        }

        int area = (int) Math.round(so.getWorldDimension().getWidth() * so.getWorldDimension().getHeight());
        int blockSize = so.getBlockSize();

        ArrayList<Observation>[] immovablePositions = so.getImmovablePositions();
        int numImmovable = 0;
        if (immovablePositions != null) {
            for (ArrayList<Observation> immoveableThings : immovablePositions) {
                numImmovable += immoveableThings.size();
            }
        }

        ArrayList<Observation>[] movablePositions = so.getMovablePositions();
        int numMovable = 0;
        if (movablePositions != null) {
            for (ArrayList<Observation> moveableType : movablePositions) {
                numMovable += moveableType.size();
            }
        }

        ArrayList<Observation>[] portalPositions = so.getPortalsPositions();
        int hasPortals = (portalPositions != null) ? 1 : 0;
        int portalTypes;
        if(hasPortals == 1) {
            portalTypes = portalPositions.length;
        } else{
            portalTypes = 0;
        }

        int verticality = ((so.getAvailableActions().contains(Types.ACTIONS.ACTION_UP)) || (so.getAvailableActions().contains(Types.ACTIONS.ACTION_DOWN))) ? 1 : 0;
        int useAvailable = (so.getAvailableActions().contains(Types.ACTIONS.ACTION_USE)) ? 1 : 0;
        int healthAvailable = (so.getAvatarHealthPoints() > 0) ? 1 : 0;


//        System.out.println(hasResources);
//        System.out.println(avatarHasResources);
//        System.out.println(resourceTypes);
//        System.out.println(numAvatarResources);
//        System.out.println(numNPCs);
//        System.out.println(NPCTypes);
//        System.out.println(area);
//        System.out.println(blockSize);
//        System.out.println(numImmovable);
//        System.out.println(numMovable);
//        System.out.println(hasPortals);
//        System.out.println(portalTypes);
//        System.out.println(verticality);
//        System.out.println(useAvailable);
//        System.out.println(healthAvailable);

        // As this code appends to the file, make sure the file is empty or doesn't exist before running if you only want one set of results.
        File f = new File("C:\\Users\\James\\OneDrive - The University of Nottingham\\Dissertation\\FeatureExtraction.csv");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f, true);
        PrintWriter pw = new PrintWriter(fos);

        String line = gameName+","+gameLevel+","+hasResources+","+avatarHasResources+","+resourceTypes+","+numAvatarResources
                +","+numNPCs+","+NPCTypes+","+area+","+blockSize+","+numImmovable+","+numMovable+","+hasPortals+","+
                portalTypes+","+verticality+","+useAvailable+","+healthAvailable;
        pw.println(line);
        pw.close();


        fos.close();
    }

    @Override
    public Types.ACTIONS act(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        return Types.ACTIONS.ACTION_NIL;
    }
}
