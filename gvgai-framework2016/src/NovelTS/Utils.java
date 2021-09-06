package NovelTS;
import tools.Vector2d;

public class Utils {

    public static int compareDistance(Atom a1, Atom a2) {
        return Math.abs(a1.getData1() - a2.getData1()) + Math.abs(a1.getData2() - a2.getData2());
    }

    public static int compareDistance(Vector2d a1, Atom a2) {
        return (int) (Math.abs(a1.x - a2.getData1()) + Math.abs(a1.y - a2.getData2()));
    }
}
