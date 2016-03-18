/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package com.python.pydev.analysis.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.utils.Reflection;

public class PyGlobalsBrowserWorkbench implements IWorkbenchWindowActionDelegate {

    private ISelection selection;

    @Override
    public void dispose() {
    }

    @Override
    public void init(IWorkbenchWindow window) {
    }

    @Override
    public void run(IAction action) {
        String text = null;
        if (this.selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) this.selection;
            text = textSelection.getText();

            if (text == null || text.length() == 0) {
                //No selection... let's see if we can get a word there... 
                //(note: not using getDocument because only 3.5 has it)
                Object document = Reflection.getAttrObj(textSelection, "fDocument");
                //returns null if we couldn't get it.
                if (document instanceof IDocument) { // document != null
                    PySelection ps = new PySelection((IDocument) document, textSelection);
                    try {
                        text = ps.getCurrToken().o1;
                    } catch (BadLocationException e) {
                        //ignore
                    }
                }
            }
        }

        PyGlobalsBrowser.getFromWorkspace(text);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

}
