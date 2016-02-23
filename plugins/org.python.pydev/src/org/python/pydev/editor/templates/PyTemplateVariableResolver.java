/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class PyTemplateVariableResolver extends TemplateVariableResolver {

    /*
     * @see TemplateVariableResolver#TemplateVariableResolver(String, String)
     */
    protected PyTemplateVariableResolver(String type, String description) {
        super(type, description);
    }

    /**
     * Returns always <code>true</code>, since simple variables are normally
     * unambiguous.
     *
     * @param context {@inheritDoc}
     * @return <code>true</code>
     */
    @Override
    protected boolean isUnambiguous(TemplateContext context) {
        return true;
    }
}