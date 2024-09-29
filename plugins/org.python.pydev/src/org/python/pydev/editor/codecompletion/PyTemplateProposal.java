package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

import com.python.pydev.analysis.refactoring.tdd.TemplateInfo;

public class PyTemplateProposal extends TemplateProposal implements ICompletionProposalHandle {

    public PyTemplateProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance) {
        super(template, context, region, image, relevance);
    }

    public TemplateInfo getAsTemplateInfo() {
        IRegion region = new Region(getReplaceOffset(), getReplaceEndOffset() - getReplaceOffset());
        return new TemplateInfo(getTemplate(), getContext(), region);
    }
}
