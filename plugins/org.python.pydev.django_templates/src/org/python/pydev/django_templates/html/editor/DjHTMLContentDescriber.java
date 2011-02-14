/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.html.editor;

import org.python.pydev.django_templates.common.DjContentDescriber;

public class DjHTMLContentDescriber extends DjContentDescriber {

    private static final String HTML_PREFIX = "<!DOCTYPE HTML"; //$NON-NLS-1$

    @Override
    protected String getPrefix() {
        return HTML_PREFIX;
    }
}
