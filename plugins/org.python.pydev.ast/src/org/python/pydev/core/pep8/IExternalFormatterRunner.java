/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.pep8;

import java.io.File;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.formatter.ExternalFormatterException;
import org.python.pydev.core.formatter.FormatStd;

/**
 * Runs an external code formatter (black, ruff) over a document, optionally restricted to some lines.
 */
public interface IExternalFormatterRunner {

    /**
     * @param filepath the file being formatted (may be null): passed to the tool so that it can find
     * its configuration and apply per-file rules.
     * @param nature used to find the interpreter (may be null: the default interpreter is used).
     * @param doc the contents to format.
     * @param std formatter settings (tool location, parameters).
     * @param workingDir working directory for the tool (may be null).
     * @param zeroBasedLines the lines to format (0-based) or null to format the whole document.
     * @return the formatted contents.
     * @throws ExternalFormatterException if the tool could not be run or failed.
     */
    String format(String filepath, IPythonNature nature, IDocument doc, FormatStd std, File workingDir,
            int[] zeroBasedLines) throws ExternalFormatterException;

}
