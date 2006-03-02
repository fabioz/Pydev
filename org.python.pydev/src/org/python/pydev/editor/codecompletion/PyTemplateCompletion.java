/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class PyTemplateCompletion extends TemplateCompletionProcessor{
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
     */
    protected Template[] getTemplates(String contextTypeId) {
        return PydevPlugin.getDefault().getTemplateStore().getTemplates();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    protected TemplateContextType getContextType(ITextViewer viewer,
            IRegion region) {
        return PydevPlugin.getDefault().getContextTypeRegistry()
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
    protected void addTemplateProposals(ITextViewer viewer, int documentOffset,
            List propList) {
        
        String str = extractPrefix(viewer, documentOffset);

        ICompletionProposal[] templateProposals = 
                computeCompletionProposals(viewer, documentOffset);

        for (int j = 0; j < templateProposals.length; j++) {
            if ( templateProposals[j].getDisplayString().startsWith(str)){
                propList.add(templateProposals[j]);
            }
        }

    }

}
