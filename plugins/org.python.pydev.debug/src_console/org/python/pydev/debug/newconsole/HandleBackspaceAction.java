/******************************************************************************
* Copyright (C) 2008-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.newconsole;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.AbstractHandleBackspaceAction;

/**
 * Executes a backspace action.
 *
 * @author fabioz
 */
public class HandleBackspaceAction extends AbstractHandleBackspaceAction {

    @Override
    public void execute(IDocument doc, ITextSelection selection, int commandLineOffset) {

        PyBackspace pyBackspace = new PyBackspace();
        pyBackspace.setDontEraseMoreThan(commandLineOffset);
        pyBackspace.setIndentPrefs(DefaultIndentPrefs.get(null));
        PySelection ps = new PySelection(doc, selection);

        pyBackspace.perform(ps);
    }

}
