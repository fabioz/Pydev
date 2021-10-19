package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

public class PyTemplateProposal extends TemplateProposal implements ICompletionProposalHandle {

    public PyTemplateProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance) {
        super(template, context, region, image, relevance);
    }

    @Override
    public Object getElement() {
        return null;
    }
}
