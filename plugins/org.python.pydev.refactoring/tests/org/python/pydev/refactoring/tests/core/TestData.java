/******************************************************************************
* Copyright (C) 2007-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.tests.core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.refactoring.utils.FileUtils;
import org.python.pydev.refactoring.utils.TestUtils;
import org.python.pydev.refactoring.utils.TestUtils.Cursors;

public class TestData {
    /* allows ##c ##r, with or without comments on the same line */
    private static final Pattern MAGIC_LEXER_CONFIG = Pattern.compile("^##[cr][^\\n]*$", Pattern.MULTILINE);

    public String source;
    public String config;
    public String result;

    public ITextSelection sourceSelection;
    public ITextSelection resultSelection;
    public File file;

    public TestData(File file) {
        String content;
        this.file = file;

        try {
            content = FileUtils.read(file);
        } catch (IOException e) {
            throw new RuntimeException();
        }

        String[] parts = MAGIC_LEXER_CONFIG.split(content, 3);

        source = parts[0];
        if (parts.length == 3) {
            config = parts[1];
            result = parts[2];
        } else if (parts.length == 2) {
            config = "";
            result = parts[1];
        } else if (parts.length == 1) {
            config = "";
            result = source;
        } else {
            throw new RuntimeException("Invalid source file, only " + parts.length + " parts found in " + file);
        }

        source = source.trim();
        result = result.trim();
        config = config.trim();

        Cursors sourceCursors = TestUtils.findCursors(source);
        Cursors resultCursors = TestUtils.findCursors(result);

        source = sourceCursors.text;
        result = resultCursors.text;

        sourceSelection = parseSelection(sourceCursors.positions);
        resultSelection = parseSelection(resultCursors.positions);
    }

    private ITextSelection parseSelection(List<Integer> list) {
        if (list.size() == 1) {
            return new TextSelection(list.get(0), 0);
        } else if (list.size() == 2) {
            int start = list.get(0);
            int end = list.get(1);
            return new TextSelection(start, end - start);
        } else {
            return null;
        }
    }

    public String getConfigContents() {
        String c = config.trim();
        if (c.startsWith("'''")) {
            c = c.substring(3);
        }
        if (c.endsWith("'''")) {
            c = c.substring(0, c.length() - 3);
        }
        return c;
    }

}
