package RegyN;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

enum GameType{
    Planar2D,
    Other
}

enum SpriteType{
    Wall,
    Floor,
    Player,
    Enemy,
    Point,
    Other,
    Unknown
}

public class GameKnowledge{
    final static int SPRITE_MAX = 50;
    final int NPC_CATEGORY = 3;
    GameType type;
    SpriteType[] sprites;
    int gameWidth;
    int gameHeight;
    double avatarSpeed;
    boolean isAvatarOriented;
    private static GameKnowledge instance;
    
    private GameKnowledge(){
        sprites = new SpriteType[SPRITE_MAX];
        for(int i = 0; i<SPRITE_MAX; i++){
            sprites[i] = SpriteType.Unknown;
        }
    }
    
    public static GameKnowledge getInstance() {
        if(instance == null)
            instance = new GameKnowledge();
        return instance;
    }
    
    public static GameKnowledge GetNew(){
        instance = new GameKnowledge();
        return instance;
    }

    public static void Reset(){
        instance = new GameKnowledge();
    }

    public boolean CheckForWalls(ArrayList<Observation> objects){
        for(Observation o : objects){
            if(this.sprites[o.itype] == SpriteType.Wall){
                return true;
            }
        }
        return false;
    }

    public void GatherStaticInfo(StateObservation stateObs){
        gameHeight = stateObs.getWorldDimension().height / stateObs.getBlockSize();
        gameWidth = stateObs.getWorldDimension().width / stateObs.getBlockSize();
        avatarSpeed = stateObs.getAvatarSpeed();
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        if (actions.contains(Types.ACTIONS.ACTION_UP) && actions.contains(Types.ACTIONS.ACTION_DOWN)
                && actions.contains(Types.ACTIONS.ACTION_LEFT) && actions.contains(Types.ACTIONS.ACTION_RIGHT)) {
            type = GameType.Planar2D;
        } else {
            type = GameType.Other;
        }
        // Statycznie zbieram info o spritach w grze
        sprites[stateObs.getAvatarType()] = SpriteType.Player;
        Position2D avatarPos = Position2D.GetAvatarPosition(stateObs);
        ArrayList<Observation> obs = Position2D.GetObservations(stateObs, avatarPos.x, avatarPos.y);
        for (Observation o : obs) {
            if (o.itype != stateObs.getAvatarType()) {
                sprites[o.itype] = SpriteType.Floor; // Jeśli na początku gry coś jest bezpośrednio pod graczem, to pewnie jest to podłoga.
            }
        }
        isAvatarOriented = checkIsAvatarOriented(stateObs);
    }
    
