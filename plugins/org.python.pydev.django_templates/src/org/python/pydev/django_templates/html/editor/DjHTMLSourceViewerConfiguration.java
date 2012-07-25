/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.html.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.common.DjDoubleClickStrategy;
import org.python.pydev.django_templates.completions.DjContentAssistProcessor;
import org.python.pydev.django_templates.editor.DjPartitionerSwitchStrategy;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.CompositeSourceViewerConfiguration;
import com.aptana.editor.common.IPartitionerSwitchStrategy;
import com.aptana.editor.common.scripting.IContentTypeTranslator;
import com.aptana.editor.common.scripting.QualifiedContentType;
import com.aptana.editor.common.text.RubyRegexpAutoIndentStrategy;
import com.aptana.editor.common.text.rules.CompositePartitionScanner;
import com.aptana.editor.css.ICSSConstants;
import com.aptana.editor.html.HTMLPlugin;
import com.aptana.editor.html.HTMLSourceConfiguration;
import com.aptana.editor.html.IHTMLConstants;
import com.aptana.editor.js.IJSConstants;

/**
 * @author Fabio Zadrozny
 */
public class DjHTMLSourceViewerConfiguration extends CompositeSourceViewerConfiguration implements IDjConstants {

    static {
        IContentTypeTranslator c = CommonEditorPlugin.getDefault().getContentTypeTranslator();
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML), new QualifiedContentType(
                TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML,
                CompositePartitionScanner.START_SWITCH_TAG), new QualifiedContentType(
                TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML,
                CompositePartitionScanner.END_SWITCH_TAG), new QualifiedContentType(
                TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE));

        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML,
                IHTMLConstants.CONTENT_TYPE_HTML), new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE));
        c.addTranslation(
                new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, ICSSConstants.CONTENT_TYPE_CSS),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_CSS_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, IJSConstants.CONTENT_TYPE_JS),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_JS_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML,
                IDjConstants.CONTENT_TYPE_DJANGO_HTML), new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE,
                EMBEDDED_DJANGO_TEMPLATES_HTML_SCOPE));
    }

    private Map<String, DjDoubleClickStrategy> fDoubleClickStrategy = new HashMap<String, DjDoubleClickStrategy>();

    protected DjHTMLSourceViewerConfiguration(IPreferenceStore preferences, AbstractThemeableEditor editor) {
        super(HTMLSourceConfiguration.getDefault(), DjHtmlSourceConfiguration.getDefault(), preferences, editor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aptana.editor.common.CompositeSourceViewerConfiguration#getTopContentType
     * ()
     */
    @Override
    protected String getTopContentType() {
        return IDjConstants.CONTENT_TYPE_DJANGO_HTML;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aptana.editor.common.CompositeSourceViewerConfiguration#
     * getLanguageSpecification()
     */
    @Override
    protected IPartitionerSwitchStrategy getPartitionerSwitchStrategy() {
        return DjPartitionerSwitchStrategy.getDefault();
    }

    protected String getStartEndTokenType() {
        return "punctuation.section.embedded.dj"; //$NON-NLS-1$
    }

    @Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
        DjDoubleClickStrategy strategy = fDoubleClickStrategy.get(contentType);
        if (strategy == null) {
            strategy = new DjDoubleClickStrategy(contentType);
            fDoubleClickStrategy.put(contentType, strategy);
        }
        return strategy;
    }

    @Override
    protected IContentAssistProcessor getContentAssistProcessor(ISourceViewer sourceViewer, String contentType) {
        if (DjHtmlSourceConfiguration.DEFAULT.equals(contentType)) {
            return DjHtmlSourceConfiguration.getDefault().getContentAssistProcessor(getEditor(), contentType);
        }
        IContentAssistProcessor htmlContentAssistProcessor = HTMLSourceConfiguration.getDefault()
                .getContentAssistProcessor(getEditor(), contentType);
        if (HTMLSourceConfiguration.DEFAULT.equals(contentType) || IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
            return new DjContentAssistProcessor(contentType, htmlContentAssistProcessor);
        }
        return htmlContentAssistProcessor;
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.editor.common.CommonSourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
     */
    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        //Same as: HTMLSourceViewerConfiguration.getAutoEditStrategies
        return new IAutoEditStrategy[] { new RubyRegexpAutoIndentStrategy(contentType, this, sourceViewer, HTMLPlugin
                .getDefault().getPreferenceStore()) };
    }
}
