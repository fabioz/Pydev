/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.eclipse.core.runtime.IStatus;
import org.python.parser.SimpleNode;
import org.python.parser.ast.*;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Creates the model from the AST tree.
 */
public class ModelMaker {

	/* algorithm:
	 * Traverse the top node.
	 * For each Item found, call foundItem.
	 * 
	 */
	public static ModuleNode createModel(SimpleNode root, int lines, int cols) {
		ModuleNode n = new ModuleNode(null, lines, cols);
		PopulateModel populator = new PopulateModel(root, n);
		try {
			root.accept(populator);
		} catch (Exception e) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
		}
		n.getScope().setEnd(n);
		return n;
	}
	
	/**
	 * Problems this class is trying to solve:
	 * 
	 * - figuring out the end position of the node. AST only has starting position,
	 * so we need to heuristically find the end of it
	 *
	 *
	 */
	static class PopulateModel extends VisitorBase {

		SimpleNode root;
		AbstractNode parent;
		
		public PopulateModel(SimpleNode root, AbstractNode parent) {
			this.root = root;
			this.parent = parent;
		}
		
		void processAliases(AbstractNode parent, aliasType[] nodes) {
			for (int i=0; i<nodes.length; i++)
				new ImportAlias(parent, nodes[i]);
		}

		void processImport(Import node) {
			ImportNode newNode = new ImportNode(parent, node);
			// have to traverse children manually to find all imports
			processAliases(newNode, node.names);
		}
		
		void processImportFrom(ImportFrom node) {
			ImportFromNode newNode = new ImportFromNode(parent, node);
			// have to traverse children manually to find all imports
			processAliases(newNode, node.names);		
		}
		
		void processClassDef(ClassDef node) {
			ClassNode newNode = new ClassNode(parent, node);
			// traverse inside the class definition			
			PopulateModel populator = new PopulateModel(node, newNode);
			try {
				node.traverse(populator);
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
			}
			newNode.getScope().setEnd(newNode);				
		}

		void processFunctionDef(FunctionDef node) {
			FunctionNode newNode = new FunctionNode(parent, node);
			// traverse inside the function definition			
			PopulateModel populator = new PopulateModel(node, newNode);
			try {
				node.traverse(populator);
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
			}	
			newNode.getScope().setEnd(newNode);				
		}

		void processLocal(Name node) {
			if (!LocalNode.isBuiltin(node.id))
				new LocalNode(parent, node);
		}

		void processFunctionCall(Call node) {
			FunctionCallNode newNode = new FunctionCallNode(parent, node);
			PopulateModel populator = new PopulateModel(node, newNode);
			try {
				node.traverse(populator);
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error populating model", e);
			}
		}

		void processMain(If node) {
			NameEqualsMainNode newNode = new NameEqualsMainNode(parent, node);
		}

		protected Object unhandled_node(SimpleNode node) throws Exception {
			return null;
		}

		public void traverse(SimpleNode node) throws Exception {
			node.traverse(this);
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
					return null;	
			}
			return super.visitIf(node);
		}
	}
}
