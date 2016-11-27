package org.python.pydev.shared_ui.content_assist;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class ContentAssistHackingAroundBugs {

    public static void fixAssistBugs(final ContentAssistant assistant) {
        if (PlatformUtils.isLinuxPlatform()) {
            // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=508245 (hack can be removed when that's fixed).

            assistant.addCompletionListener(new ICompletionListener() {
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
                        final Object completionProposalPopup = fProposalPopupField.get(assistant);
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
}
