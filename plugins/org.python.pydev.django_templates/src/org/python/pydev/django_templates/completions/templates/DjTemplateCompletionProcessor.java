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

/**
 * @author Fabio Zadrozny
 */
public class DjTemplateCompletionProcessor extends TemplateCompletionProcessor{
    
    private final String contextType;
    private Image image;
    private boolean displayOnlyName;

    public DjTemplateCompletionProcessor(String contextType, Image image, boolean displayOnlyName){
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
    protected TemplateContextType getContextType(ITextViewer viewer,
            IRegion region) {
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
    protected ICompletionProposal createProposal(Template template, TemplateContext context, IRegion region, int relevance) {
        return new DjTemplateProposal(template, context, region, getImage(template), relevance, displayOnlyName);
    }


}
