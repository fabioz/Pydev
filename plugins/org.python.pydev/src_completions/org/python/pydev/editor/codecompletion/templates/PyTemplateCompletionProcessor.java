/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.templates;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.editor.templates.TemplateHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class PyTemplateCompletionProcessor extends TemplateCompletionProcessor {

    private String currentContext;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
     */
    @Override
    protected Template[] getTemplates(String contextTypeId) {
        return TemplateHelper.getTemplateStore().getTemplates();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    @Override
    protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
        return TemplateHelper.getContextTypeRegistry().getContextType(this.currentContext);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
     */
    @Override
    protected Image getImage(Template template) {
        return PydevPlugin.getImageCache().get(UIConstants.COMPLETION_TEMPLATE);
    }

    /**
     * @param viewer
     * @param documentOffset
     * @param propList
     *  
     */
    public void addTemplateProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> propList) {
        IDocument doc = viewer.getDocument();
        if (doc.getLength() == 0) {
            this.currentContext = PyContextType.PY_MODULES_CONTEXT_TYPE;

        } else {
            this.currentContext = PyContextType.PY_COMPLETIONS_CONTEXT_TYPE;

        }

        String str = extractPrefix(viewer, documentOffset);

        ICompletionProposal[] templateProposals = computeCompletionProposals(viewer, documentOffset);

        for (int j = 0; j < templateProposals.length; j++) {
            if (templateProposals[j].getDisplayString().startsWith(str)) {
                propList.add(templateProposals[j]);
            }
        }

    }

    /**
     * Overridden so that we can do the indentation in this case.
     */
    @Override
    protected TemplateContext createContext(final ITextViewer viewer, final IRegion region) {
        TemplateContextType contextType = getContextType(viewer, region);
        return createContext(contextType, viewer, region);
    }

    public static PyDocumentTemplateContext createContext(final TemplateContextType contextType,
            final ITextViewer viewer, final IRegion region) {
        if (contextType != null) {
            IDocument document = viewer.getDocument();
            PySelection selection = new PySelection(document, viewer.getTextWidget().getSelection().x);
            String indent = selection.getIndentationFromLine();
            return createContext(contextType, viewer, region, indent);
        }
        return null;
    }

    /**
     * Creates a concrete template context for the given region in the document. This involves finding out which
     * context type is valid at the given location, and then creating a context of this type. The default implementation
     * returns a <code>DocumentTemplateContext</code> for the context type at the given location.
     *
     * @param contextType the context type for the template.
     * @param viewer the viewer for which the context is created
     * @param region the region into <code>document</code> for which the context is created
     * @return a template context that can handle template insertion at the given location, or <code>null</code>
     */
    public static PyDocumentTemplateContext createContext(final TemplateContextType contextType,
            final ITextViewer viewer, final IRegion region, String indent) {
        if (contextType != null) {
            IDocument document = viewer.getDocument();
            final String indentTo = indent;
            return new PyDocumentTemplateContext(contextType, document, region.getOffset(), region.getLength(),
                    indentTo, viewer);
        }
        return null;
    }

}
