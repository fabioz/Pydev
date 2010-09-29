/**
 * Copyright (c) 2010 Aptana, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.python.pydev.django_templates;

/**
 * @author Fabio Zadrozny
 */
public interface IDjConstants {

    public String CONTENT_TYPE_DJANGO_HTML = "org.python.pydev.contenttype.django_templates.html"; //$NON-NLS-1$
    public String CONTENT_TYPE_DJANGO_CSS = "org.python.pydev.contenttype.django_templates.css"; //$NON-NLS-1$
    public String CONTENT_TYPE_DJANGO_XML = "org.python.pydev.contenttype.django_templates.xml"; //$NON-NLS-1$

    public String LANGUAGE_DJANGO_TEMPLATES_HTML = "text/django_templates_html"; //$NON-NLS-1$
    public String LANGUAGE_DJANGO_TEMPLATES_CSS = "text/django_templates_css"; //$NON-NLS-1$

    /**
     * Scope names for RHTML scopes.
     */
    public static final String EMBEDDED_CSS_SCOPE = "source.css.embedded.html"; //$NON-NLS-1$
    public static final String EMBEDDED_JS_SCOPE = "source.js.embedded.html"; //$NON-NLS-1$
    public static final String EMBEDDED_DJANGO_TEMPLATES_HTML_SCOPE = "source.django_templates.embedded.html"; //$NON-NLS-1$
    public static final String EMBEDDED_DJANGO_TEMPLATES_CSS_SCOPE = "source.django_templates.embedded.css"; //$NON-NLS-1$
    
    public static final String TOPLEVEL_DJANGO_TEMPLATES_HTML_SCOPE = "text.html.django_templates"; //$NON-NLS-1$
    public static final String TOPLEVEL_DJANGO_TEMPLATES_CSS_SCOPE = "text.css.django_templates"; //$NON-NLS-1$
    public static final String EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE = "source.django_templates.embedded.tag.html"; //$NON-NLS-1$

    public static final String TOPLEVEL_DJANGO_TEMPLATES_XML_SCOPE = "text.xml.django_templates"; //$NON-NLS-1$
    public static final String EMBEDDED_DJANGO_TEMPLATES_TAG_SCOPE_XML = "source.django_templates.embedded.tag.xml"; //$NON-NLS-1$
}
