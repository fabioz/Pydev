/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.ui.pages.inlinelocal;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.InlineLocalRefactoring;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.InlineLocalRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.pages.core.eclipse.MessageWizardPage;

public class InlineTempInputPage extends MessageWizardPage {
    public static final String PAGE_NAME = "InlineTempInputPage"; //$NON-NLS-1$

    public InlineTempInputPage() {
        super(PAGE_NAME, true, MessageWizardPage.STYLE_QUESTION);
    }

    public void createControl(Composite parent) {
        super.createControl(parent);
    }

    protected String getMessageString() {
        InlineLocalRequestProcessor req = getInlineRefactoring().getRequestProcessor();
        int occurences = req.getOccurences();
        String variableName = req.getVariableName();
        if (occurences == 1) {
            return Messages.format(Messages.inlineLocalMessage, variableName);
        } else {
            return Messages.format(Messages.inlineLocalMessageMany, variableName, occurences);
        }
    }

    public InlineLocalRefactoring getInlineRefactoring() {
        return (InlineLocalRefactoring) getRefactoring();
    }
}
