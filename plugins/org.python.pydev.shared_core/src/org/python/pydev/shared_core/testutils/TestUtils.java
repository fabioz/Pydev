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
package org.python.pydev.shared_core.testutils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class TestUtils {

    public static String getContentTypesAsStr(IDocument document) throws Exception
    {
        String last = null;

        List<String> found = new ArrayList<String>();
        FastStringBuffer buf = new FastStringBuffer();
        int len = document.getLength();
        for (int i = 0; i < len; i++) {
            String docContentType = document.getContentType(i);
            if (last == null) {
                buf.append(docContentType + ':' + i + ':');

            } else if (!last.equals(docContentType)) {
                buf.append(i);
                found.add(buf.toString());

                buf.clear();
                buf.append(docContentType + ':' + i + ':');
            }

            last = docContentType;
        }
        found.add(buf.toString());

        return listToExpected(found);
    }

    @SuppressWarnings("rawtypes")
    public static String listToExpected(List expected) {
        String[] f = new String[expected.size()];
        for (int i = 0; i < f.length; i++) {
            f[i] = expected.get(i).toString();
        }
        return listToExpected(f);
    }

    public static String listToExpected(String... expected) {
        FastStringBuffer expectedBuf = new FastStringBuffer();
        for (int i = 0; i < expected.length; i++) {
            String expectedToken = expected[i];
            expectedBuf.append("\"" + expectedToken + "\"");
            if (i != expected.length - 1) {
                expectedBuf.append(",\n");
            }
        }
        return expectedBuf.toString();
    }

    public static String scan(ITokenScanner scanner, IDocument document) {
        scanner.setRange(document, 0, document.getLength());

        ArrayList<String> found = new ArrayList<String>();
        FastStringBuffer buf = new FastStringBuffer();
        IToken token = scanner.nextToken();
        while (!token.isEOF()) {
            Object data = token.getData();
            if (data != null) {
                buf.clear();
                buf.append(data.toString()).append(":");
                buf.append(scanner.getTokenOffset()).append(":");
                buf.append(scanner.getTokenLength());
                found.add(buf.toString());
            } else {
                buf.clear();
                buf.append("null").append(":");
                buf.append(scanner.getTokenOffset()).append(":");
                buf.append(scanner.getTokenLength());
                found.add(buf.toString());
            }
            token = scanner.nextToken();
        }
        return listToExpected(found);
    }
}
