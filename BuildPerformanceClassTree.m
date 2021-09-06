% call with [tree, error, fullC] = BuildPerformanceClassTree("gameFeatures.csv", "performanceCategories.csv")
% pruning trees can be done with prune(tree, "Level", level)
function [tree, error, fullC] = BuildPerformanceClassTree(featureFile, catFile)
    gameFeatures = readmatrix(featureFile)
    classes = readmatrix(catFile)
    classes = classes(:,2)
    
    fullC = zeros(size(gameFeatures, 1), 1)
    count = 1
    for i = (0:size(classes, 1)-1)*5+1
        fullC(i:i+4) = classes(count)
        count = count + 1
    end
    
    % we can build our tree with levels included for more
    % accuracy/observations for fitting to the tree
    tree = fitctree(gameFeatures, fullC)
    error = resubLoss(tree)
    
    if error >= 0.056 % alter for target error
        [tree, error, fullC] = BuildPerformanceClassTree(featureFile, catFile)
    else
        view(tree, "Mode", "graph")
    end
end
