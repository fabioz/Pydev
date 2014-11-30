/******************************************************************************
* Copyright (C) 2008-2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.codecompletion.PyContentAssistant;

/**
 * Configuration for the source viewer.
 */
public class PydevScriptConsoleSourceViewerConfiguration extends SourceViewerConfiguration {

    public static final String PARTITION_TYPE = IDocument.DEFAULT_CONTENT_TYPE;

    private ITextHover hover;

    private PyContentAssistant contentAssist;

    private IQuickAssistAssistant quickAssist;

    public PydevScriptConsoleSourceViewerConfiguration(ITextHover hover, PyContentAssistant contentAssist,
            IQuickAssistAssistant quickAssist) {
        this.hover = hover;
        this.contentAssist = contentAssist;
        this.quickAssist = quickAssist;
    }

    @Override
    public int getTabWidth(ISourceViewer sourceViewer) {
        IAdaptable adaptable = null;
        if (sourceViewer instanceof IAdaptable) {
            adaptable = (IAdaptable) sourceViewer;
        }
        return new DefaultIndentPrefs(adaptable).getTabWidth();
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sv, String contentType) {
        return hover;
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] { PARTITION_TYPE };
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        contentAssist.setInformationControlCreator(this.getInformationControlCreator(sourceViewer));
        return contentAssist;
    }

    @Override
    public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        quickAssist.setInformationControlCreator(this.getInformationControlCreator(sourceViewer));
        return quickAssist;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getInformationControlCreator(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
        return PyContentAssistant.createInformationControlCreator(sourceViewer);
    }

}
