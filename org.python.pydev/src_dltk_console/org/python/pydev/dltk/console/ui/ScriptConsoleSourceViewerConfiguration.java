/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;

/**
 * Configuration for the source viewer.
 */
public class ScriptConsoleSourceViewerConfiguration extends SourceViewerConfiguration {
    
    public static final String PARTITION_TYPE = IDocument.DEFAULT_CONTENT_TYPE;

    private ITextHover hover;

    private IContentAssistant contentAssist;

    private IQuickAssistAssistant quickAssist;

    public ScriptConsoleSourceViewerConfiguration(ITextHover hover, IContentAssistant contentAssist, IQuickAssistAssistant quickAssist) {
        this.hover = hover;
        this.contentAssist = contentAssist;
        this.quickAssist = quickAssist;
    }

    public int getTabWidth(ISourceViewer sourceViewer) {
        return DefaultIndentPrefs.getStaticTabWidth();
    }

    public ITextHover getTextHover(ISourceViewer sv, String contentType) {
        return hover;
    }

    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] { PARTITION_TYPE };
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        return contentAssist;
    }
    
    @Override
    public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        return quickAssist;
    }
}
