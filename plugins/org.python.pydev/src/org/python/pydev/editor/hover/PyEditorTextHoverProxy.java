/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 * 
 * Copied from the JDT implementation of
 * <code>org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverProxy</code>
 * and modified for PyDev.
 */
package org.python.pydev.editor.hover;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;

/**
 * Proxy for PyDev editor Text Hovers.
 */
public class PyEditorTextHoverProxy implements ITextHover, ITextHoverExtension {

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

    /*
     * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
     */
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        if (ensureHoverCreated() && fHover.isContentTypeSupported(this.contentType)) {
            return fHover.getHoverInfo(textViewer, hoverRegion);
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