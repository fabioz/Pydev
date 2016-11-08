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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.editor.ColorCacheAndStyleForTesting.TextAttr;
import org.python.pydev.shared_core.string.FastStringBuffer;

import junit.framework.TestCase;

public class PyStringScannerTest extends TestCase {

    public void testStringTokenScanner() {
        ColorCacheAndStyleForTesting colorCache = new ColorCacheAndStyleForTesting();
        PyStringScanner pyStringScanner = new PyStringScanner(colorCache);
        Document document = new Document("@param foo: this is foo");
        pyStringScanner.setRange(document, 0, document.getLength());
        FastStringBuffer buf = new FastStringBuffer();
        while (true) {
            IToken nextToken = pyStringScanner.nextToken();
            if (nextToken.isEOF()) {
                break;
            }
            buf.append(((TextAttr) nextToken.getData()).data).append(':').append(pyStringScanner.getTokenOffset())
                    .append(':').append(pyStringScanner.getTokenLength()).append('\n');
        }
        assertEquals(""
                + "docstring_markup:0:6\n"
                + "string:6:1\n"
                + "string:7:16\n"
                + "", buf.toString());
    }

}
