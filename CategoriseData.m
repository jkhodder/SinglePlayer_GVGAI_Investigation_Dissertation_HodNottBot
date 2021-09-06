% call with [c, tree, error, optimalK, kMeansEval] = CategoriseData("automaticGameFeatures.csv", 10)
function [c, tree, error, optimalK, kMeansEval] = CategoriseData(filename, maxK)
    gameFeatures = readmatrix(filename)
    originalFeatures = gameFeatures % store for tree creation
    
    len = size(gameFeatures, 1)
    
    % rescale the features with the maximum value in each column being
    % scaled to 1, we add a new min to act as a floor, results from this
    % should look like: 10,4,3 -> 1,0.4,0.3, rather than 1,0.1429,0
    gameFeaturesWithMin = zeros(len+1,15)
    gameFeaturesWithMin(1:len,:) = gameFeatures
    for i = 1:size(gameFeatures,2)
        newScale = rescale(gameFeaturesWithMin(:,i), 0, 1)
        gameFeatures(:,i) = newScale(1:len,:)
    end

    kRange = 2:maxK
    kMeansFunc = @(X, K)(kmeans(X, K))
    kMeansEval = evalclusters(gameFeatures, kMeansFunc, "CalinskiHarabasz", "KList", kRange)
    optimalK = kMeansEval.OptimalK
    classes = kmeans(gameFeatures, optimalK)
    
    % make class for each game equal to mode of game levels
    c = zeros(len/5,1)
    count = 1
    for i = 1:5:len
        c(count,:) = mode(classes(i:i+4,:))
        count = count + 1
    end
    
    % we can build our tree with levels included for more
    % accuracy/observations for fitting to the tree
    tree = fitctree(originalFeatures, classes)
    error = resubLoss(tree)
    
    view(tree, "Mode", "graph")
    plot(kMeansEval)
end
