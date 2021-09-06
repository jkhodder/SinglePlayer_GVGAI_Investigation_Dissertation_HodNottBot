% call with [c, tree, error, optimalK, kMeansEval] = CategorisePrunedData("separatedGameFeatures.csv", 6)
function [c, tree, error, optimalK, kMeansEval] = CategoriseSeparatedData(filename, maxK)
    gameFeatures = readmatrix(filename)
    originalFeatures = gameFeatures(:,1:end-1) % store for tree creation
    
    unprunedInd = find(gameFeatures(:,end)==1)
    unpruned = gameFeatures(unprunedInd,1:end-1) % get points that end index == 1 and remove that column
    
    len = size(unpruned, 1)
    % rescale the features with the maximum value in each column being
    % scaled to 1, we add a new min to act as a floor, results from this
    % should look like: 10,4,3 -> 1,0.4,0.3, rather than 1,0.1429,0
    gameFeaturesWithMin = zeros(len+1,15)
    gameFeaturesWithMin(1:len,:) = unpruned
    for i = 1:size(unpruned,2)
        newScale = rescale(gameFeaturesWithMin(:,i), 0, 1)
        unpruned(:,i) = newScale(1:len,:)
    end
    
    kRange = 2:maxK
    kMeansFunc = @(X, K)(kmeans(X, K))
    kMeansEval = evalclusters(unpruned, kMeansFunc, "CalinskiHarabasz", "KList", kRange)
    optimalK = kMeansEval.OptimalK
    c = kmeans(unpruned, optimalK)
    
    fullC = zeros(size(gameFeatures, 1), 1)
    fullC(unprunedInd) = c % don't need to worry about the pruned games as classes start from 1, so unpruned is left as 0
    
    % make class for each game equal to mode of game levels
    gameNum = size(gameFeatures, 1)
    c = zeros(gameNum/5,1)
    count = 1
    for i = 1:5:gameNum
        c(count,:) = mode(fullC(i:i+4,:))
        count = count + 1
    end
    
    % we can build our tree with levels included for more
    % accuracy/observations for fitting to the tree
    tree = fitctree(originalFeatures, fullC)
    error = resubLoss(tree)
    
    if error >= 0.05 % alter for target error
        [c, tree, error, optimalK, kMeansEval] = CategorisePrunedData(filename, maxK)
    else
        plot(kMeansEval)
        view(tree, "Mode", "graph")
    end
end
