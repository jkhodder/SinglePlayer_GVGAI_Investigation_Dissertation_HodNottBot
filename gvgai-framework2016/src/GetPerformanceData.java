import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import core.ArcadeMachine;
import core.competition.CompetitionParameters;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 04/10/13
 * Time: 16:29
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class GetPerformanceData
{

    public static void main(String[] args) throws IOException {
        String testName = "";

        //Available games:
        String gamesPath = "examples/gridphysics/";
        String games[] = new String[]{};
        String generateLevelPath = "examples/gridphysics/";

        //All public games
        games = new String[]{"aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", //0-4
                "blacksmoke", "boloadventures", "bomber", "boulderchase", "boulderdash",      //5-9
                "brainman", "butterflies", "cakybaky", "camelRace", "catapults",              //10-14
                "chainreaction", "chase", "chipschallenge", "clusters", "colourescape",       //15-19
                "chopper", "cookmepasta", "cops", "crossfire", "defem",                       //20-24
                "defender", "digdug", "dungeon", "eggomania", "enemycitadel",                 //25-29
                "escape", "factorymanager", "firecaster",  "fireman", "firestorms",           //30-34
                "freeway", "frogs", "gymkhana", "hungrybirds", "iceandfire",                  //35-39
                "infection", "intersection", "islands", "jaws", "labyrinth",                  //40-44
                "labyrinthdual", "lasers", "lasers2", "lemmings", "missilecommand",           //45-49
                "modality", "overload", "pacman", "painter", "plants",                        //50-54
                "plaqueattack", "portals", "racebet", "raceBet2", "realportals",              //55-59
                "realsokoban", "rivers", "roguelike", "run", "seaquest",                      //60-64
                "sheriff", "shipwreck", "sokoban", "solarfox" ,"superman",                    //65-69
                "surround", "survivezombies", "tercio", "thecitadel", "thesnowman",           //70-74
                "waitforbreakfast", "watergame", "waves", "whackamole", "witnessprotection",  //75-79
                "zelda", "zenpuzzle" };                                                       //80, 81

        String[] controllers = new String[]{"adrienctx", "asd592", "combination", "Jamie_Hutchison", "MaastCTS2",
                                    "NovelTS", "Number27", "RegyN", "UrbanSk", "YOLOBOT", "HodNottBot"};

        int seed;
        int numRuns = 10;

        for(int gameIdx = 0; gameIdx <= 81; gameIdx++) {
            System.out.println(games[gameIdx]);
            for (int levelIdx = 0; levelIdx < 5; levelIdx++) {
                System.out.println("Level: "+levelIdx);

                double[][][] results = new double[controllers.length][numRuns][3];
                String game = gamesPath + games[gameIdx] + ".txt";
                String level = gamesPath + games[gameIdx] + "_lvl" + levelIdx + ".txt";

                for (int runNumber = 0; runNumber < numRuns; runNumber++) {
                    System.out.println("Run number: " + (runNumber+1));
                    seed = new Random().nextInt();

                    for (int controllerIdx = 0; controllerIdx < controllers.length; controllerIdx++) {
                        System.out.println(controllers[controllerIdx]);
                        double[] runResult = ArcadeMachine.runOneGame(game, level, false, controllers[controllerIdx] + ".Agent", null, seed, 0);
                        if (runResult.length >= 1) {
                            results[controllerIdx][runNumber][0] = runResult[0];

                            if (runResult.length >= 2) {
                                results[controllerIdx][runNumber][1] = runResult[1];

                                if (runResult.length >= 3) {
                                    results[controllerIdx][runNumber][2] = runResult[2];
                                } else {
                                    results[controllerIdx][runNumber][2] = -3332021;
                                }

                            } else {
                                results[controllerIdx][runNumber][1] = -2222021;
                                results[controllerIdx][runNumber][2] = -2222021;
                            }

                        } else {
                            results[controllerIdx][runNumber][0] = -1112021;
                            results[controllerIdx][runNumber][1] = -1112021;
                            results[controllerIdx][runNumber][2] = -1112021;
                        }
                    }
                }
                System.out.println("Finished level " + levelIdx);


                File f = new File("C:\\Users\\James\\OneDrive - The University of Nottingham\\Dissertation\\gvgai-framework2016\\src\\newResults\\" + testName + games[gameIdx] + "Level" + levelIdx + "Results.csv");
                f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f, true);
                PrintWriter pw = new PrintWriter(fos);

                for (int lineIdx = 0; lineIdx < controllers.length; lineIdx++) {
                    String line = "";
                    for (int runIdx = 0; runIdx < results[lineIdx].length; runIdx++) {
                        if (line.length() == 0) {
                            line += controllers[lineIdx] + ", " + results[lineIdx][runIdx][0] + ";" + results[lineIdx][runIdx][1] + ";" + results[lineIdx][runIdx][2];
                        } else {
                            line += ", " + results[lineIdx][runIdx][0] + ";" + results[lineIdx][runIdx][1] + ";" + results[lineIdx][runIdx][2];
                        }
                    }
                    pw.println(line);
                }
                pw.close();
                fos.close();

                System.out.println("Saved level " + levelIdx + " results");
            }
        }

    }
}
