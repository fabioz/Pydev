/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;
import org.python.pydev.editor.correctionassist.heuristics.AssistAssign;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleViewer;


/**
 * Shows quick assists for the console
 *
 * @author Fabio
 */
public class PydevConsoleQuickAssistProcessor implements IQuickAssistProcessor {

    public PydevConsoleQuickAssistProcessor(PyCorrectionAssistant quickAssist) {
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    @Override
    public boolean canFix(Annotation annotation) {
        return false;
    }

    /**
     * Computes quick assists for the console.
     */
    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
        ISourceViewer sourceViewer = invocationContext.getSourceViewer();
        List<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        if (sourceViewer instanceof ScriptConsoleViewer) {
            ScriptConsoleViewer viewer = (ScriptConsoleViewer) sourceViewer;

            //currently, only the assign quick assist is used
            AssistAssign assistAssign = new AssistAssign();

            ISelection selection = sourceViewer.getSelectionProvider().getSelection();
            if (selection instanceof ITextSelection) {
                PySelection ps = new PySelection(sourceViewer.getDocument(), (ITextSelection) selection);
                int offset = viewer.getCaretOffset();
                String commandLine = viewer.getCommandLine();

                //let's calculate the 1st line that is not a whitespace.
                if (assistAssign.isValid(ps.getSelLength(), commandLine, offset)) {
                    int commandLineOffset = viewer.getCommandLineOffset();
                    try {
                        IDocument doc = sourceViewer.getDocument();
                        while (true) {
                            if (commandLineOffset == doc.getLength() - 1) {
                                break;
                            }
                            char c = doc.getChar(commandLineOffset);
                            if (Character.isWhitespace(c)) {
                                commandLineOffset++;
                            } else {
                                break;
                            }
                        }
                        props.addAll(assistAssign.getProps(ps, PydevPlugin.getImageCache(), sourceViewer, offset,
                                commandLine, commandLineOffset));

                    } catch (BadLocationException e) {
                        Log.log(e);
                    }
                }
            }
        }
        return props.toArray(new ICompletionProposal[props.size()]);
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

}
