/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.content_assist;

import org.eclipse.jface.text.contentassist.IContentAssistProcessor;

/**
 * This is an assist processor that can cycle through completions (all / templates) 
 *
 * @author Fabio
 */
public abstract class AbstractCompletionProcessorWithCycling implements IContentAssistProcessor {

    /**
     * This is the content assistant that is used to start this processor.
     */
    protected DefaultContentAssist contentAssistant;

    //-------- cycling through regular completions and templates
    public static final int SHOW_ALL = 1;
    public static final int SHOW_ONLY_TEMPLATES = 2;
    // Show only completions returned from the interpreter
    public static final int SHOW_FOR_TAB_COMPLETIONS = 3;
    protected int whatToShow = SHOW_ALL;

    public void startCycle() {
        whatToShow = SHOW_ALL;
    }

    protected void doCycle() {
        if (whatToShow == SHOW_ALL) {
            whatToShow = SHOW_ONLY_TEMPLATES;
        } else {
            whatToShow = SHOW_ALL;
        }
    }

    /**
     * Updates the status message.
     */
    public void updateStatus() {
        if (whatToShow == SHOW_ALL) {
            contentAssistant.setIterationStatusMessage("Press %s for templates.");
        } else {
            contentAssistant.setIterationStatusMessage("Press %s for default completions.");
        }
    }

    //-------- end cycling through regular completions and templates

    public AbstractCompletionProcessorWithCycling(DefaultContentAssist pyContentAssistant) {
        this.contentAssistant = pyContentAssistant;
    }
}
