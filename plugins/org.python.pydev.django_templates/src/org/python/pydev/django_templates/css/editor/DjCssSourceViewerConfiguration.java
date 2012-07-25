/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.django_templates.css.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
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
import com.aptana.editor.common.text.rules.CompositePartitionScanner;
import com.aptana.editor.css.CSSSourceConfiguration;
import com.aptana.editor.css.ICSSConstants;

/**
 * @author Fabio Zadrozny
 */
public class DjCssSourceViewerConfiguration extends CompositeSourceViewerConfiguration implements IDjConstants {

    static {
        IContentTypeTranslator c = CommonEditorPlugin.getDefault().getContentTypeTranslator();
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_CSS), new QualifiedContentType(
                TOPLEVEL_DJANGO_TEMPLATES_CSS_SCOPE));

        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_CSS,
                CompositePartitionScanner.START_SWITCH_TAG), new QualifiedContentType(
                TOPLEVEL_DJANGO_TEMPLATES_CSS_SCOPE, EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE));

        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_CSS,
                CompositePartitionScanner.END_SWITCH_TAG), new QualifiedContentType(
                TOPLEVEL_DJANGO_TEMPLATES_CSS_SCOPE, EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE));

        c.addTranslation(
                new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_CSS, ICSSConstants.CONTENT_TYPE_CSS),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_CSS_SCOPE));

        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_CSS,
                IDjConstants.CONTENT_TYPE_DJANGO_CSS), new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_CSS_SCOPE,
                EMBEDDED_DJANGO_TEMPLATES_CSS_SCOPE));
    }

    private Map<String, DjDoubleClickStrategy> fDoubleClickStrategy = new HashMap<String, DjDoubleClickStrategy>();

    protected DjCssSourceViewerConfiguration(IPreferenceStore preferences, AbstractThemeableEditor editor) {
        super(CSSSourceConfiguration.getDefault(), DjCssSourceConfiguration.getDefault(), preferences, editor);
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
        return IDjConstants.CONTENT_TYPE_DJANGO_CSS;
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
        if (DjCssSourceConfiguration.DEFAULT.equals(contentType)) {
            return DjCssSourceConfiguration.getDefault().getContentAssistProcessor(getEditor(), contentType);
        }
        //Note: The HTMLSourceViewerConfiguration should get the CSS content assist based on the content type. 
        IContentAssistProcessor cssContentAssistProcessor = CSSSourceConfiguration.getDefault()
                .getContentAssistProcessor(getEditor(), contentType);
        if (CSSSourceConfiguration.DEFAULT.equals(contentType) || IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
            return new DjContentAssistProcessor(contentType, cssContentAssistProcessor);
        }
        return cssContentAssistProcessor;
    }
}
