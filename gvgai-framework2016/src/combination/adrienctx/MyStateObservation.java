package combination.adrienctx;

import core.game.Observation;
import tools.Vector2d;

import java.util.ArrayList;

import combination.YoloState;

/**
 * Created by acouetoux on 30/06/16.
 * Modified by Saskia Friedrich, 01/12/2017
 */
public class MyStateObservation {

    public YoloState yoloState;

    public MyStateObservation(YoloState _s){
    	yoloState = _s;
    }

    @Override
    public boolean equals(Object arg){
        MyStateObservation obj = (MyStateObservation) arg;
        Vector2d pos1 = this.yoloState.getAvatarPosition();
        Vector2d pos2 = obj.yoloState.getAvatarPosition();
        Vector2d orientation1 = this.yoloState.getAvatarOrientation();
        Vector2d orientation2 = obj.yoloState.getAvatarOrientation();

        if(!(pos1.equals(pos2))){
            return false;
        }
        if(!(orientation1.equals(orientation2))){
            return false;
        }

        ArrayList<Observation>[] obsList1 = this.yoloState.getNpcPositions();
        ArrayList<Observation>[] obsList2 = obj.yoloState.getNpcPositions();
        if(!areTwoObsListEqual(obsList1, obsList2)){
            return false;
        }
        obsList1 = this.yoloState.getMovablePositions();
        obsList2 = obj.yoloState.getMovablePositions();
        if(!areTwoObsListEqual(obsList1, obsList2)){
            return false;
        }
        obsList1 = this.yoloState.getResourcesPositions();
        obsList2 = obj.yoloState.getResourcesPositions();
        if(!areTwoObsListEqual(obsList1, obsList2)){
            return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        int code = 0;
        ArrayList<Observation>[] obsList1 = yoloState.getNpcPositions();
        code += getSumOfDistances(obsList1);
        obsList1 = yoloState.getMovablePositions();
        code += getSumOfDistances(obsList1);
        obsList1 = yoloState.getResourcesPositions();
        code += getSumOfDistances(obsList1);

        return code;
    }

    private int getSumOfDistances(ArrayList<Observation>[] obsList){
        int i = 0;
        int j = 0;
        int sum = 0;
        if (obsList != null) {
            while (i < obsList.length) {
                while (j < obsList[i].size()) {
                    sum += (int)obsList[i].get(j).sqDist;
                    j++;
                }
                i++;
            }
        }
        return sum;
    }

    private boolean areTwoObsListEqual(ArrayList<Observation>[] obsList1, ArrayList<Observation>[] obsList2){
        int i = 0;
        int j = 0;
        if ((obsList1 != null) && (obsList2 != null)) {
            if (obsList1.length != obsList2.length) {
                return false;
            } else {
                while (i < obsList1.length) {
                    if (obsList1[i].size() != obsList2[i].size()) {
                        return false;
                    } else {
                        while (j < obsList1[i].size()) {
                            if (!obsList1[i].get(j).equals(obsList2[i].get(j))) {
                                return false;
                            }
                            j++;
                        }
                    }
                    i++;
                }
            }
        }
        return true;
    }
}
