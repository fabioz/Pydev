/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 17, 2006
 */
package org.python.pydev.parser.prettyprinterv2;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;

/**
 * This interface is provided for clients that want to implement code-formatting
 */
public interface IFormatter {

    /**
     * Formats the whole doc
     * @throws SyntaxErrorException 
     */
    void formatAll(IDocument doc, IPyFormatStdProvider edit, IFile f, boolean isOpenedFile, boolean throwSyntaxError)
            throws SyntaxErrorException;

    /**
     * Formats the passed regions.
     */
    void formatSelection(IDocument doc, int[] regionsToFormat, IPyFormatStdProvider edit, PySelection ps);

}
