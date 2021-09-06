package asd592;

import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Adrian on 26.05.2017.
 */
public class Map {

    private int[][] map;
    private int[][] stayDurationMap;
    private int height;
    private int width;
    private int realHeight;
    private int realWidth;
    private int blockSize;
    private Vector2d mainObjective;
    private Vector2d avatarPosition;
    private ArrayList<Vector2d> visitedObjectives;
    private ArrayList<Vector2d> visitedImmovables;
    private ArrayList<Vector2d> visitedPortals;
    private boolean objectiveFound;
    private ArrayList<Observation>[] immovablesPositions;
    private ArrayList<Observation>[] portalPositions;
    private ArrayList<Observation>[] npcPositions;
    private ArrayList<Observation>[] resourcesPositions;
    private ArrayList<Observation>[] movablesPositions;
    private StateObservation currentStateObs;


    public Map(StateObservation stateObservation) {
        this.currentStateObs = stateObservation;
        this.height = stateObservation.getWorldDimension().height;
        this.width = stateObservation.getWorldDimension().width;
        this.blockSize = stateObservation.getBlockSize();
        this.realHeight = height / blockSize;
        this.realWidth = width / blockSize;
        this.map = new int[realWidth][realHeight];
        this.stayDurationMap = new int[realWidth][realHeight];

        this.visitedObjectives = new ArrayList<>();
        this.visitedImmovables = new ArrayList<>();
        this.visitedPortals = new ArrayList<>();
        setObjective();
 //       setExtraPoints();
        resetStayDurationMap();
    }

    public void update(StateObservation stateObservation) {
        this.avatarPosition = stateObservation.getAvatarPosition();
        this.currentStateObs = stateObservation;
        updateObservations(stateObservation);
        setObjective();
        checkPostion();
//        setExtraPoints();
        stayDurationPenalty();


    }

    public void updateObservations(StateObservation stateObservation) {
        this.immovablesPositions = stateObservation.getImmovablePositions();
        this.portalPositions = stateObservation.getPortalsPositions();
        this.npcPositions = stateObservation.getNPCPositions();
        this.resourcesPositions = stateObservation.getResourcesPositions();
        this.movablesPositions = stateObservation.getMovablePositions();

    }

    //checks if the avatar is at the main objective

    public void checkForMainObjective() {
        if (mainObjective != null) {
            if (mainObjective.equals(avatarPosition)) {
                this.visitedObjectives.add(mainObjective);
                resetStayDurationMap();
                setObjective();

            }

        }
    }

    public void setExtraPoints() {
        setValue(mainObjective, 50, true);
        if (immovablesPositions != null) {
            for (ArrayList<Observation> immovables : immovablesPositions) {
                if (immovables.size() < 20) {
                    for (Observation immovable : immovables) {
                        if (!visitedImmovables.contains(immovable.position)) {
                            setValue(immovable.position, 5, false);
                        }
                    }
                }
            }
            if (resourcesPositions != null) {
                for (ArrayList<Observation> resources : resourcesPositions) {
                    for (Observation resource :resources){
                        setValue(resource.position,5,false);
                    }
                }
            }


        }
    }

    public void checkPostion() {
        checkForMainObjective();
        checkForOtherObjectives();

    }

    public void checkForOtherObjectives() {
        if (immovablesPositions != null) {
            for (int i = 1; i < immovablesPositions.length; i++) {
                for (Observation immovable : immovablesPositions[i]) {
                    if (immovable.position.equals(avatarPosition)) {
                        visitedImmovables.add(immovable.position);

                    }
                }
            }
        }
        if (portalPositions != null) {
            for (ArrayList<Observation> portals : portalPositions) {
                for (Observation portal : portals) {
                    if (portal.position.equals(avatarPosition)) {
                        visitedPortals.add(portal.position);

                    }
                }
            }
        }


    }

    //set the value of the fields the map with their distance to the Main Objective

