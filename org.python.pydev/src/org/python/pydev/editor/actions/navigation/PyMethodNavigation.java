/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions.navigation;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.VisitorBase;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.outline.ParsedItem;
import org.python.pydev.outline.ParsedModel;
import org.python.pydev.outline.SelectionPosition;

/**
 * The trick here is getting the outline... To do that, some refactorings had
 * to be done to the PyOutlinePage, to get the parsed items and the ParsedItem,
 * so that it is now public.
 * 
 * @author Fabio Zadrozny
 */
public abstract class PyMethodNavigation extends PyAction {

	/**
	 * This class is interested in knowing where are we...
	 * 
	 * @author Fabio Zadrozny
	 *
	 */
	class Visitor extends VisitorBase {

		/**
		 * The initial line starts in 0
		 */
		public int initialLine;

		/**
		 * This is the previous node.
		 */
		public SimpleNode prevNode = null;

		/**
		 * This is the current node. (Its begin line
		 * starts at 1 and not 0).
		 */
		public SimpleNode currentNode = null;

		/**
		 * This is the next found node.
		 */
		public SimpleNode nextNode = null;

		/**
		 * We have to know the initialLine, so that we can know where we are.
		 * @param initialLine
		 */
		public Visitor(int initialLine) {
			this.initialLine = initialLine;
		}

		/**
		 * Marks the current, previous and next node...
		 * @param node
		 */
		private void mark(SimpleNode node) {
			if (this.initialLine >= node.beginLine - 1) {
				if (this.currentNode != null) {
					this.prevNode = this.currentNode;
				}
				this.currentNode = node;
			} else if (nextNode == null) { //only sets the next node once...
				nextNode = node;
			}
		}

		public Object visitClassDef(ClassDef node) throws Exception {
//			print("visiting...visitClassDef");
			mark(node);
			node.traverse(this);
			return null;
		}

		public Object visitFunctionDef(FunctionDef node) throws Exception {
//			print("visiting...visitFunctionDef");
			mark(node);
			return null;
		}

		protected Object unhandled_node(SimpleNode node) throws Exception {
			return null;
		}

		public void traverse(SimpleNode node) throws Exception {
		}

	}

	/**
	 * This method gets the parsed model, discovers where we are in the
	 * document (through the visitor), and asks the implementing class
	 * to where we should go... 
	 */
	public void run(IAction action) {
		PyEdit pyEdit = getPyEdit();
		IDocument doc =
			pyEdit.getDocumentProvider().getDocument(pyEdit.getEditorInput());
		ITextSelection selection =
			(ITextSelection) pyEdit.getSelectionProvider().getSelection();
		
		ParsedModel model = new ParsedModel(null, pyEdit.getParser());
		ParsedItem item = (ParsedItem)model.getRoot();
		SimpleNode node = item.getToken();

		if (node == null)
			return;

		int startLine = selection.getStartLine();
		Visitor v = whereAmI(startLine, node);
		
//		print (v.nextNode);
		SelectionPosition select = getSelect(v);
//		print("select = " + select);
		if (select != null) {
			pyEdit.selectSelectionInEditor(select);
		}
		model.dispose();
	}

	/**
	 * Returns a visitor that knows where we are and the nodes next to me...
	 * 
	 * @param startLine
	 * @param root
	 * @return
	 */
	public Visitor whereAmI(int startLine, SimpleNode root) {
		Visitor v = new Visitor(startLine);
		try {
			synchronized (v) {
				root.traverse(v);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return v;
	}

	/**
	 * This method should return to where we should go, depending on
	 * the visitor passed as a parameter (it contains the node where we
	 * are, the next node and the previous node).
	 * 
	 * @param v
	 * @return
	 */
	public abstract SelectionPosition getSelect(Visitor v);

}
