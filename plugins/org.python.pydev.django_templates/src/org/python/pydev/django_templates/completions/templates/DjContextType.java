/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author Fabio Zadrozny
 */
package org.python.pydev.django_templates.completions.templates;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

/**
 * @author Fabio Zadrozny
 */
public class DjContextType extends TemplateContextType {

    /**
     * Context type used for code-completions
     */
    public static final String DJ_COMPLETIONS_CONTEXT_TYPE = "org.python.pydev.django_templates.templatesContext";
    public static final String DJ_TAGS_COMPLETIONS_CONTEXT_TYPE = "org.python.pydev.django_templates.tagsTemplatesContext";
    public static final String DJ_FILTERS_COMPLETIONS_CONTEXT_TYPE = "org.python.pydev.django_templates.filtersTemplatesContext";

    /**
     * Creates a new XML context type. 
     */
    public DjContextType() {
        addGlobalResolvers();

    }

    private void addGlobalResolvers() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.LineSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
        addResolver(new DjTemplateVariableResolver("on_or_off", "Choose on or off", new String[] { "on", "off" }));
    }

}
