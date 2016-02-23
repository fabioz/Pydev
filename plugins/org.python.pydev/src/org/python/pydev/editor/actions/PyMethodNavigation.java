/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: fabioz
 * Created: February 2004
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * Base class for function that go to next/previous class or function definition.
 * 
 * @author Fabio Zadrozny
 */
public abstract class PyMethodNavigation extends PyAction {

    /**
     * This method will search for the next/previous function (depending on the abstract methods)
     * and will go to the position in the document that corresponds to the name of the class/function definition.
     */
    @Override
    public void run(IAction action) {
        PyEdit pyEdit = getPyEdit();
        IDocument doc = pyEdit.getDocument();
        ITextSelection selection = (ITextSelection) pyEdit.getSelectionProvider().getSelection();

        boolean searchForward = getSearchForward();

        int startLine = selection.getStartLine();

        //we want to start searching in the line before/after the line we're in.
        if (searchForward) {
            startLine += 1;
        } else {
            startLine -= 1;
        }
        stmtType goHere = FastParser.firstClassOrFunction(doc, startLine, searchForward, pyEdit.isCythonFile());

        NameTok node = getNameNode(goHere);
        if (node != null) {
            //ok, somewhere to go
            pyEdit.revealModelNode(node);

        } else {
            //no place specified until now... let's try to see if we should go to the start or end of the file
            if (searchForward) {
                pyEdit.selectAndReveal(doc.getLength(), 0);

            } else {
                pyEdit.selectAndReveal(0, 0);
            }
        }
    }

    /**
     * @return true if the search should be forward (next method) and false if it should be backward (previous method)
     */
    protected abstract boolean getSearchForward();

    /**
     * @param defNode the ClassDef or FunctionDef from where we want to get the name
     * @return the name of the given statement
     */
    protected NameTok getNameNode(stmtType defNode) {
        NameTok node = null;
        if (defNode != null) {
            if (defNode instanceof ClassDef) {
                ClassDef def = (ClassDef) defNode;
                node = (NameTok) def.name;
            }
            if (defNode instanceof FunctionDef) {
                FunctionDef def = (FunctionDef) defNode;
                node = (NameTok) def.name;
            }
        }
        return node;
    }

}
