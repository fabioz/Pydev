package org.python.pydev.django_templates.completions.templates;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;

import com.aptana.editor.common.contentassist.ICommonCompletionProposal;

public class DjTemplateProposal extends TemplateProposal implements ICommonCompletionProposal {

    private boolean isDefault;
    private boolean isSuggested;
    private boolean displayOnlyName;

    public DjTemplateProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance, boolean displayOnlyName) {
        super(template, context, region, image, relevance);
        this.displayOnlyName = displayOnlyName;
    }

    public String getFileLocation() {

        return "";
    }

    public Image[] getUserAgentImages() {

        return null;
    }

    public boolean isDefaultSelection() {

        return isDefault;
    }

    public boolean isSuggestedSelection() {

        return isSuggested;
    }

    public void setIsDefaultSelection(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setIsSuggestedSelection(boolean isSuggested) {
        this.isSuggested = isSuggested;
    }
    
    public String getDisplayString() {
        if(displayOnlyName){
            return getTemplate().getName();
        }
        return super.getDisplayString();
    }
}
