/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.django_templates.html.editor;

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
import org.python.pydev.django_templates.editor.DjSourceConfiguration;
import org.python.pydev.django_templates.editor.DjPartitionerSwitchStrategy;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.CompositeSourceViewerConfiguration;
import com.aptana.editor.common.IPartitionerSwitchStrategy;
import com.aptana.editor.common.scripting.IContentTypeTranslator;
import com.aptana.editor.common.scripting.QualifiedContentType;
import com.aptana.editor.common.text.rules.CompositePartitionScanner;
import com.aptana.editor.css.ICSSConstants;
import com.aptana.editor.html.HTMLSourceConfiguration;
import com.aptana.editor.html.HTMLSourceViewerConfiguration;
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
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, CompositePartitionScanner.START_SWITCH_TAG),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, CompositePartitionScanner.END_SWITCH_TAG),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE));

        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, IHTMLConstants.CONTENT_TYPE_HTML),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, ICSSConstants.CONTENT_TYPE_CSS),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_CSS_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, IJSConstants.CONTENT_TYPE_JS),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_JS_SCOPE));
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML, IDjConstants.CONTENT_TYPE_DJANGO_HTML),
                new QualifiedContentType(TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE, EMBEDDED_DJANGO_TEMPLATES_SCOPE));
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
        return "punctuation.section.embedded.djhtml"; //$NON-NLS-1$
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
        if(DjSourceConfiguration.DEFAULT.equals(contentType) || IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)){
            return new DjContentAssistProcessor(contentType, null);
        }
        AbstractThemeableEditor editor = getAbstractThemeableEditor();
        IContentAssistProcessor htmlContentAssistProcessor = HTMLSourceViewerConfiguration.getContentAssistProcessor(contentType, editor);
        if("__html__dftl_partition_content_type".equals(contentType)){
            return new DjContentAssistProcessor(contentType, htmlContentAssistProcessor);
        }
        return htmlContentAssistProcessor;
    }
}
