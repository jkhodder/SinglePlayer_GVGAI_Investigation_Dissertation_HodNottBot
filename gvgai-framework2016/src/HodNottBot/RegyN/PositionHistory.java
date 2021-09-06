package HodNottBot.RegyN;

import core.game.StateObservation;

import java.util.ArrayDeque;
import java.util.Queue;

public class PositionHistory {
    int length;
    Queue<Position2D> Positions;
    private static PositionHistory instance = null;

    private PositionHistory(){
        length = 200;
        Positions = new ArrayDeque<>();
    }

    public static PositionHistory GetInstance(){
        if(instance == null){
            instance = new PositionHistory();
        }
        return instance;
    }

    public static PositionHistory GetNew(){
        instance = new PositionHistory();
        return instance;
    }

    public static void Reset(){
        instance = new PositionHistory();
    }
    
    public boolean Contains(Position2D position){
        for(Position2D p : Positions){
            if(p.Equals(position)){
                return true;
            }
        }
        return false;
    }
    
    public int Count(Position2D position){
        int count = 0;
        for(Position2D p : Positions){
            if(p.Equals(position)){
                count++;
            }
        }
        return count;
    }

    public void Add(Position2D position){
        Positions.add(position);
        if(Positions.size() > length){
            Positions.remove();
        }
    }

    float getLocationBias(StateObservation state) {
        double power = 3.0;
        double tempLocationBias = 0.0;
        double timeDiscountFactor = 0.99;
        int i = 0;
        for (Position2D pos : Positions) {
            if (((Position2D.GetAvatarPosition(state).Equals(pos)))) {
                tempLocationBias += Math.pow(timeDiscountFactor, length - i) * 0.01;
            }
            i++;
        }
        return (float)Math.pow(1.0 - tempLocationBias, power);
    }
}
