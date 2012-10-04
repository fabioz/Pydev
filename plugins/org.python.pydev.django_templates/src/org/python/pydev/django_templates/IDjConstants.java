/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates;

import com.aptana.editor.css.ICSSConstants;
import com.aptana.editor.html.IHTMLConstants;

/**
 * @author Fabio Zadrozny
 */
public interface IDjConstants {

    //It's important to start with the html content type, otherwise the code formatter doesn't work as expected!
    //i.e.: com.aptana.editor.common.CommonSourceViewerConfiguration.getContentFormatter(ISourceViewer) expects
    //the type to start with the html content type prefix!

    //com.aptana.contenttype.html.django_templates
    public String CONTENT_TYPE_DJANGO_HTML = IHTMLConstants.CONTENT_TYPE_HTML + ".django_templates"; //$NON-NLS-1$

    //com.aptana.contenttype.css.django_templates
    public String CONTENT_TYPE_DJANGO_CSS = ICSSConstants.CONTENT_TYPE_CSS + ".django_templates"; //$NON-NLS-1$

    /**
     * Scope names for django templates scopes.
     */
    public static final String EMBEDDED_CSS_SCOPE = "source.css.embedded.html"; //$NON-NLS-1$
    public static final String EMBEDDED_JS_SCOPE = "source.js.embedded.html"; //$NON-NLS-1$
    public static final String EMBEDDED_DJANGO_TEMPLATES_HTML_SCOPE = "source.django_templates.embedded.html"; //$NON-NLS-1$
    public static final String EMBEDDED_DJANGO_TEMPLATES_CSS_SCOPE = "source.django_templates.embedded.css"; //$NON-NLS-1$

    public static final String TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE = "text.html.django_templates"; //$NON-NLS-1$
    public static final String TOPLEVEL_DJANGO_TEMPLATES_CSS_SCOPE = "text.css.django_templates"; //$NON-NLS-1$
    public static final String EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE = "source.django_templates.embedded.tag.html"; //$NON-NLS-1$

}
