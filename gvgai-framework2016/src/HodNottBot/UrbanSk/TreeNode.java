package HodNottBot.UrbanSk;

import java.util.ArrayList;

import ontology.Types;

import core.game.StateObservation;

public class TreeNode {

    public TreeNode parent;
    public StateObservation content;
    
    public TreeNode(StateObservation content, TreeNode parent) {
	this.content=content;
	this.parent=parent;
    }

    public ArrayList<TreeNode> generateChildren(){
	ArrayList<TreeNode> ret = new ArrayList<TreeNode>();
	for (Types.ACTIONS action : content.getAvailableActions()){
	    StateObservation childContent=content.copy();
	    childContent.advance(action);
	    TreeNode child = new TreeNode(childContent, this);
	    ret.add(child);
	}
	
	return ret;
	
    }

}
