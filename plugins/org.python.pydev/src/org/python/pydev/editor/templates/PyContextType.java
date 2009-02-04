/*
 * Created on Aug 6, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.templates;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

/**
 * @author Fabio Zadrozny
 */
public class PyContextType extends TemplateContextType {

    /**
     * Context type used for code-completions
     */
    public static final String PY_COMPLETIONS_CONTEXT_TYPE = "org.python.pydev.editor.templates.python";
    
    /**
     * Context type used for new modules (wizard)
     */
    public static final String PY_MODULES_CONTEXT_TYPE = "org.python.pydev.editor.templates.python.modules";

    /**
     * Creates a new XML context type. 
     */
    public PyContextType() {
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
