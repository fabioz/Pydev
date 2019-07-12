/**
* Copyright (c) 2018 by Brainwy Software Ltda. All Rights Reserved.
* Licensed under the terms of the Eclipse Public License (EPL).
* Please see the license.txt included with this distribution for details.
* Any modifications to this file must keep this entire header intact.
*/
package org.python.pydev.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextOperationTargetExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorExtension5;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * Used to disable (enter macro mode) and re-enable (leave macro mode) features
 * in an editor which are not compatible with macro record or playback.
 */
public class MacroModeStateHandler {

    /**
     * Constant used to save whether the content assist was enabled before being
     * disabled in disableCodeCompletion.
     */
    private static final String CONTENT_ASSIST_ENABLED = "contentAssistEnabled";//$NON-NLS-1$

    /**
     * Constant used to save whether the quick assist was enabled before being
     * disabled in disableCodeCompletion.
     */
    private static final String QUICK_ASSIST_ENABLED = "quickAssistEnabled";//$NON-NLS-1$

    private IEditorPart fEditorPart;

    private Map<String, Boolean> fMemento = new HashMap<>();

    /**
     * @param editorPart
     *            the editor where macro record or playback will take place.
     */
    public MacroModeStateHandler(IEditorPart editorPart) {
        fEditorPart = editorPart;
    }

    /**
     * Disables features not compatible with macro record or playback.
     */
    public void enterMacroMode() {
        if (fEditorPart instanceof ITextEditorExtension5) {
            ITextEditorExtension5 iTextEditorExtension5 = (ITextEditorExtension5) fEditorPart;
            if (iTextEditorExtension5.isBlockSelectionModeEnabled()) {
                // Note: macro can't deal with block selections... there's nothing really
                // inherent to not being able to work, but given:
                // org.eclipse.jface.text.TextViewer.verifyEventInBlockSelection(VerifyEvent)
                // and the fact that we don't generate events through the Display (because it's
                // too error prone -- so much that it could target the wrong IDE instance for
                // the events because it deals with system messages and not really events
                // inside the IDE) and the fact that we can't force a new system message time
                // for temporary events created internally, makes it really hard to work
                // around the hack in verifyEventInBlockSelection.
                // So, we simply disable block selection mode as well as the action which would
                // activate it.

                // Note: ideally we'd have a way to set a new time for the time returned in
                // org.eclipse.swt.widgets.Display.getLastEventTime()
                // -- as it is, internally the events time will be always the same because
                // there's no API to reset it -- if possible we should reset it when we
                // generate our internal events at:
                // org.eclipse.ui.workbench.texteditor.macros.internal.StyledTextKeyDownMacroInstruction.execute(IMacroPlaybackContext)
                iTextEditorExtension5.setBlockSelectionMode(false);
            }
        }

        if (fEditorPart instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) fEditorPart;
            disable(textEditor, ITextEditorActionConstants.CONTENT_ASSIST);
            disable(textEditor, ITextEditorActionConstants.QUICK_ASSIST);
            disable(textEditor, ITextEditorActionConstants.BLOCK_SELECTION_MODE);
        }

        if (fEditorPart != null) {
            ITextOperationTarget textOperationTarget = fEditorPart.getAdapter(ITextOperationTarget.class);
            if (textOperationTarget instanceof ITextOperationTargetExtension) {
                ITextOperationTargetExtension targetExtension = (ITextOperationTargetExtension) textOperationTarget;

                // Disable content assist and mark it to be restored later on
                disable(textOperationTarget, targetExtension, ISourceViewer.CONTENTASSIST_PROPOSALS,
                        CONTENT_ASSIST_ENABLED);
                disable(textOperationTarget, targetExtension, ISourceViewer.QUICK_ASSIST, QUICK_ASSIST_ENABLED);
            }
        }
    }

    /**
     * Resets the state of the editor to what it was before macro record or playback
     * started.
     */
    public void leaveMacroMode() {
        if (fEditorPart != null) {
            ITextOperationTarget textOperationTarget = fEditorPart.getAdapter(ITextOperationTarget.class);
            if (textOperationTarget instanceof ITextOperationTargetExtension) {
                ITextOperationTargetExtension targetExtension = (ITextOperationTargetExtension) textOperationTarget;
                if (textOperationTarget instanceof ITextOperationTargetExtension) {
                    restore(targetExtension, ISourceViewer.CONTENTASSIST_PROPOSALS, CONTENT_ASSIST_ENABLED);
                    restore(targetExtension, ISourceViewer.QUICK_ASSIST, QUICK_ASSIST_ENABLED);
                }
            }

            if (fEditorPart instanceof ITextEditor) {
                ITextEditor textEditor = (ITextEditor) fEditorPart;
                restore(textEditor, ITextEditorActionConstants.CONTENT_ASSIST);
                restore(textEditor, ITextEditorActionConstants.QUICK_ASSIST);
                restore(textEditor, ITextEditorActionConstants.BLOCK_SELECTION_MODE);
            }
        }
    }

    private void restore(ITextOperationTargetExtension targetExtension, int operation, String preference) {
        Boolean contentAssistProposalsBeforMacroMode = fMemento.get(preference);
        if (contentAssistProposalsBeforMacroMode != null) {
            if ((contentAssistProposalsBeforMacroMode).booleanValue()) {
                targetExtension.enableOperation(operation, true);
            } else {
                targetExtension.enableOperation(operation, false);
            }
        }
    }

    private void restore(ITextEditor textEditor, String actionId) {
        Boolean b = fMemento.get(actionId);
        if (b != null && b) {
            Control control = textEditor.getAdapter(Control.class);
            if (control != null && !control.isDisposed()) {
                // Do nothing if already disposed.
                IAction action = textEditor.getAction(actionId);
                if (action instanceof TextEditorAction) {
                    TextEditorAction textEditorAction = (TextEditorAction) action;
                    textEditorAction.setEditor(textEditor);
                    textEditorAction.update();
                }
            }
        }
    }

    private void disable(ITextOperationTarget textOperationTarget, ITextOperationTargetExtension targetExtension,
            int operation, String preference) {
        if (textOperationTarget.canDoOperation(operation)) {
            fMemento.put(preference, true);
            targetExtension.enableOperation(operation, false);
        }
    }

    private void disable(ITextEditor textEditor, String actionId) {
        IAction action = textEditor.getAction(actionId);
        if (action != null && action instanceof TextEditorAction) {
            TextEditorAction textEditorAction = (TextEditorAction) action;
            fMemento.put(actionId, true);
            textEditorAction.setEditor(null);
            textEditorAction.update();
        }
    }

}
