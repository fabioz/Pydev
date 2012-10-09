/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author Fabio Zadrozny
 */
package org.python.pydev.django_templates.completions.templates;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.codecompletion.templates.PyTemplateCompletionProcessor;

/**
 * @author Fabio Zadrozny
 */
public class DjTemplateCompletionProcessor extends TemplateCompletionProcessor {

    private final String contextType;
    private Image image;
    private boolean displayOnlyName;

    public DjTemplateCompletionProcessor(String contextType, Image image, boolean displayOnlyName) {
        this.contextType = contextType;
        this.image = image;
        this.displayOnlyName = displayOnlyName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
     */
    protected Template[] getTemplates(String contextTypeId) {
        return TemplateHelper.getTemplateStore().getTemplates();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
        return TemplateHelper.getContextTypeRegistry().getContextType(this.contextType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
     */
    protected Image getImage(Template template) {
        return image;
    }

    @Override
    protected ICompletionProposal createProposal(Template template, TemplateContext context, IRegion region,
            int relevance) {
        return new DjTemplateProposal(template, context, region, getImage(template), relevance, displayOnlyName);
    }

    /**
     * Overridden so that we can do the indentation in this case.
     */
    @Override
    protected TemplateContext createContext(final ITextViewer viewer, final IRegion region) {
        TemplateContextType contextType = getContextType(viewer, region);
        return PyTemplateCompletionProcessor.createContext(contextType, viewer, region);
    }
}
