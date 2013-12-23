/******************************************************************************
* Copyright (C) 2004-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.content_assist;

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;

public class DefaultContentAssist extends ContentAssistant {

    /**
     * Keeps a boolean indicating if the last request was an auto-activation or not.
     */
    private boolean lastAutoActivated;

    /**
     * The number of times this content assistant has been activated.
     */
    public int lastActivationCount;

    public DefaultContentAssist() {
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
        return super.showPossibleCompletions();
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

}
