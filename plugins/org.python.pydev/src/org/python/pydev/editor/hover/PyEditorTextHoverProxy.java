/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mark Leone - Modifications for PyDev
 *******************************************************************************/
package org.python.pydev.editor.hover;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;

/**
 * Proxy for PyDev editor Text Hovers.
 */
public class PyEditorTextHoverProxy implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

    private PyEditorTextHoverDescriptor fHoverDescriptor;

    private AbstractPyEditorTextHover fHover;

    private String contentType;

    public PyEditorTextHoverProxy(PyEditorTextHoverDescriptor descriptor, String contentType) {
        this.contentType = contentType;
        fHoverDescriptor = descriptor;
    }

    public boolean isEnabled() {
        return true;
    }

    /*
     * @see ITextHover#getHoverRegion(ITextViewer, int)
     */
    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        if (ensureHoverCreated() && fHover.isContentTypeSupported(this.contentType)) {
            return fHover.getHoverRegion(textViewer, offset);
        }

        return null;
    }

    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
        if (ensureHoverCreated() && fHover.isContentTypeSupported(this.contentType)) {
            return fHover.getHoverInfo2(textViewer, hoverRegion);
        }
        return null;
    }

    /*
     * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
     */
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        Object ret = getHoverInfo2(textViewer, hoverRegion);
        if (ret != null) {
            return ret.toString();
        }
        return null;
    }

    private boolean ensureHoverCreated() {
        if (!isEnabled() || fHoverDescriptor == null) {
            return false;
        }
        return isCreated() || createHover();
    }

    private boolean isCreated() {
        return fHover != null;
    }

    private boolean createHover() {
        fHover = fHoverDescriptor.createTextHover();
        return isCreated();
    }

    /*
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     * @since 3.0
     */
    @Override
    public IInformationControlCreator getHoverControlCreator() {
        if (ensureHoverCreated() && (fHover instanceof ITextHoverExtension)) {
            return ((ITextHoverExtension) fHover).getHoverControlCreator();
        }

        return null;
    }
}