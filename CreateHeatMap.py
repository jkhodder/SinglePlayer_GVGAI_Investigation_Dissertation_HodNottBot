import pandas as pd
import numpy as np
import matplotlib
import matplotlib.pyplot as plt
from sklearn import preprocessing

WIN_INDEX = 2
TIMEOUT_INDEX = 4
FAILED_INDEX = 5
COMPLETED_SCORE_INDEX = 6
COMPLETED_TIME_INDEX = 8
SCORE_INDEX = 7
TIME_INDEX = 9

# Set this to change the values compared in the heatmap
used_index = COMPLETED_TIME_INDEX
normalisation_needed = True

# get names and categories for each game
# with open("CurrentDataCollectionMethod\\automaticCategories.csv") as f:
with open("CurrentDataCollectionMethod\\seperatedCategories.csv") as f:
# with open("CurrentDataCollectionMethod\\performanceCategories.csv") as f:
    lines = f.readlines()
    print(lines)
    lines = list(map(lambda x: (x[0], int(x[1])), map(lambda x: x[:-1].split(",") , lines)))

    games = {}
    categories = {}
    for pair in lines:
        games[pair[0]] = {}
        if pair[1] not in categories:
            categories[pair[1]] = [pair[0]]
        else:
            categories[pair[1]].append(pair[0])

# read excel file
# performance_data = pd.ExcelFile("ControllerExperimentData.xlsx")
performance_data = pd.ExcelFile("ControllerPerformanceData.xlsx")
# performance_data = pd.ExcelFile("HodNottBotPerformanceData.xlsx")
for game in games:
    # get sheet data, we don't need to worry about the template sheet
    game_data = performance_data.parse(game)

    # due to the files formatting, the first controller for each game has totals on the 5th row, with each next controller being a further 6 rows down
    for i in range(0, 91, 6):
    # for i in range(0, 31, 6):
        # get the controllers name
        controllerName = game_data.iloc[i,0]
        # code will refer to wins but this index can be changed by modifying the index contained in used_index
        controllerTotalWins = game_data.iloc[i+5,used_index]
        games[game][controllerName] = controllerTotalWins

# stores intermediate category data
totalWins = 0
categoryResults = {}
for category in categories:
    categoryResults[category] = {}
    for game in categories[category]:
        for controllerName in games[game]:
            if controllerName not in categoryResults[category]:
                categoryResults[category][controllerName] = games[game][controllerName]
            else:
                categoryResults[category][controllerName] += games[game][controllerName]
            totalWins += games[game][controllerName]

# stores data to be plotted
# controllerNames = ["adrienctx", "asd592", "combination", "Jamie_Hutchison", "MaastCTS2", "NovelTS", "Number27", "RegyN", "UrbanSk", "YOLOBOT"] # this defines the order and controllers you want to appear in the heatmap, has to be equal to string in performance data excel sheets
# controllerNames = ["Jamie_Hutchison", "MaastCTS2", "YOLOBOT", "combination", "adrienctx", "RegyN", "NovelTS", "asd592", "Number27", "UrbanSk"] # this defines the order and controllers you want to appear in the heatmap, has to be equal to string in performance data excel sheets
controllerNames = ["Jamie_Hutchison", "MaastCTS2", "YOLOBOT", "combination", "adrienctx", "RegyN", "NovelTS", "asd592", "Number27", "UrbanSk", "HodNottBot - Automatic Static Categories", "HodNottBot - Automatic Dynamic Categories", "HodNottBot - Seperated Static Categories", "HodNottBot - Seperated Dynamic Categories", "HodNottBot - Performance-Based Static Categories", "HodNottBot - Performance-Based Dynamic Categories"] # this defines the order and controllers you want to appear in the heatmap, has to be equal to string in performance data excel sheets
# controllerNames = ["HodNottBot - Automatic Static Categories", "HodNottBot - Automatic Dynamic Categories", "HodNottBot - Seperated Static Categories", "HodNottBot - Seperated Dynamic Categories", "HodNottBot - Performance-Based Static Categories", "HodNottBot - Performance-Based Dynamic Categories"] # this defines the order and controllers you want to appear in the heatmap, has to be equal to string in performance data excel sheets
usableResults = []
categoryKeys = list(categoryResults.keys())
categoryKeys.sort()
for category in categoryKeys:
    usableResults.append([])
    for controllerName in controllerNames:
        # we divide the wins to get a win percentage, there are 50 games played per game
        winLossRatio = categoryResults[category][controllerName] / (len(categories[category]) * 50)
        usableResults[-1].append(round(winLossRatio, 3))

