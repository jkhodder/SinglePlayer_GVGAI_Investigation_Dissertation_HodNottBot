# An Investigation of Single-Player General Video-Game AI

This repository is used to store all files and directories related to my Masters Dissertation at the University of Nottingham

Within this repository is a directory named gvgai-framework2016, which is an IntelliJ project containing the following:
- The GVGAI competition framework
- Sample controllers for analysis and comparison, these are contained within the src folder
- HodNottBot implementation, contained in the src folder, and contains the implemented agent and an additional agent made for feature extraction
- FeatureExtraction.java, contained in the src folder, used to record game features
- GetPerformanceData.java, contained in the src folder, runs through each level of each available game 10 times with each controller, recording performance data
- RunSomeGames.java, contained in the src folder, used to run N number of games with a given controller, used for demonstrations and visual verification that a controller works
- Results of previous runs of GetPerformanceData, contained in src\results, these are not readable and must be processed with resultsCollector.py, individual runs that collected performance data for HodNottBot variants are contained in src\HodNottBotResults

Contained in the repository are also:
- CreateHeatMap.py, which allows for various types of heatmaps to be created using performance and categorisation data
- resultsCollector.py, which takes performance data and converts this into readable results that are then saved in the copyPasteResults directory
- Performance data, with sample controller data stored in ControllerExperimentData.xlsx, HodNottBot data stored in HodNottBotPerformanceData.xlsx, and data for all controllers stored in ControllerPerformanceData.xlsx, these can be read by CreateHeatMap.py
- CategoriseData.m, CategoriseSeparatedData.m, and BuildPerformanceClassTree.m, which are 3 matlab scripts that creates clusters for games, creates clusters for games using the Separated Classification technique that clusters solvable games seperately, and builds a tree with given categorisation indexes for use in the Performance Classification technique, respectively.
- The OldDataCollectionMethod directory, this contains depreciated data related to a previous method of feature extraction that was used previously, and is no longer relavant
- The CurrentDataCollectionMethod directory, this contains heatmaps, trees, line charts and matlab workspaces related to our results, as well as extracted game features in both the standard format and the Separated Classification format, also contained are the categorisations of each game for each classification method. For more information about the individual files, please note the appendix of the paper, which links relevant figures to the corresponding filenames.

