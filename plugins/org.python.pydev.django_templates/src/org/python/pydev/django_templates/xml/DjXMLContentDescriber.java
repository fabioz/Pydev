package org.python.pydev.django_templates.xml;

import org.python.pydev.django_templates.common.DjContentDescriber;

public class DjXMLContentDescriber extends DjContentDescriber {

    private static final String XML_PREFIX = "<?xml "; //$NON-NLS-1$

    @Override
    protected String getPrefix() {
        return XML_PREFIX;
    }
}
