/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
