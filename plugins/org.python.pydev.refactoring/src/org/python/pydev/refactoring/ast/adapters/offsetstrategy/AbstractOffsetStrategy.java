/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public abstract class AbstractOffsetStrategy implements IOffsetStrategy {

    protected IDocument doc;

    protected IASTNodeAdapter<? extends SimpleNode> adapter;

    protected NodeHelper nodeHelper;

    public AbstractOffsetStrategy(IASTNodeAdapter<? extends SimpleNode> adapter, IDocument doc,
            AdapterPrefs adapterPrefs) {
        this.adapter = adapter;
        this.doc = doc;
        this.nodeHelper = new NodeHelper(adapterPrefs);
    }

    protected IRegion getRegion() throws BadLocationException {
        return doc.getLineInformation(getLine());
    }

    protected int getLineOffset() throws BadLocationException {
        return getRegion().getOffset();
    }

    @Override
    public int getOffset() throws BadLocationException {
        return getLineOffset();
    }

    /**
     * @return the line where the new code should be inserted. Note that when getting the offset, the
     * default implementation will get the start of this line (previously it got the last offset of the line).
     * 
     * So, if the code should be added before a method, one should return the function ast definition line -1 
     * (the -1 is before the ast starts at 1 and the doc at 0), and if it should be added at the last line,
     * it should return the last ast node line directly. 
     */
    protected abstract int getLine();

}
