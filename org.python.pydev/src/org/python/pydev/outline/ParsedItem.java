/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
 package org.python.pydev.outline;

import java.util.ArrayList;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Compare;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.aliasType;

/**
 * ParsedModel is composed of ParsedItems.
 * 
 * <p>The model gets an AST (Abstract Syntax Tree) from jython's parser
 * and this is then converted to a tree of ParsedItems
 */
public class ParsedItem  {
	ParsedItem parent;
	ParsedItem[] children = null; // array of modTypes
	SimpleNode token; // parser token that this node represents

	/**
	 * Traverses the parsed tree. 
	 * 
	 * <p>Fills the array list with the items we are interested in.
	 */
	static class Visitor extends VisitorBase {

		ArrayList fill;
		ParsedItem parent;

		boolean hasImports;
		ParsedItem imports;
		
		public Visitor(ParsedItem parent, ArrayList fill) {
			this.parent = parent;
			this.fill = fill;
		}

		protected Object unhandled_node(SimpleNode node) throws Exception {
			return null;
		}

		public void traverse(SimpleNode node) throws Exception {}
		
		public Object visitClassDef(ClassDef node) throws Exception {
			fill.add(new ParsedItem(parent, node));
			return null;
		}

		public Object visitImport(Import node) throws Exception {
			fill.add(new ParsedItem(parent, node));
			return null;
		}

		public Object visitImportFrom(ImportFrom node) throws Exception {
			fill.add(new ParsedItem(parent, node));
			return null;
		}

		public Object visitFunctionDef(FunctionDef node) throws Exception {
			fill.add(new ParsedItem(parent, node));
			return null;
		}
		
		// On if statements, we are looking to tag if __name__ == 'main' idiom
		public Object visitIf(If node) throws Exception {
			if (node.test instanceof Compare) {
				Compare compareNode = (Compare)node.test;
				// handcrafted structure walking
				if (compareNode.left instanceof Name 
					&& ((Name)compareNode.left).id.equals("__name__")
					&& compareNode.ops != null
					&& compareNode.ops.length == 1 
					&& compareNode.ops[0] == Compare.Eq)
					if ( true
					&& compareNode.comparators != null
					&& compareNode.comparators.length == 1
					&& compareNode.comparators[0] instanceof Str 
					&& ((Str)compareNode.comparators[0]).s.equals("__main__"))
					fill.add(new ParsedItem(parent, node));					
		}
			return super.visitIf(node);
		}

	}
	public ParsedItem(ParsedItem parent, SimpleNode token) {
		this.parent = parent;
		this.token = token;
	}
	public SimpleNode getToken() {
		return token;
	}
		
	public ParsedItem getParent() {
		return parent;
	}

	/*
	 * @return where in the document is this item located
	 */
	public IOutlineModel.SelectThis getPosition() {
		return getPosition(token);
	}

	/*
	 * @return where in the document is this item located
	 */
	public static IOutlineModel.SelectThis getPosition(SimpleNode token) {
		int startOffset = 0;
		boolean wholeLine = false;
		if (token instanceof ClassDef) {
			startOffset = 5;
		}
		else if (token instanceof FunctionDef) {
			startOffset = 3;
		}
		else if (token instanceof Import) {
			startOffset = -1;
		}
		else {
			// for others, just select the whole line, since the syntax tree -> document position calculation can be painful
			wholeLine = true;
		}
		
		IOutlineModel.SelectThis position = new IOutlineModel.SelectThis(token.beginLine, token.beginColumn+startOffset, toString(token).length());

		if (wholeLine)
			position.column = IOutlineModel.SelectThis.WHOLE_LINE;		
		return position;
	}
	
	public ParsedItem[] getChildren() {
		if (children == null) {
			ArrayList allMyChildren = new ArrayList();
			Visitor v = new Visitor(this, allMyChildren);
			try {
				if (token != null)
					token.traverse(v);	// traversal fills in the children
				children = new ParsedItem[allMyChildren.size()];
				for (int i=0; i<allMyChildren.size();i++)
					children[i] = (ParsedItem)allMyChildren.get(i);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return children;
	}
	
	
	public String toString() {
		return toString(token);
	}
	
	public static String toString(SimpleNode token) {
		if (token instanceof ClassDef) {
			return ((ClassDef)token).name;
		}
		else if (token instanceof FunctionDef) {
			return ((FunctionDef)token).name;
		}
		else if (token instanceof Import) {
			aliasType[] imports = ((Import)token).names;
			StringBuffer retVal = new StringBuffer();
			for (int i=0; i<imports.length; i++) {
				retVal.append(imports[i].name);
				retVal.append(", ");
			}
			retVal.delete(retVal.length() - 2, retVal.length());
			return retVal.toString();
		}
		else if (token instanceof ImportFrom) {
			// from wxPython.wx import *
			ImportFrom importToken = (ImportFrom)token;
			StringBuffer modules = new StringBuffer();
			for (int i=0; i<importToken.names.length;i++) {
				modules.append(importToken.names[i].name);
				modules.append(",");
			}
			if (modules.length() == 0) {
				modules.append("*,");
			}
			modules.deleteCharAt(modules.length()-1);
			return importToken.module + ".(" + modules.toString() + ")";
		}
		else if (token instanceof If) {
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
		if (token instanceof Import) {
			rank = 0;
		} else if (token instanceof ImportFrom) {
			rank = 1;
		} else if (token instanceof ClassDef) {
			rank = 2;
		} else if (token instanceof FunctionDef) {
			rank = 3;
		} else if (token instanceof If) {
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
	
