/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.BadLocationException;

public interface IOffsetStrategy {
    int AFTERINIT = 1;
    int BEGIN = 2;
    int END = 4;
    int BEFORECURRENT = 8;

    /**
     * @return the offset where the code should be inserted.
     */
    int getOffset() throws BadLocationException;
}
