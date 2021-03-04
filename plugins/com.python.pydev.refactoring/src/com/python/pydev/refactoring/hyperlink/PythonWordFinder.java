/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.hyperlink;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.shared_core.string.TextSelectionUtils;

/**
 * Just see if we can find a word at a given position in the document (same as JavaWordFinder)
 *
 * @author Fabio
 */
public class PythonWordFinder {

    public static IRegion findWord(IDocument document, int offset) {
        return TextSelectionUtils.findWord(document, offset);
    }
}
