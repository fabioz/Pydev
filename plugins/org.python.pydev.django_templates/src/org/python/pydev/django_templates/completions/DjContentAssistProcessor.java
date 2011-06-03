/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.completions;

import java.util.ArrayList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.python.pydev.django_templates.completions.templates.DjContextType;
import org.python.pydev.django_templates.completions.templates.DjTemplateCompletionProcessor;
import org.python.pydev.django_templates.editor.DjSourceConfiguration;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import com.aptana.editor.common.contentassist.ICommonContentAssistProcessor;
import com.aptana.editor.css.CSSSourceConfiguration;
import com.aptana.editor.css.contentassist.CSSContentAssistProcessor;
import com.aptana.editor.html.HTMLSourceConfiguration;

public class DjContentAssistProcessor implements IContentAssistProcessor, ICommonContentAssistProcessor {

    private IContentAssistProcessor htmlContentAssistProcessor;
    private DjTemplateCompletionProcessor templatesContentAssistProcessor;
    private DjTemplateCompletionProcessor templatesTagsContentAssistProcessor;
    private DjTemplateCompletionProcessor templatesFiltersContentAssistProcessor;
    private String contentType;
    private final boolean isDefaultContentType;

    public DjContentAssistProcessor(String contentType, IContentAssistProcessor htmlContentAssistProcessor) {
        this.contentType = contentType;
        this.isDefaultContentType = this.contentType.equals(IDocument.DEFAULT_CONTENT_TYPE);
        this.htmlContentAssistProcessor = htmlContentAssistProcessor;
    }

    private DjTemplateCompletionProcessor getTemplatesContentAssistProcessor() {
        if (this.templatesContentAssistProcessor == null) {
            this.templatesContentAssistProcessor = new DjTemplateCompletionProcessor(
                    DjContextType.DJ_COMPLETIONS_CONTEXT_TYPE, 
                    PydevPlugin.getImageCache().get(UIConstants.COMPLETION_TEMPLATE), 
                    false);
        }
        return this.templatesContentAssistProcessor;
    }

    private DjTemplateCompletionProcessor getTemplatesTagsContentAssistProcessor() {
        if (this.templatesTagsContentAssistProcessor == null) {
            this.templatesTagsContentAssistProcessor = new DjTemplateCompletionProcessor(
                    DjContextType.DJ_TAGS_COMPLETIONS_CONTEXT_TYPE, null, true);
        }
        return this.templatesTagsContentAssistProcessor;
    }

    private DjTemplateCompletionProcessor getTemplatesFiltersContentAssistProcessor() {
        if (this.templatesFiltersContentAssistProcessor == null) {
            this.templatesFiltersContentAssistProcessor = new DjTemplateCompletionProcessor(
                    DjContextType.DJ_FILTERS_COMPLETIONS_CONTEXT_TYPE, null, true);
        }
        return this.templatesFiltersContentAssistProcessor;
    }

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        ICompletionProposal[] proposals = null;
        if (this.htmlContentAssistProcessor != null) {
            proposals = this.htmlContentAssistProcessor.computeCompletionProposals(viewer, offset);
        }

