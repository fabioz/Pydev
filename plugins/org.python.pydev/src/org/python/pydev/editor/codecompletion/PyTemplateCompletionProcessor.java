package org.python.pydev.editor.codecompletion;

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
import org.python.pydev.core.IPyTemplateCompletionProcessor;
import org.python.pydev.editor.codecompletion.templates.PyDocumentTemplateContext;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.editor.templates.TemplateHelper;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class PyTemplateCompletionProcessor extends TemplateCompletionProcessor
        implements IPyTemplateCompletionProcessor {

    private String currentContext;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
     */
    @Override
    protected Template[] getTemplates(String contextTypeId) {
        return TemplateHelper.getTemplateStore().getTemplates();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    @Override
    protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
        return TemplateHelper.getContextTypeRegistry().getContextType(this.currentContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
     */
    @Override
    protected Image getImage(Template template) {
        return ImageCache.asImage(SharedUiPlugin.getImageCache().get(UIConstants.COMPLETION_TEMPLATE));
    }

    /**
     * @param viewer
     * @param documentOffset
     * @param propList
     *
     */
    @Override
    public void addTemplateProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> propList) {
        IDocument doc = viewer.getDocument();
        if (doc.getLength() == 0) {
            this.currentContext = PyContextType.PY_MODULES_CONTEXT_TYPE;

        } else {
            this.currentContext = PyContextType.PY_COMPLETIONS_CONTEXT_TYPE;

        }

        String str = extractPrefix(viewer, documentOffset);

        ICompletionProposal[] templateProposals = computeCompletionProposals(viewer, documentOffset);

        for (int j = 0; j < templateProposals.length; j++) {
            if (templateProposals[j].getDisplayString().startsWith(str)) {
                propList.add(templateProposals[j]);
            }
        }

    }

    /**
     * Overridden so that we can do the indentation in this case.
     */
    @Override
    protected TemplateContext createContext(final ITextViewer viewer, final IRegion region) {
        TemplateContextType contextType = getContextType(viewer, region);
        return PyDocumentTemplateContext.createContext(contextType, viewer, region);
    }

}
