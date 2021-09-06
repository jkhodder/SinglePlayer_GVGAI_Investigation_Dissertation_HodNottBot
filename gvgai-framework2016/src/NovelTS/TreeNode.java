package NovelTS;

import core.game.StateObservation;
import ontology.Types.WINNER;
import ontology.Types.ACTIONS;
import java.util.Queue;
import java.util.ArrayList;


public class TreeNode {
	public int numGameOver;

	public int numVisit;

	// the parentnode
	public TreeNode parent;

	// contains all the child node
	private TreeNode[] children;

	// keep track of how many children are there
	public int numchild;

	// the depth of current node
	private int depth;

	// score will store the score of current node
	private double totalScore;

	// bestRout will store the best score of current route
	public double bestRoute;

	// best Child will store the index of the best children
	public int bestChild;

	// store the novelty of the current node
	private double novelty;

	// heuristic value
 	private double heuristic;

	public int childDepth;

	public Feature feature;

	public double reward;

	public boolean checked;
	// Huge number
	private int HUGE_NUMBER = 3000;

	public TreeNode() {
		checked = false;
		this.novelty = 1;
		this.parent = null;

		// contains all the child node
		children = new TreeNode[Agent.NUM_ACTIONS];

		// bestRout will store the best score of current route
		bestRoute = -HUGE_NUMBER - 1;

		// best Child will store the index of the best children
		bestChild = -1;

	}


	public TreeNode(TreeNode parent , int action, double novelty) {
		this.parent = parent;
		this.novelty = novelty;

		// contains all the child node
		this.children = new TreeNode[Agent.NUM_ACTIONS];

		//the depth of current node
		if (parent != null) {
			depth = parent.depth + 1;
		}

		// best Child will store the index of the best children
		bestChild = -1;
		checked = false;
	}


	/**
	 * This is the method responsible to find a child that is not expanded
	 * Changing this function will allow different behaviour of
	 * the search tree
	 */
	public int unprunedAction() {
		int option = -1;
		if (Param.search == Param.Search.OPEN_LOOP_BFS) {
			option = olbfs();
		}

		else if (Param.search == Param.Search.OPEN_LOOP_RANDOM_WALK) {
			option = olRandomWalk();
		}
		if (option == -1) {
			// All children node is pruned
			this.novelty = 0;
			return 0;
	 	}
		 return option;
	}

	public int olbfs() {
		int minDepth = 10000;
		int bestAction = -1;
		ArrayList<Integer> rnd = IWPlayer.randomAction.randomSequence();
		for (int i: rnd) {
			if (!children[i].isPruned()) {
				if (children[i].childDepth < minDepth) {
					bestAction = i;
					minDepth = children[i].childDepth;
				}
			}
		}

		return bestAction;
	}

	public int olRandomWalk() {
		int bestAction = -1;
		ArrayList<Integer> rnd = IWPlayer.randomAction.randomSequence();
		for (int i: rnd) {
			if (!children[i].isPruned()) {
				return bestAction;
			}
		}

		return bestAction;
	}


	/**
	 * This method will return an unexplored action of the current
	 * node.
	 */
	public int nextChild() {
		ArrayList<Integer> rnd = IWPlayer.randomAction.randomSequence();
		for (int i: rnd) {
			if (children[i]== null) {
				numchild += 1;
				return i;
			}
		}
		return 0;
	}


	/**
	* Update bestRoute, bestChild and childDepth
	*/
	public void update() {
		// Find a child that result in most reward
		this.bestChild =-1;
		this.bestRoute = -HUGE_NUMBER - 1;
		this.childDepth = 0;

		double route = 0 ;
		ArrayList<Integer> rnd = IWPlayer.randomAction.randomSequence();

		// Record the number of pruned children
		int numPruned = 0;

		// Record the number of dangerous children
		int numDanger = 0;

		int bestChildVisit = 0;

		double score = getScore();
		this.reward = 0;

		for (int i: rnd) {
			if (children[i]!= null) {
				double tempReward = children[i].getScore() - score;
				if (tempReward > reward) {
					reward = tempReward;
				}

				if (children[i].childDepth + 1 > this.childDepth) {
					this.childDepth = children[i].childDepth + 1;
				}

				if (children[i].numGameOver != 0) {
					numDanger += 1;
					route = -3000;
				}
				else if (children[i].isPruned()) {
					numPruned += 1;
					route = 0;
				} else {
					// If the child is not a leaf
					if (children[i].numchild != 0) {
						route = (tempReward + children[i].bestRoute) * 0.9;
					} else {
						route = (tempReward + heuristic) * 0.9;
					}

				}
				route += IWPlayer.randomAction.noice();
				if (route > this.bestRoute) {
					this.bestRoute = route;
					this.bestChild= i;
					bestChildVisit = children[i].numVisit;
				}
			}
		}
		// If all children are pruned, then set the current node to pruned
		// to avoid re-expansion
		if (Agent.NUM_ACTIONS == numPruned) {
			this.novelty = 0;
		}

		if (Agent.NUM_ACTIONS == numDanger) {
			this.novelty = 0;
			this.bestRoute = -3000;
			this.bestChild = -1;
		}

		// If all children are dangerous, then set the current node to dangerous

	}


	/**
	 * Method to return the best action of the root node
	 * Will return the node that result in highest reward
	 * If all child is in gameOver state, return the most
	 * visited node
	 */
	public int bestAction() {
		// Return the best child if not all child is pruned
		if (this.bestChild != -1) {
			return this.bestChild;
		}

		// Else return the most visited child
		ArrayList<Integer> rnd = IWPlayer.randomAction.randomSequence();
		int maxVisit = 0;
		for (int i: rnd) {
			if (children[i]!= null) {
				if (maxVisit < children[i].numVisit) {
					maxVisit = children[i].numVisit;
					this.bestChild = i;
				}
			}
		}
		return bestChild;
	}


	/**
	 * Calculate the total number of nodes
	 * Used for debug purpose
	 */
	public int countChild() {
		int count = 0;
		for (int i = 0; i<Agent.NUM_ACTIONS; i++) {
			if (children[i] != null) {
				count += children[i].countChild();
			}
		}
		return count +1;
	}


 	/** Getters and Setters **/
	public void addchild(TreeNode node, int index) {
		this.children[index] = node;
	}


	// Return the child with given index
	public TreeNode getchild(int i) {
		return children[i];
	}

	public double getScore() {
		return this.totalScore / this.numVisit;
	}

	// Check if current node is fully expanded
	public boolean isExpanded() {
		return numchild == Agent.NUM_ACTIONS;
	}


	public boolean isPruned() {
		if (Param.iwPrunning) {
			if (this.novelty == 0) {
				return true;
			}
		}

		if (Param.dangerPrunning) {
			if (this.dangerous()) {
				return true;
			}
		}
		return false;
	}


	public void updateScore(double score, double heuristic) {
		if (score == -HUGE_NUMBER) {
			this.numGameOver += 1;
		}

		this.totalScore += score;
		this.heuristic = heuristic;
		this.numVisit += 1;
	}

	public void updateScore(double score) {
		if (score == -HUGE_NUMBER) {
			this.numGameOver += 1;
		}
		this.totalScore += score;
		this.numVisit += 1;
	}

	public boolean dangerous() {
		return (float)numGameOver/ numVisit > 0.1;
	}
}
