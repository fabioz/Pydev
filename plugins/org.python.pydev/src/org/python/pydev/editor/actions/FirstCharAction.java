/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.util.Collection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;

/**
 * @author Fabio Zadrozny 
 */
public class FirstCharAction extends PyAction {

    protected SourceViewer viewer;

    /**
     * Run to the first char (other than whitespaces) or to the real first char. 
     */
    @Override
    public void run(IAction action) {

        try {
            ITextEditor textEditor = getTextEditor();
            IDocument doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();

            perform(doc, selection);
        } catch (Exception e) {
            beep(e);
        }
    }

    private void perform(IDocument doc, ITextSelection selection) {
        boolean isAtFirstChar = isAtFirstVisibleChar(doc, selection.getOffset());
        if (!isAtFirstChar) {
            gotoFirstVisibleChar(doc, selection.getOffset());
        } else {
            gotoFirstChar(doc, selection.getOffset());
        }
    }

    @Override
    protected void setCaretPosition(int pos) throws BadLocationException {
        viewer.setSelectedRange(pos, 0);
    }

    /**
     * Creates a handler that will properly treat home considering python code (if it's still not defined
     * by the platform -- otherwise, just go with what the platform provides).
     */
    public static VerifyKeyListener createVerifyKeyListener(final SourceViewer viewer, final IWorkbenchPartSite site,
            boolean forceCreation) {
        // This only needs to be done for eclipse 3.2 (where line start is not
        // defined).
        // Eclipse 3.3 onwards already defines the home key in the text editor.

        final boolean isDefined;
        if (site != null) {
            ICommandService commandService = (ICommandService) site.getService(ICommandService.class);
            Collection definedCommandIds = commandService.getDefinedCommandIds();
            isDefined = definedCommandIds.contains("org.eclipse.ui.edit.text.goto.lineStart");

        } else {
            isDefined = false;
        }

        if (forceCreation || !isDefined) {
            return new VerifyKeyListener() {

                @Override
                public void verifyKey(VerifyEvent event) {
                    if (event.doit) {
                        boolean isHome;
                        if (isDefined) {
                            isHome = KeyBindingHelper.matchesKeybinding(event.keyCode, event.stateMask,
                                    "org.eclipse.ui.edit.text.goto.lineStart");
                        } else {
                            isHome = event.keyCode == SWT.HOME && event.stateMask == 0;
                        }
                        if (isHome) {
                            ISelection selection = viewer.getSelection();
                            if (selection instanceof ITextSelection) {
                                FirstCharAction firstCharAction = new FirstCharAction();
                                firstCharAction.viewer = viewer;
                                firstCharAction.perform(viewer.getDocument(), (ITextSelection) selection);
                                event.doit = false;
                            }
                        }
                    }
                }
            };
        }
        return null;
    }

}