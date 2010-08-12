/*
 * Created on Aug 6, 2004
 *
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
    }
    



}
