/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.utils;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.parser.jython.SimpleNode;

public final class NodeUtils {

    private NodeUtils() {
    }

    public static int getOffset(IDocument doc, SimpleNode node) {
        int nodeOffsetBegin = PySelection.getAbsoluteCursorOffset(doc, node.beginLine - 1, node.beginColumn - 1);
        return nodeOffsetBegin;
    }

}
