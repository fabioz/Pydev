/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Compare;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.Pass;
import org.python.parser.ast.Str;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.aliasType;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Creates the model from the AST tree.
 * uses PopulateModel visitor pattern to create the tree.
 */
public class ModelMaker {

	/* algorithm:
	 * Traverse the top node.
	 * For each Item found, call foundItem.
	 * 
	 */
	public static ModuleNode createModel(SimpleNode root, IDocument doc, IPath file) {
		int lastLine = doc.getNumberOfLines();
		int lineLength = 255;
		try {
			IRegion r = doc.getLineInformation(lastLine-1);
			lineLength = r.getLength();
		} catch (BadLocationException e1) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error getting last line", e1);
		}
		ModuleNode n = new ModuleNode(file, lastLine, lineLength);
		PopulateModel populator = new PopulateModel(root, n, doc);
		try {
			root.accept(populator);
		} catch (Exception e) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
		}
		n.getScope().setEnd(n);
		return n;
	}
	
	/**
	 * Create the model by traversing AST tree.
	 *
	 * visit* functions are required by visitor patters.
	 * When the pattern finds something interesting, it calls process* which
	 * create the model.
	 */
	static class PopulateModel extends VisitorBase {

		SimpleNode root;
		AbstractNode parent;
		IDocument doc;
		
		public PopulateModel(SimpleNode root, AbstractNode parent, IDocument doc) {
			this.root = root;
			this.parent = parent;
			this.doc = doc;
		}
		
		private String getLineText(SimpleNode node) {
			try {
				IRegion lineInfo = doc.getLineInformation(node.beginLine -1);
				return doc.get(lineInfo.getOffset(), lineInfo.getLength());
			} catch (BadLocationException e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected getLineText error", e);
			}
			return "";
		}
		
		/** 
		 * processAliases creates Import tokens. 
		 * import os, sys would create 2 aliases
		 */
		void processAliases(AbstractNode parent, aliasType[] nodes) {
			for (int i=0; i<nodes.length; i++)
				new ImportAlias(parent, nodes[i], getLineText(nodes[i]));
		}

		void processImport(Import node) {
			ImportNode newNode = new ImportNode(parent, node, getLineText(node));
			// have to traverse children manually to find all imports
			processAliases(newNode, node.names);
		}
		
		void processImportFrom(ImportFrom node) {
			ImportFromNode newNode = new ImportFromNode(parent, node, getLineText(node));
			// have to traverse children manually to find all imports
			processAliases(newNode, node.names);		
		}
		
		void processClassDef(ClassDef node) {
			ClassNode newNode = new ClassNode(parent, node);
			// traverse inside the class definition			
			PopulateModel populator = new PopulateModel(node, newNode, doc);
			try {
				node.traverse(populator);
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
			}
			newNode.getScope().setEnd(newNode);				
		}

		void processFunctionDef(FunctionDef node) {
			FunctionNode newNode = new FunctionNode(parent, node, getLineText(node));
			// traverse inside the function definition			
			PopulateModel populator = new PopulateModel(node, newNode, doc);
			try {
				node.traverse(populator);
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
			}	
			newNode.getScope().setEnd(newNode);				
		}

		void processLocal(Name node) {
			if (!LocalNode.isBuiltin(node.id))
				new LocalNode(parent, node, getLineText(node));
		}

		void processFunctionCall(Call node) {
			FunctionCallNode newNode = new FunctionCallNode(parent, node, getLineText(node));
			PopulateModel populator = new PopulateModel(node, newNode, doc);
			try {
				node.traverse(populator);
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
			}
		}

		void processMain(If node) {
			new NameEqualsMainNode(parent, node);
		}

		void processPass(Pass node) {
			new PassNode(parent, node);
		}

		void processStr(Str node) {
			new StrNode(parent, node);
		}

		private void processAttribute(Attribute node) {
			new AttributeNode(parent, node, getLineText(node));
		}

		protected Object unhandled_node(SimpleNode node) throws Exception {
			//System.err.println("Unhandled: " + node.getClass().toString() + " L:" + Integer.toString(node.beginLine));
			return null;
		}

		public void traverse(SimpleNode node) throws Exception {
			node.traverse(this);
		}

		public Object visitPass(Pass node) throws Exception {
			processPass(node);
			return null;
		}

		public Object visitStr(Str node) throws Exception {
			processStr(node);
			return null;
		}

		public Object visitClassDef(ClassDef node) throws Exception {
			processClassDef(node);
			return null;
		}

		public Object visitFunctionDef(FunctionDef node) throws Exception {
			processFunctionDef(node);
			return null;
		}

		
		public Object visitImport(Import node) throws Exception {
			processImport(node);
			return null;
		}

		public Object visitImportFrom(ImportFrom node) throws Exception {
			processImportFrom(node);
			return null;
		}

		/* Is every name a local? */
		public Object visitName(Name node) throws Exception {
			processLocal(node);
			return null;
		}
		
		public Object visitCall(Call node) throws Exception {
			processFunctionCall(node);
			return null;
		}
		
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
					processMain(node);
			}
			return super.visitIf(node);
		}
	
		public Object visitAttribute(Attribute node) throws Exception {
			processAttribute(node);
			super.visitAttribute(node);
			return null;	// do not want to visit its children?
		}
	}
}
