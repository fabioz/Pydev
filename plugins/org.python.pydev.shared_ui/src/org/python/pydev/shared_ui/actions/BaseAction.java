/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

public class BaseAction extends Action implements IEditorActionDelegate {

    public BaseAction() {
        super();
    }

    public BaseAction(String text, int style) {
        super(text, style);
    }

    /**
     * @return true if the contents of the editor may be changed. Clients MUST call this before actually
     * modifying the editor.
     */
    public static boolean canModifyEditor(ITextEditor editor) {

        if (editor instanceof ITextEditorExtension2) {
            return ((ITextEditorExtension2) editor).isEditorInputModifiable();

        } else if (editor instanceof ITextEditorExtension) {
            return !((ITextEditorExtension) editor).isEditorInputReadOnly();

        } else if (editor != null) {
            return editor.isEditable();

        }

        //If we don't have the editor, let's just say it's ok (working on document).
        return true;
    }

    // Always points to the current editor
    protected volatile IEditorPart targetEditor;

    public void setEditor(IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
    }

    /**
     * This is an IEditorActionDelegate override
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        setEditor(targetEditor);
    }

    /**
     * Activate action  (if we are getting text)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        action.setEnabled(true);
    }

    /**
     * This function returns the text editor.
     */
    protected ITextEditor getTextEditor() {
        if (targetEditor instanceof ITextEditor) {
            return (ITextEditor) targetEditor;
        } else {
            throw new RuntimeException("Expecting text editor. Found:" + targetEditor.getClass().getName());
        }
    }

    /**
     * Helper for setting caret
     * @param pos
     * @throws BadLocationException
     */
    protected void setCaretPosition(int pos) throws BadLocationException {
        getTextEditor().selectAndReveal(pos, 0);
    }

    @Override
    public void run(IAction action) {

    }
}
