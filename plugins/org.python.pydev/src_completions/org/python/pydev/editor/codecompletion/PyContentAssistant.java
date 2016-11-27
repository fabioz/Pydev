/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.content_assist.DefaultContentAssist;

/**
 * @author Fabio Zadrozny
 */
public class PyContentAssistant extends DefaultContentAssist {

    public PyContentAssistant() {
        super();
        enableColoredLabels(true);

        if (PlatformUtils.isLinuxPlatform()) {
            // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=508245 (hack can be removed when that's fixed).

            this.addCompletionListener(new ICompletionListener() {
                Table fTable;
                Listener fKeyDownListener;
                boolean fCalled = false;

                @Override
                public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
                    // We do it in selectionChanged (and not on assistSessionStarted) because the table is only created
                    // at this point.
                    if (!fCalled) {
                        fCalled = true;
                    } else {
                        return;
                    }

                    try {
                        Field fProposalPopupField = ContentAssistant.class.getDeclaredField("fProposalPopup");
                        fProposalPopupField.setAccessible(true);
                        final Object completionProposalPopup = fProposalPopupField.get(PyContentAssistant.this);
                        Field fProposalTableField = completionProposalPopup.getClass()
                                .getDeclaredField("fProposalTable");
                        fProposalTableField.setAccessible(true);
                        Table table = (Table) fProposalTableField.get(completionProposalPopup);
                        if (table == null || table.isDisposed()) {
                            return;
                        }
                        fKeyDownListener = new Listener() {

                            @Override
                            public void handleEvent(Event event) {
                                if (event.character == '\n' || event.character == '\r') {
                                    try {
                                        Method m = completionProposalPopup.getClass()
                                                .getDeclaredMethod("insertSelectedProposalWithMask", int.class);
                                        m.setAccessible(true);
                                        m.invoke(completionProposalPopup, event.stateMask);
                                    } catch (Throwable e) {
                                        Log.log(e);
                                    }
                                }
                            }
                        };
                        table.addListener(SWT.KeyDown, fKeyDownListener);
                        fTable = table;
                    } catch (Throwable e) {
                        // Just ignore if this hack stops working.
                    }
                }

                @Override
                public void assistSessionStarted(ContentAssistEvent event) {
                }

                @Override
                public void assistSessionEnded(ContentAssistEvent event) {
                    if (fCalled) {
                        fCalled = false;
                        if (fTable != null && !fTable.isDisposed() && fKeyDownListener != null) {
                            fTable.removeListener(SWT.KeyDown, fKeyDownListener);
                        }
                        fTable = null;
                        fKeyDownListener = null;
                    }
                }
            });
        }
    }

    /**
     * Shows the completions available and sets the lastAutoActivated flag
     * and updates the lastActivationCount.
     */
    @Override
    public String showPossibleCompletions() {
        try {
            return super.showPossibleCompletions();
        } catch (RuntimeException e) {
            Throwable e1 = e;
            while (e1.getCause() != null) {
                e1 = e1.getCause();
            }
            if (e1 instanceof JDTNotAvailableException) {
                return e1.getMessage();
            }
            throw e;
        }
    }

    public static IInformationControlCreator createInformationControlCreator(ISourceViewer sourceViewer) {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, new PyInformationPresenter());
            }
        };
    }

}
