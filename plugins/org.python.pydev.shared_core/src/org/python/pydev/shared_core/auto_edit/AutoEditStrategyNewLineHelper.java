/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.auto_edit;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public class AutoEditStrategyNewLineHelper {

    public static boolean isNewLineText(IDocument document, int length, String text) {
        return length == 0 && text != null && TextSelectionUtils.endsWithNewline(document, text) && text.length() < 3; //could be \r\n
    }
}
