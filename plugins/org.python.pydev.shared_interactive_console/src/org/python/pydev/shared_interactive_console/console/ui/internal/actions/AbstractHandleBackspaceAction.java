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
package org.python.pydev.shared_interactive_console.console.ui.internal.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

public abstract class AbstractHandleBackspaceAction {

    public abstract void execute(IDocument doc, ITextSelection selection, int commandLineOffset);

}
