/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.scope.ASTEntry;

/**
 * The trick here is getting the outline... To do that, some refactorings had
 * to be done to the PyOutlinePage, to get the parsed items and the ParsedItem,
 * so that it is now public.
 * 
 * @author Fabio Zadrozny
 */
public abstract class PyMethodNavigation extends PyAction {

	/**
	 * This method gets the parsed model, discovers where we are in the
	 * document (through the visitor), and asks the implementing class
	 * to where we should go... 
	 */
	public void run(IAction action) {
		PyEdit pyEdit = getPyEdit();
		IDocument doc = pyEdit.getDocumentProvider().getDocument(pyEdit.getEditorInput());
		ITextSelection selection = (ITextSelection) pyEdit.getSelectionProvider().getSelection();

		ASTEntry goHere = getSelect(pyEdit.getAST(), selection.getStartLine());
        SimpleNode node = getNameNode(goHere);
        if(node != null){
            //ok, somewhere to go
            pyEdit.revealModelNode(node);
        }else{
            //no place specified until now... let's try to see if we should go to the start or end of the file
            if(goToEndOfFile()){
                pyEdit.selectAndReveal(doc.getLength(), 0);
            }else if(goToStartOfFile()){
                pyEdit.selectAndReveal(0, 0);
            }
        }
	}

	protected SimpleNode getNameNode(ASTEntry goHere) {
		SimpleNode node = null;
        if(goHere != null){
        	if(goHere.node instanceof NameTok || goHere.node instanceof Name){
        		node = goHere.node;
        	}
            if(goHere.node instanceof ClassDef){
                ClassDef def = (ClassDef) goHere.node;
                node = def.name;
            }
            if(goHere.node instanceof FunctionDef){
                FunctionDef def = (FunctionDef) goHere.node;
                node = def.name;
            }
        }
		return node;
	}

	protected abstract boolean goToEndOfFile() ;

	protected abstract boolean goToStartOfFile() ;

    /**
	 * This method should return to where we should go, depending on
	 * the ast passed as a parameter
	 * 
	 * @return the entry to where we should go
	 */
	public abstract ASTEntry getSelect(SimpleNode ast, int line);

}
