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
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.codecompletion.PyContentAssistant;

/**
 * Configuration for the source viewer.
 */
public class ScriptConsoleSourceViewerConfiguration extends SourceViewerConfiguration {
    
    public static final String PARTITION_TYPE = IDocument.DEFAULT_CONTENT_TYPE;

    private ITextHover hover;

    private PyContentAssistant ca;

    public ScriptConsoleSourceViewerConfiguration(ITextHover hover, PyContentAssistant ca) {
        this.hover = hover;
        this.ca = ca;
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

    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        return ca;
    }
}
