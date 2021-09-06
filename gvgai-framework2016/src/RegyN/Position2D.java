package RegyN;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;

enum PositionGridType{
    OnGrid,
    OffX,
    OffY,
    OffGrid
}

public class Position2D {
    public double x;
    public double y;

    public Position2D(double x, double y){
        this.x = x;
        this.y = y;
    }

    public Position2D(){
        this.x = 0;
        this.y = 0;
    }
    
    public static Position2D GetAvatarPosition(StateObservation obs){
        Position2D res = new Position2D();
        res.x = obs.getAvatarPosition().x / obs.getBlockSize();
        res.y = obs.getAvatarPosition().y / obs.getBlockSize();
        return res;
    }

    public static ArrayList<Observation> GetObservations(StateObservation obs, double x, double y){
        int baseX = (int) x;
        int baseY = (int) y;
        if(baseX >= obs.getObservationGrid().length || baseY >= obs.getObservationGrid()[0].length || baseX < 0 || baseY < 0) {
            return new ArrayList<>();
        }
        int nextX = baseX;
        int nextY = baseY;
        double margin = 0.01;
        ArrayList<Observation> results = new ArrayList<>(obs.getObservationGrid()[baseX][baseY]);
        if(baseX - x > margin && baseX > 1.0){
            nextX = baseX - 1;
        }
        else if(x - baseX > margin && baseX < (obs.getWorldDimension().width / obs.getBlockSize() - 1)){
            nextX = baseX + 1;
        }
        if(baseY - y > margin && baseY > 1.0){
            nextY = baseY - 1;
        }
        else if(y - baseY > margin && baseY < (obs.getWorldDimension().height / obs.getBlockSize() - 1)){
            nextY = baseY + 1;
        }

        if(nextY != baseY && nextX != baseX){
            results.addAll(obs.getObservationGrid()[nextX][nextY]);
            results.addAll(obs.getObservationGrid()[baseX][nextY]);
            results.addAll(obs.getObservationGrid()[nextX][baseY]);
        }
        else if(nextX != baseX){
            results.addAll(obs.getObservationGrid()[nextX][baseY]);
        }
        else if(nextY != baseY){
            results.addAll(obs.getObservationGrid()[baseX][nextY]);
        }
        return results;
    }

    public static ArrayList<Observation> GetObservationsFast(StateObservation obs, double x, double y){
        return obs.getObservationGrid()[(int) x][(int) y];
    }

    public static Position2D ModifyPosition(Position2D source, Types.ACTIONS action){
        Position2D newPosition = source;
        double speed = GameKnowledge.getInstance().avatarSpeed;
        if(action == Types.ACTIONS.ACTION_UP){
            newPosition = new Position2D(source.x, source.y - speed);
        }
        else if(action == Types.ACTIONS.ACTION_DOWN){
            newPosition = new Position2D(source.x, source.y + speed);
        }
        else if(action == Types.ACTIONS.ACTION_LEFT){
            newPosition = new Position2D(source.x - speed, source.y);
        }
        else if(action == Types.ACTIONS.ACTION_RIGHT){
            newPosition = new Position2D(source.x + speed, source.y);
        }
        return newPosition;
    }

    public Vector2d AsVector(){
        return new Vector2d(x, y);
    }

    public PositionGridType IsOnGrid(){
        int baseX = (int) x;
        int baseY = (int) y;
        double margin = 0.01;
        boolean offX = (double) baseX > x + margin || (double) baseX < x - margin;
        boolean offY = (double) baseY > y + margin || (double) baseY < y - margin;
        if(offX){
            if(offY){
                return PositionGridType.OffGrid;
            }
            return PositionGridType.OffX;
        }
        else{
            if(offY){
                return PositionGridType.OffY;
            }
            return PositionGridType.OnGrid;
        }
    }

    public boolean Equals(Position2D other){
        return x == other.x && y == other.y;
    }
}
