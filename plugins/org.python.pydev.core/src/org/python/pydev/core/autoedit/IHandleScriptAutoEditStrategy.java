/******************************************************************************
* Copyright (C) 2012-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.core.autoedit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.utils.IDocumentCommand;

public interface IHandleScriptAutoEditStrategy {

    /**
     * Allows the strategy to manipulate the document command.
     *
     * @param document the document that will be changed
     * @param command the document command describing the change
     */
    void customizeDocumentCommand(IDocument document, IDocumentCommand command);

    boolean canSkipCloseParenthesis(IDocument parenDoc, IDocumentCommand docCmd) throws BadLocationException;

    void customizeNewLine(IDocument historyDoc, IDocumentCommand docCmd) throws BadLocationException;

    String convertTabs(String cmd);

    void setConsiderOnlyCurrentLine(boolean considerOnlyCurrentLine);

}
