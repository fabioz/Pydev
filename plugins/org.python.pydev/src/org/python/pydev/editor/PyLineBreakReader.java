/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class PyLineBreakReader {

    private BufferedReader fReader;

    /**
     * Creates a reader that breaks an input text to fit in a given width.
     *
     * @param reader Reader of the input text
     * @param maxLineWidth The max width (pixels) where the text has to fit in
     */
    public PyLineBreakReader(Reader reader) {
        fReader = new BufferedReader(reader);
    }

    /**
     * Reads the next line. The lengths of the line will not exceed the given maximum
     * width.
     *
     * @return the next line
     * @throws IOException
     */
    public String readLine() throws IOException {
        return fReader.readLine();
    }

}
