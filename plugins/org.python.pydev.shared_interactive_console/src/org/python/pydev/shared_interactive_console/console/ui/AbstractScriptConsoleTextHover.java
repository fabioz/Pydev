/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

public abstract class AbstractScriptConsoleTextHover implements ITextHover {

    protected abstract String getHoverInfoImpl(IScriptConsoleViewer viewer, IRegion hoverRegion);

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        return getHoverInfoImpl((IScriptConsoleViewer) textViewer, hoverRegion);
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        return new Region(offset, 0);
    }
}
