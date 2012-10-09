/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.completions.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class DjTemplateVariableResolver extends TemplateVariableResolver {

    private String[] options;

    /*
     * @see TemplateVariableResolver#TemplateVariableResolver(String, String)
     */
    protected DjTemplateVariableResolver(String type, String description, String[] options) {
        super(type, description);
        this.options = options;
    }

    protected String[] resolveAll(TemplateContext context) {
        return options;
    }

    /**
     * Returns always <code>true</code>, since simple variables are normally
     * unambiguous.
     *
     * @param context {@inheritDoc}
     * @return <code>true</code>
     */
    protected boolean isUnambiguous(TemplateContext context) {
        return true;
    }
}