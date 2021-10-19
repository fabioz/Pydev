package org.python.pydev.editor.codecompletion.proposals;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

public class PyTemplateProposalForTests extends TemplateProposal implements ICompletionProposalHandle {

    public PyTemplateProposalForTests(Template template, TemplateContext context, IRegion region, Image image,
            int relevance) {
        super(template, context, region, image, relevance);
    }

    @Override
    public String getDisplayString() {
        if (SharedCorePlugin.inTestMode()) {
            return this.getPrefixCompletionText(null, 0).toString();
        } else {
            return super.getDisplayString();
        }
    }

    @Override
    public Object getElement() {
        throw new UnsupportedOperationException();
    }

}