        return addDjProposals(viewer, offset, proposals);
    }

    /**
     * Heuristically extracts the prefix used for determining template relevance
     * from the viewer's document. The default implementation returns the String from
     * offset backwards that forms a java identifier.
     *
     * @param viewer the viewer
     * @param offset offset into document
     * @return the prefix to consider
     * @see #getRelevance(Template, String)
     */
    public String extractPrefix(ITextViewer viewer, int offset) {
        IDocument document = viewer.getDocument();
        return extractPrefix(document, offset);
    }

    public String extractPrefix(IDocument document, int offset) {
        int i = offset;
        if (i > document.getLength())
            return ""; //$NON-NLS-1$

        try {
            while (i > 0) {
                char ch = document.getChar(i - 1);
                if (!Character.isJavaIdentifierPart(ch) && ch != '|') {
                    break;
                }
                i--;
                if (ch == '|') {
                    break; //We also want to add the | to the prefix.
                }
            }

            return document.get(i, offset - i);
        } catch (BadLocationException e) {
            return ""; //$NON-NLS-1$
        }
    }

    private ICompletionProposal[] addDjProposals(ITextViewer viewer, int offset, ICompletionProposal[] proposals) {

        boolean completionsForTags = showCompletionsInsideDjangoContext(viewer.getDocument(), offset);

        ICompletionProposal[] djProposals;
        DjTemplateCompletionProcessor processor;
        String str = extractPrefix(viewer, offset);

        if (completionsForTags) {
            if (str.startsWith("|")) {
                processor = this.getTemplatesFiltersContentAssistProcessor();
                str = str.substring(1);
            } else {
                processor = this.getTemplatesTagsContentAssistProcessor();
            }
        } else {
            processor = this.getTemplatesContentAssistProcessor();
        }

        djProposals = processor.computeCompletionProposals(viewer, offset);

        ArrayList<ICompletionProposal> djProposalsList = new ArrayList<ICompletionProposal>();
        for (int j = 0; j < djProposals.length; j++) {
            if (djProposals[j].getDisplayString().startsWith(str)) {
                ICompletionProposal p = djProposals[j];
                djProposalsList.add(p);
            }
        }

        if (proposals != null && proposals.length > 0) {
            for (int i = 0; i < proposals.length; i++) {
                if (proposals[i].getDisplayString().startsWith(str)) {
                    ICompletionProposal p = proposals[i];
                    djProposalsList.add(p);
                }
            }
        }
        return djProposalsList.toArray(new ICompletionProposal[djProposalsList.size()]);
    }

    public boolean showCompletionsInsideDjangoContext(IDocument document, int offset) {
        //Content type we're at:
        //Request on __html__dftl_partition_content_type: templates in the html level
        //Request on __dftl_partition_content_type:  |{%| euo a=|"test"| nthnh  |%|}|
        //Request on __djhtml__dftl_partition_content_type: at {% |roto| %}

        //So, we use a simple heuristic to know what we should use:
        //1. if it's at __djhtml__dftl_partition_content_type, we know exactly where we are (inside a django tag)
        //2. If we're at __html__dftl_partition_content_type, we also know we should only get completions that are top-level
        //3. __dftl_partition_content_type is the 'gray' area, so, we go backwards to discover where we are (looking for 
        //the first occurrence of __djhtml__dftl_partition_content_type or __html__dftl_partition_content_type or the 
        //sequence {% (starting, meaning inside django tag) or %} (ending, meaning outside django tag.) 

        boolean completionsForTags = true;
        if (DjSourceConfiguration.DEFAULT.equals(this.contentType)) {
            completionsForTags = true;

        } else if (HTMLSourceConfiguration.DEFAULT.equals(this.contentType) || CSSSourceConfiguration.DEFAULT.equals(this.contentType)) {
            completionsForTags = false;

        } else if (IDocument.DEFAULT_CONTENT_TYPE.equals(this.contentType)) {
            try {
                int discoverOffset = offset;
                while (discoverOffset >= 0) {
                    String cont = document.getContentType(discoverOffset);
                    discoverOffset--;
                    if (DjSourceConfiguration.DEFAULT.equals(cont)) {
                        completionsForTags = true;
                        break;

                    } else if (HTMLSourceConfiguration.DEFAULT.equals(cont) || CSSSourceConfiguration.DEFAULT.equals(cont)) {
                        completionsForTags = false;
                        break;
                    }
                }

            } catch (BadLocationException e) {
                completionsForTags = true; //we got to the end and didn't find the scope, so, just go on and show the 'simple' ones.
            }
        }
        return completionsForTags;
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        if(htmlContentAssistProcessor != null){
            return htmlContentAssistProcessor.getCompletionProposalAutoActivationCharacters();
        }
        return null;
    }

    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    public String getErrorMessage() {
        return null;
    }

    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset, char activationChar, boolean autoActivated) {
        ICompletionProposal[] proposals = null;
        boolean completeDj = true;
        if(isDefaultContentType){
            try {
                int tempOffset = offset;
                while(tempOffset >= 0){
                    tempOffset--;
                    char prevChar = viewer.getDocument().getChar(tempOffset);
                    if(prevChar == '<'){
                        completeDj = false;
                        break;
                    }
                    if(!Character.isJavaIdentifierPart(prevChar)){
                        break;
                    }
                }
            } catch (BadLocationException e) {
            }
        }
        if (this.htmlContentAssistProcessor instanceof ICommonContentAssistProcessor) {
            ICommonContentAssistProcessor commonContentAssistProcessor = (ICommonContentAssistProcessor) this.htmlContentAssistProcessor;
            proposals = commonContentAssistProcessor.computeCompletionProposals(viewer, offset, activationChar, autoActivated);
        } else if(this.htmlContentAssistProcessor != null){
            proposals = this.htmlContentAssistProcessor.computeCompletionProposals(viewer, offset);
        }
        
        //css and django templates 'compete' in the same namespace, so, we have to add an exception in the check below...
        if(!(this.htmlContentAssistProcessor instanceof CSSContentAssistProcessor) && proposals != null && proposals.length > 0){
            completeDj = false;
        }

        if(completeDj){
            return addDjProposals(viewer, offset, proposals);
        }
        return proposals;
    }
    

    /* (non-Javadoc)
     * @see com.aptana.editor.common.contentassist.ICommonContentAssistProcessor#isValidAutoActivationLocation(char, int, org.eclipse.jface.text.IDocument, int)
     */
    public boolean isValidAutoActivationLocation(char c, int keyCode, IDocument document, int offset) {
        if(htmlContentAssistProcessor instanceof ICommonContentAssistProcessor){
            return ((ICommonContentAssistProcessor)htmlContentAssistProcessor).isValidAutoActivationLocation(c, keyCode, document, offset);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.aptana.editor.common.contentassist.ICommonContentAssistProcessor#isValidIdentifier(char, int)
     */
    public boolean isValidIdentifier(char c, int keyCode) {
        if(htmlContentAssistProcessor instanceof ICommonContentAssistProcessor){
            return ((ICommonContentAssistProcessor)htmlContentAssistProcessor).isValidIdentifier(c, keyCode);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.aptana.editor.common.contentassist.ICommonContentAssistProcessor#isValidActivationCharacter(char, int)
     */
    public boolean isValidActivationCharacter(char c, int keyCode) {
        if(htmlContentAssistProcessor instanceof ICommonContentAssistProcessor){
            return ((ICommonContentAssistProcessor)htmlContentAssistProcessor).isValidActivationCharacter(c, keyCode);
        }
        return false;
    }

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.contentassist.ICommonContentAssistProcessor#dispose()
	 */
	public void dispose()
	{
	}

}
