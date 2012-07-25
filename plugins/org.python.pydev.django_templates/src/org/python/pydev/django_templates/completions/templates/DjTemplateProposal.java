/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.completions.templates;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;

import com.aptana.editor.common.contentassist.ICommonCompletionProposal;

public class DjTemplateProposal extends TemplateProposal implements ICommonCompletionProposal {

    private boolean isDefault;
    private boolean isSuggested;
    private boolean displayOnlyName;
    private int relevance;

    public DjTemplateProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance,
            boolean displayOnlyName) {
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
        if (displayOnlyName) {
            return getTemplate().getName();
        }
        return super.getDisplayString();
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ICompletionProposal o) {
        if (this == o) {
            return 0;
        }

        // not yet sorting on relevance
        String s1 = this.getDisplayString();
        String s2 = o.getDisplayString();
        if (s1 == null) {
            s1 = "";
        }
        if (s2 == null) {
            s2 = "";
        }
        return s1.compareToIgnoreCase(s2);

    }

    public int getRelevance() {
        return this.relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.editor.common.contentassist.ICommonCompletionProposal#validateTrigger(org.eclipse.jface.text.IDocument, int, org.eclipse.swt.events.KeyEvent)
     */
    public boolean validateTrigger(IDocument document, int offset, KeyEvent keyEvent) {
        return true;
    }
}
