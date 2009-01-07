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
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class PyTemplateCompletionProcessor extends TemplateCompletionProcessor{
    
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
    protected TemplateContextType getContextType(ITextViewer viewer,
            IRegion region) {
        return TemplateHelper.getContextTypeRegistry()
                .getContextType(PyContextType.PY_CONTEXT_TYPE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
     */
    protected Image getImage(Template template) {
        return PydevPlugin.getImageCache().get(UIConstants.COMPLETION_TEMPLATE);
    }

    /**
     * @param viewer
     * @param documentOffset
     * @param propList
     *  
     */
    public void addTemplateProposals(ITextViewer viewer, int documentOffset,
            List<ICompletionProposal> propList) {
        
        String str = extractPrefix(viewer, documentOffset);

        ICompletionProposal[] templateProposals = 
                computeCompletionProposals(viewer, documentOffset);

        for (int j = 0; j < templateProposals.length; j++) {
            if ( templateProposals[j].getDisplayString().startsWith(str)){
                propList.add(templateProposals[j]);
            }
        }

    }

    /**
     * Overridden so that we can do the indentation in this case.
     * 
     * Creates a concrete template context for the given region in the document. This involves finding out which
     * context type is valid at the given location, and then creating a context of this type. The default implementation
     * returns a <code>DocumentTemplateContext</code> for the context type at the given location.
     *
     * @param viewer the viewer for which the context is created
     * @param region the region into <code>document</code> for which the context is created
     * @return a template context that can handle template insertion at the given location, or <code>null</code>
     */
    @Override
    protected TemplateContext createContext(final ITextViewer viewer, final IRegion region) {
        TemplateContextType contextType= getContextType(viewer, region);
        if (contextType != null) {
            IDocument document= viewer.getDocument();
            String indent = "";
            PySelection selection = new PySelection(document, viewer.getTextWidget().getSelection().x);
            indent = selection.getIndentationFromLine();
            
            final String indentTo = indent;
            
            return new PyDocumentTemplateContext(contextType, document, region.getOffset(), region.getLength(), indentTo, viewer);
        }
        return null;
    }

}