    private boolean checkIsAvatarOriented(StateObservation stateObs) {
        Vector2d originalOrientation = stateObs.getAvatarOrientation();
        Vector2d originalPosition = stateObs.getAvatarPosition();
        List<Types.ACTIONS> actions = Arrays.asList(Types.ACTIONS.ACTION_LEFT, Types.ACTIONS.ACTION_UP, Types.ACTIONS.ACTION_RIGHT, Types.ACTIONS.ACTION_DOWN);
        for(Types.ACTIONS testAction : actions){
            StateObservation stateCopy = stateObs.copy();
            stateCopy.advance(testAction);
            Vector2d walkDirection;
            switch (testAction) {
                case ACTION_DOWN:
                    walkDirection = Types.DOWN;
                case ACTION_UP:
                    walkDirection = Types.UP;
                case ACTION_RIGHT:
                    walkDirection = Types.RIGHT;
                case ACTION_LEFT:
                    walkDirection = Types.LEFT;
                default:
                    walkDirection = Types.NONE;
            }
            Vector2d newPosition = stateCopy.getAvatarPosition();
            
            // Can we can walk to an field without having the right orientation?
            if (!walkDirection.equals(originalOrientation)) {
                if (!originalPosition.equals(newPosition)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void GatherSpriteInfoRandomly(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        //Dynamiczne zbieranie informacji o grze
        StateObservation stCopy = stateObs.copy();
        Random generator = new Random();
        Position2D avatarPos = Position2D.GetAvatarPosition(stCopy);
        int testInRow = 0;
        while(elapsedTimer.remainingTimeMillis() > 10){
            if(stCopy.isGameOver() || CheckForNearbyNpc(stCopy, avatarPos)){
                stCopy = stateObs.copy();
                continue;
            }
            if(trySnapToGrid(stCopy, avatarPos)){
                continue;
            }
            avatarPos = Position2D.GetAvatarPosition(stCopy);
            if(!tryGetInfoFromSurrounding(stCopy, avatarPos)){
                int choice = generator.nextInt(4);
                switch(choice){
                    case 0:
                        stCopy.advance(Types.ACTIONS.ACTION_UP);
                        break;
                    case 1:
                        stCopy.advance(Types.ACTIONS.ACTION_DOWN);
                        break;
                    case 2:
                        stCopy.advance(Types.ACTIONS.ACTION_LEFT);
                        break;
                    case 3:
                        stCopy.advance(Types.ACTIONS.ACTION_RIGHT);
                        break;
                }
            }
            testInRow++;
        }
    }

    // Zwraca Obserwację, która nie jest znana, jeśli jest jedyną nieznaną obserwacją w tablicy.
    private Observation FindSingleUnknown(ArrayList<Observation> objects){
        Observation unknown = null;
        for(Observation o : objects){
            if(this.sprites[o.itype] == SpriteType.Unknown){
                if(unknown == null){
                    unknown = o;
                }
                else{
                    unknown = null;
                    break;
                }
            }
        }
        return unknown;
    }

    // Zakładam, że avatar jest centralnie na polu i sprawdzam sąsiednie pola na obecność NPC
    private boolean CheckForNearbyNpc(StateObservation stateObs, Position2D avatarPos) {
        if(avatarPos.x < 0 || avatarPos.y < 0 || avatarPos.x >= gameWidth || avatarPos.y >= gameHeight)
            return false;
        if(avatarPos.x >= 1){
            for(Observation o : stateObs.getObservationGrid()[(int)avatarPos.x - 1][(int)avatarPos.y]){
                if(o.category == NPC_CATEGORY)
                    return true;
            }
        }
        if(avatarPos.x < gameWidth - 1){
            for(Observation o : stateObs.getObservationGrid()[(int)avatarPos.x + 1][(int)avatarPos.y]){
                if(o.category == NPC_CATEGORY)
                    return true;
            }
        }
        if(avatarPos.y >= 1){
            for(Observation o : stateObs.getObservationGrid()[(int)avatarPos.x][(int)avatarPos.y - 1]){
                if(o.category == NPC_CATEGORY)
                    return true;
            }
        }
        if(avatarPos.y < gameHeight - 1){
            for(Observation o : stateObs.getObservationGrid()[(int)avatarPos.x][(int)avatarPos.y + 1]){
                if(o.category == NPC_CATEGORY)
                    return true;
            }
        }
        return false;
    }

    // Avatar centralnie na polu, sprawdzam czy w okolicy da się czegoś dowiedzieć i jeśli tak, to się dowiaduję 1 ruchem
    private boolean tryGetInfoFromSurrounding(StateObservation stateObs, Position2D avatarPos) {
        boolean gotInfo = false;
        if(tryLearnSingleUnknown(stateObs, avatarPos.x+1, avatarPos.y, Types.ACTIONS.ACTION_RIGHT)){
            gotInfo = true;
        }
        else if(tryLearnSingleUnknown(stateObs, avatarPos.x-1, avatarPos.y, Types.ACTIONS.ACTION_LEFT)){
            gotInfo = true;
        }
        else if(tryLearnSingleUnknown(stateObs, avatarPos.x, avatarPos.y-1, Types.ACTIONS.ACTION_UP)){
            gotInfo = true;
        }
        else if(tryLearnSingleUnknown(stateObs, avatarPos.x, avatarPos.y+1, Types.ACTIONS.ACTION_DOWN)){
            gotInfo = true;
        }
        return gotInfo;
    }

    private boolean tryLearnSingleUnknown(StateObservation stateObs, double x, double y, Types.ACTIONS action){
        if(x < 0 || y < 0 || x >= GameKnowledge.getInstance().gameWidth || y >= GameKnowledge.getInstance().gameHeight){
            return false;
        }
        Observation unknown = FindSingleUnknown(Position2D.GetObservationsFast(stateObs, x, y));
        boolean canLearn = (unknown != null);
        if(canLearn){
            double numberOfPoints = stateObs.getGameScore();
            Position2D avatarPosBefore = Position2D.GetAvatarPosition(stateObs);
            Vector2d avatarOrientationBefore = stateObs.getAvatarOrientation();
            stateObs.advance(action);
            Position2D avatarPosAfter = Position2D.GetAvatarPosition(stateObs);
            Vector2d avatarOrientationAfter = stateObs.getAvatarOrientation();
            if(!stateObs.isAvatarAlive()){
                sprites[unknown.itype] = SpriteType.Enemy;
            }
            else if(stateObs.getGameScore() > numberOfPoints){
                sprites[unknown.itype] = SpriteType.Point;
            }
            else if(avatarPosAfter.x == avatarPosBefore.x && avatarPosAfter.y == avatarPosBefore.y){
                // Obiekt zablokował przejście, ale się nie przesunął
                if(isAvatarOriented && (avatarOrientationBefore.x != avatarOrientationAfter.x ||
                        avatarOrientationBefore.y != avatarOrientationAfter.y)){
                    stateObs.advance(action);
                    avatarPosAfter = Position2D.GetAvatarPosition(stateObs);
                }
                if(FindSingleUnknown(Position2D.GetObservationsFast(stateObs, x, y)) == unknown
                    && avatarPosAfter.x == avatarPosBefore.x && avatarPosAfter.y == avatarPosBefore.y
                    && unknown.category == 4){
                    sprites[unknown.itype] = SpriteType.Wall;
                }
                else{
                    sprites[unknown.itype] = SpriteType.Other;
                }
            }
            else{
                sprites[unknown.itype] = SpriteType.Other;
            }
        }
        return canLearn;
    }

    private boolean trySnapToGrid(StateObservation stateObs, Position2D avatarPos) {
        PositionGridType grid = Position2D.GetAvatarPosition(stateObs).IsOnGrid();
        boolean positionChanged = false;
        Random generator = new Random();
        if(grid == PositionGridType.OffX){
            int choice = generator.nextInt(2);
            if(choice == 1){
                stateObs.advance(Types.ACTIONS.ACTION_RIGHT);
            }
            else{
                stateObs.advance(Types.ACTIONS.ACTION_LEFT);
            }
            positionChanged = true;
        }
        else if(grid == PositionGridType.OffY){
            int choice = generator.nextInt(2);
            if(choice == 1){
                stateObs.advance(Types.ACTIONS.ACTION_UP);
            }
            else{
                stateObs.advance(Types.ACTIONS.ACTION_DOWN);
            }
            positionChanged = true;
        }
        else if(grid == PositionGridType.OffGrid){
            int choice = generator.nextInt(4);
            switch(choice){
                case 0:
                    stateObs.advance(Types.ACTIONS.ACTION_UP);
                    break;
                case 1:
                    stateObs.advance(Types.ACTIONS.ACTION_DOWN);
                    break;
                case 2:
                    stateObs.advance(Types.ACTIONS.ACTION_LEFT);
                    break;
                case 3:
                    stateObs.advance(Types.ACTIONS.ACTION_RIGHT);
                    break;
            }
            positionChanged = true;
        }
        return positionChanged;
    }
}