# get results for all categories per controller
totalControllerResults = [round(sum([categoryResults[category][controller] for category in categoryKeys])/(len(games)*50), 3) for controller in controllerNames]
usableResults.append(totalControllerResults)

if normalisation_needed:
    for categoryIdx in range(len(usableResults)):
        cat_results = preprocessing.normalize([usableResults[categoryIdx]])
        usableResults[categoryIdx] = list(map(lambda x: round(x,3), list(cat_results[0])))

# get results for all controllers per category
totalCategoryResults = [round(sum(category)/len(controllerNames), 3) for category in usableResults]
for categoryIdx in range(len(usableResults)):
    usableResults[categoryIdx].append(totalCategoryResults[categoryIdx])

usableResults = np.array(usableResults)

categoryHeadings = ["category {}\n".format(category)+"\n".join([",".join(categories[category][i:i+5]) for i in range(0, len(categories[category]), 5)]) for category in categories]
categoryHeadings.sort() # will sort based on "category n" at beginning of each heading string
categoryHeadings.append("All Games")

# controllerTitles = ["adrienctx\nOLETS", "asd592\nBFS/GA", "combination\nMCTS/BFS/OLETS\n(adrienctx+YOLOBOT)", "Jamie_Hutchison\nMCTS", "MaastCTS2\nMCTS", "NovelTS\nIW (BFS)", "Number27\nBFS/GA", "RegyN\nBFS (Max Tree)", "UrbanSk\nGA", "YOLOBOT\nMCTS/BFS"] # change this to include techniques
# controllerTitles = ["Jamie_Hutchison\nMCTS", "MaastCTS2\nMCTS", "YOLOBOT\nMCTS/BFS", "combination\nMCTS/BFS/OLETS\n(adrienctx+YOLOBOT)", "adrienctx\nOLETS", "RegyN\nBFS (Max Tree)", "NovelTS\nIW (BFS)", "asd592\nBFS/GA", "Number27\nBFS/GA", "UrbanSk\nGA", "All Controllers"] # change this to include techniques
controllerTitles = ["Jamie_Hutchison\nMCTS", "MaastCTS2\nMCTS", "YOLOBOT\nMCTS/BFS", "combination\nMCTS/BFS/OLETS\n(adrienctx+YOLOBOT)", "adrienctx\nOLETS", "RegyN\nBFS (Max Tree)", "NovelTS\nIW (BFS)", "asd592\nBFS/GA", "Number27\nBFS/GA", "UrbanSk\nGA", "HodNottBot\nAutomatic\nStatic", "HodNottBot\nAutomatic\nDynamic", "HodNottBot\nSeperated\nStatic", "HodNottBot\nSeperated\nDynamic", "HodNottBot\nPerformance\nStatic", "HodNottBot\nPerformance\nDynamic", "All Controllers"] # change this to include techniques
# controllerTitles = ["HodNottBot\nAutomatic\nStatic", "HodNottBot\nAutomatic\nDynamic", "HodNottBot\nSeperated\nStatic", "HodNottBot\nSeperated\nDynamic", "HodNottBot\nPerformance\nStatic", "HodNottBot\nPerformance\nDynamic", "All Controllers"] # change this to include techniques

# taken from: https://matplotlib.org/stable/gallery/images_contours_and_fields/image_annotated_heatmap.html
figure, ax = plt.subplots()
image = ax.imshow(usableResults)
ax.set_xticks(np.arange(len(controllerTitles)))
ax.set_yticks(np.arange(len(categoryHeadings)))
ax.set_xticklabels(controllerTitles)
ax.set_yticklabels(categoryHeadings)
# plt.setp(ax.get_xticklabels(), rotation=45, ha="right", rotation_mode="anchor")

for i in range(len(categoryHeadings)):
    for j in range(len(controllerTitles)):
        text = ax.text(j, i, usableResults[i, j], ha="center", va="center", color="w")

# figure.tight_layout()
plt.show()
