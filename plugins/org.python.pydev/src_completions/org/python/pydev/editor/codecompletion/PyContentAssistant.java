/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.bindingutils.KeyBindingHelper;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.PyInformationPresenter;

/**
 * @author Fabio Zadrozny
 */
public class PyContentAssistant extends ContentAssistant {

    /**
     * Keeps a boolean indicating if the last request was an auto-activation or not.
     */
    private boolean lastAutoActivated;

    /**
     * The number of times this content assistant has been activated.
     */
    public int lastActivationCount;

    public PyContentAssistant() {
        this.enableAutoInsert(true);
        this.lastAutoActivated = true;

        try {
            setRepeatedInvocationMode(true);
        } catch (Exception e) {
            //no need to log
        }

        try {
            setRepeatedInvocationTrigger(KeyBindingHelper.getContentAssistProposalBinding());
        } catch (Exception e) {
            //no need to log
        }

        try {
            setStatusLineVisible(true);
        } catch (Exception e) {
            //no need to log
        }
    }

    /**
     * Shows the completions available and sets the lastAutoActivated flag
     * and updates the lastActivationCount.
     */
    @Override
    public String showPossibleCompletions() {
        lastActivationCount += 1;
        lastAutoActivated = false;
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

    /**
     * @return true if the last time was an auto activation (and updates
     * the internal flag regarding it).
     */
    public boolean getLastCompletionAutoActivated() {
        boolean r = lastAutoActivated;
        lastAutoActivated = true;
        return r;
    }

    public void setIterationStatusMessage(String string) {
        setStatusMessage(StringUtils.format(string, getIterationGesture()));
    }

    private String getIterationGesture() {
        TriggerSequence binding = KeyBindingHelper.getContentAssistProposalBinding();
        return binding != null ? binding.format() : "completion key";
    }

    /**
     * Available for stopping the completion.
     */
    @Override
    public void hide() {
        super.hide();
    }

    public static IInformationControlCreator createInformationControlCreator(ISourceViewer sourceViewer) {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, new PyInformationPresenter());
            }
        };
    }

}
