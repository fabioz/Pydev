/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
 package org.python.pydev.outline;

import java.util.ArrayList;
import java.util.Iterator;

import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ClassNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.ImportFromNode;
import org.python.pydev.editor.model.ImportNode;
import org.python.pydev.editor.model.NameEqualsMainNode;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * ParsedModel is composed of ParsedItems.
 * 
 * <p>The model traverses Python model to get a tree of items for display.
 * We have to deal with token
 */
public class ParsedItem  {
	ParsedItem parent;
	ParsedItem[] children = null; // array of modTypes
	AbstractNode token; // parser token that this node represents. Can be null!

	public ParsedItem(ParsedItem parent, AbstractNode token) {
		this.parent = parent;
		this.token = token;
	}
	public AbstractNode getToken() {
		return token;
	}
		
	public ParsedItem getParent() {
		return parent;
	}

	/**
	 * Traverses AST model, and puts its children into our tree
	 */
	private void adoptChildrenOfNode(AbstractNode node, ArrayList children) {
		if (node == null)
			return;
		Iterator i = node.getChildren().iterator();
		while (i.hasNext()) {
			AbstractNode child = (AbstractNode)i.next();
			// these are the nodes we display
			if (child instanceof ClassNode ||
				child instanceof FunctionNode ||
				child instanceof ImportNode ||
				child instanceof ImportFromNode ||
				child instanceof NameEqualsMainNode)
				children.add(new ParsedItem(this, child));
			if (! (child instanceof ClassNode) && !(child instanceof FunctionNode))
				// functions & class will have their own children, do not traverse now
				adoptChildrenOfNode(child, children);
		}
	}
	
	public ParsedItem[] getChildren() {
		if (children == null) {
			ArrayList allMyChildren = new ArrayList();
			adoptChildrenOfNode(token, allMyChildren);
			children = new ParsedItem[allMyChildren.size()];
			for (int i=0; i<allMyChildren.size();i++)
				children[i] = (ParsedItem)allMyChildren.get(i);
		}
		return children;
	}
	
	
	public String toString() {
		return toString(token);
	}
	
	public static String toString(AbstractNode token) {
		if (token == null)
			return "null";
		if (token instanceof ClassNode) {
			return NodeUtils.getNameFromNameTok((NameTok) ((ClassNode)token).astNode.name);
		}
		else if (token instanceof FunctionNode) {
			return NodeUtils.getNameFromNameTok((NameTok) ((FunctionNode)token).astNode.name);
		}
		else if (token instanceof ImportNode) {
			aliasType[] imports = ((ImportNode)token).astNode.names;
			StringBuffer retVal = new StringBuffer();
			for (int i=0; i<imports.length; i++) {
			    aliasType aliasType = imports[i];
                
                //as ...
                if(aliasType.asname != null){
                    retVal.append(((NameTok)aliasType.asname).id);
                    retVal.append(" = ");
                }

                retVal.append(((NameTok)aliasType.name).id);
                retVal.append(", ");
			}
			retVal.delete(retVal.length() - 2, retVal.length());
			return retVal.toString();
		}
		else if (token instanceof ImportFromNode) {
			// from wxPython.wx import *
			ImportFrom importToken = ((ImportFromNode)token).astNode;
			StringBuffer modules = new StringBuffer();
			for (int i=0; i<importToken.names.length;i++) {
			    aliasType aliasType = importToken.names[i];

                //as ...
                if(aliasType.asname != null){
                    modules.append(((NameTok)aliasType.asname).id);
                    modules.append(" = ");
                }

                modules.append(((NameTok)aliasType.name).id);
				modules.append(",");
			}
			if (modules.length() == 0) {
				modules.append("*,"); //the comma will be deleted
			}
			modules.deleteCharAt(modules.length()-1);
			return modules.toString() + " (" + ((NameTok)importToken.module).id + ")";
		}
		else if (token instanceof NameEqualsMainNode) {
			return "__name__ == main";
		}
		else {
			return "ERROR";
		}
	}
	
	/**
	 * @return rank for sorting ParserItems. When comparing
	 * two items, first we compare class ranking, then titles
	 */
	public int getClassRanking() {
		int rank = 0;
		if (token instanceof ImportFromNode) {
			rank = 0;
		} else if (token instanceof ImportNode) {
			rank = 1;
		} else if (token instanceof ClassNode) {
			rank = 2;
		} else if (token instanceof FunctionNode) {
			rank = 3;
		} else if (token instanceof NameEqualsMainNode) {
			rank = 10;
		}
		return rank;
	}

	/**
	 * @param item
	 * @return compares ParsedItems by their rank
	 */
	public int compareTo(ParsedItem item) {
		int myRank = getClassRanking();
		int rank = item.getClassRanking();
		if (myRank == rank) {
			return toString().compareTo(item.toString());
		}
		else {
			return (myRank < rank ? -1 : 1);
		}
	}
}
	
