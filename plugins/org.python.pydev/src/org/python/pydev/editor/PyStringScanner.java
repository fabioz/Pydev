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
package org.python.pydev.editor;

import org.eclipse.jface.text.rules.Token;
import org.python.pydev.ui.ColorAndStyleCache;

public class PyStringScanner extends AbstractStringScanner {

    public PyStringScanner(ColorAndStyleCache colorCache) {
        super(colorCache);
    }

    @Override
    public void updateColorAndStyle() {
        fStringReturnToken = new Token(colorCache.getStringTextAttribute());
        fDocStringMarkupTextReturnToken = new Token(colorCache.getDocstringMarkupTextAttribute());
    }

}
