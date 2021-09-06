def collectData(gameName, levels, resultsPath, outputPath):
    collectedData = {}

    for level in range(levels):
        filePath = "{}\\{}Level{}Results.csv".format(resultsPath, gameName, level)
        print(filePath)
        with open(filePath) as file:
            for line in file.readlines():
                lineContents = line[:-1].split(", ")
                lineOwner = lineContents[0]
                lineData = lineContents[1:]

                if lineOwner not in collectedData:
                    collectedData[lineOwner] = []

                collectedData[lineOwner].append([0,0,0,0,0,0,0,0])

                for run in lineData:
                    data = run.split(";")
                    if data[2] == "-2222021.0":
                        collectedData[lineOwner][-1][3] += 1
                        continue
                    collectedData[lineOwner][-1][5] += float(data[1])
                    collectedData[lineOwner][-1][7] += float(data[2])
                    if data[0] == "-100.0":
                        collectedData[lineOwner][-1][2] += 1
                        continue
                    collectedData[lineOwner][-1][4] += float(data[1])
                    collectedData[lineOwner][-1][6] += float(data[2])
                    if data[0] == "1.0":
                        collectedData[lineOwner][-1][0] += 1
                    else:
                        collectedData[lineOwner][-1][1] += 1

                print("{}: {}".format(lineOwner, collectedData[lineOwner][-1]))

    with open("{}\\{}CollectedData.csv".format(outputPath, gameName), "w") as file:
        for controller in collectedData.keys():
            count = 0
            for run in collectedData[controller]:
                count+=1
                file.write("{}_{}, {}\n".format(controller, count, ", ".join(str(x) for x in run)))



# collectData("zenpuzzle", 5, "C:\\Users\\James\\OneDrive - The University of Nottingham\\Dissertation\\gvgai-framework2016\\src\\results", "C:\\Users\\James\\OneDrive - The University of Nottingham\\Dissertation\\HodNottBotCopyPasteResults")
games = ["aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait",
 "blacksmoke", "boloadventures", "bomber", "boulderchase", "boulderdash",
  "brainman", "butterflies", "cakybaky", "camelRace", "catapults",
   "chainreaction", "chase", "chipschallenge", "clusters", "colourescape",
    "chopper", "cookmepasta", "cops", "crossfire", "defem",
     "defender", "digdug", "dungeon", "eggomania", "enemycitadel",
      "escape", "factorymanager", "firecaster",  "fireman", "firestorms",
       "freeway", "frogs", "gymkhana", "hungrybirds", "iceandfire",
        "infection", "intersection", "islands", "jaws", "labyrinth",
         "labyrinthdual", "lasers", "lasers2", "lemmings", "missilecommand",
          "modality", "overload", "pacman", "painter", "plants",
           "plaqueattack", "portals", "racebet", "raceBet2", "realportals",
            "realsokoban", "rivers", "roguelike", "run", "seaquest",
             "sheriff", "shipwreck", "sokoban", "solarfox" ,"superman",
              "surround", "survivezombies", "tercio", "thecitadel", "thesnowman",
               "waitforbreakfast", "watergame", "waves", "whackamole", "witnessprotection",
                "zelda", "zenpuzzle"]

# testName = "SeperatedStatic"
# testName = "SeperatedDynamic"
# testName = "AutomaticStatic"
# testName = "AutomaticDynamic"
# testName = "SeperatedDynamic"
# testName = "PerformanceStatic"
testName = "PerformanceDynamic"
for game in games:
    collectData("{}{}".format(testName, game), 5, "C:\\Users\\James\\OneDrive - The University of Nottingham\\Dissertation\\gvgai-framework2016\\src\\HodNottBotResults", "C:\\Users\\James\\OneDrive - The University of Nottingham\\Dissertation\\copyPasteResults")