    public void setDistance() {
        for (int i = 0; i < realWidth; i++) {
            for (int j = 0; j < realHeight; j++) {
                Vector2d vector2d = new Vector2d(i * blockSize, j * blockSize);
                map[i][j] = 20 - (int) vector2d.dist(mainObjective) / blockSize;
            }
        }
    }

    public int
    getScore(Vector2d position) {
        int x = (int) position.x / blockSize;
        int y = (int) position.y / blockSize;
        int score = 0;
        if (x >= 0 && x < realWidth && y >= 0 && y < realHeight) {
            score = map[x][y] - stayDurationMap[x][y];
        }
        return score;
    }

    //search for the main Objective
    //resources and immovables are preferred over portals and npcs

    public void setObjective() {
        this.objectiveFound = false;
        if (resourcesPositions != null) {
            ArrayList<Observation>[] sortedRessources = sortOberservations(resourcesPositions);
            searchObjective(sortedRessources);
        }
        if (immovablesPositions != null && !objectiveFound) {
            ArrayList<Observation>[] sortedArray = sortOberservations(immovablesPositions);
            searchObjective(sortedArray);

        }
        if (!objectiveFound) {
            if (portalPositions != null) {
                ArrayList<Observation>[] sortedPortals = sortOberservations(portalPositions);
                searchObjective(sortedPortals);

            }

        }
        if (!objectiveFound) {
            if (npcPositions != null) {
                ArrayList<Observation>[] sortedNPCs = sortOberservations(npcPositions);
                searchObjective(sortedNPCs);


            }
        }


    }

    //search the main objective
    //only observations that occur less than 30 times are considered

    public void searchObjective(ArrayList<Observation>[] array) {
        ArrayList<Observation>[] sortedArray = sortOberservations(array);
        for (ArrayList<Observation> observations : sortedArray) {
            if (objectiveFound) {
                break;
            } else {
                if (observations.size() < 30) {
                    for (Observation observation : observations) {
                        //                       System.out.println("Searching for new Objectives");
                        if (!visitedObjectives.contains(observation.position) && !visitedImmovables.contains(observation.position)) {
                            objectiveFound = true;
                            mainObjective = observation.position;
                            setDistance();
                            break;

                        }
                    }
                }
            }
        }
    }

    //Penalise the avatar for staying in the same position

    public void stayDurationPenalty() {
        //      setValue(avatarPosition,-1,false);
        int x = (int) avatarPosition.x / blockSize;
        int y = (int) avatarPosition.y / blockSize;

        if (x >= 0 && x < realWidth && y >= 0 && y < realHeight) {
            stayDurationMap[x][y] += 1;
        }


        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (i >= 0 && i < realWidth && j >= 0 && j < realHeight) {
                    stayDurationMap[i][j] += 1;
                }
            }
        }/*
        for (int i = x-1; i <= x+1; i++){
            for (int j = y-1; j <= y+1; j++){
                stayDurationMap[x][y] -= 0.5;
            }
        }*/

    }

    public void resetStayDurationMap() {
        for (int i = 0; i < realWidth; i++) {
            for (int j = 0; j < realHeight; j++) {
                stayDurationMap[i][j] = 0;
            }
        }
    }

    public ArrayList<Observation>[] sortOberservations(ArrayList<Observation>[] observations) {
        ArrayList<Observation>[] sortedObservations = observations.clone();
        Arrays.sort(sortedObservations, Comparator.comparing(ArrayList<Observation>::size));
        return sortedObservations;

    }


/*    public Vector2d vectorConvert(Vector2d vector2d) {
        double x = vector2d.x / blockSize;
        double y = vector2d.y / blockSize;
        return new Vector2d(x, y);
    }*/

    public void setValue(Vector2d vector2d, int value, boolean newValue) {
        int x = (int) vector2d.x / blockSize;
        int y = (int) vector2d.y / blockSize;
        if (x < realWidth && x >= 0 && y < realHeight && y >= 0) {
            if (newValue) {
                map[x][y] = value;
            } else {
                map[x][y] += value;
            }
        }

    }


}
