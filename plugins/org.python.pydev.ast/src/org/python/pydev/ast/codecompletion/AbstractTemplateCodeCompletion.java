/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.shared_core.callbacks.ICallback0;

public abstract class AbstractTemplateCodeCompletion extends AbstractPyCodeCompletion {

    public static ICallback0<TemplateContextType> getTemplateContextType;

    /**
     * Creates a concrete template context for the given region in the document. This involves finding out which
     * context type is valid at the given location, and then creating a context of this type. The default implementation
     * returns a <code>DocumentTemplateContext</code> for the context type at the given location.
     *
     * @param viewer the viewer for which the context is created
     * @param region the region into <code>document</code> for which the context is created
     * @return a template context that can handle template insertion at the given location, or <code>null</code>
     */
    protected TemplateContext createContext(IRegion region, IDocument document) {
        TemplateContextType contextType = getContextType(region);
        if (contextType != null) {
            return new DocumentTemplateContext(contextType, document, region.getOffset(), region.getLength());
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    protected TemplateContextType getContextType(IRegion region) {
        if (getTemplateContextType != null) {
            return getTemplateContextType.call();
        }
        return new TemplateContextType();
    }

}
