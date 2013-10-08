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
package org.python.pydev.shared_interactive_console.console.ui.internal;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

public interface IHandleScriptAutoEditStrategy extends IAutoEditStrategy {

    void customizeParenthesis(IDocument doc, DocumentCommand docCmd) throws BadLocationException;

    boolean canSkipCloseParenthesis(IDocument parenDoc, DocumentCommand docCmd) throws BadLocationException;

    void customizeNewLine(IDocument historyDoc, DocumentCommand docCmd) throws BadLocationException;

    String convertTabs(String cmd);

}
