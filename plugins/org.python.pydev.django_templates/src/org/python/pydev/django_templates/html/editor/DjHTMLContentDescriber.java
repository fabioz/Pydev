package org.python.pydev.django_templates.html.editor;

import org.python.pydev.django_templates.common.DjContentDescriber;

public class DjHTMLContentDescriber extends DjContentDescriber {

    private static final String HTML_PREFIX = "<!DOCTYPE HTML"; //$NON-NLS-1$

    @Override
    protected String getPrefix() {
        return HTML_PREFIX;
    }
}
