/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.templates;

import java.io.IOException;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;


/**
 * Helper for getting the template store and registry.
 * 
 * @author Fabio
 */
public class TemplateHelper {

    /** The template store. */
    private static TemplateStore fStore;

    /** The context type registry. */
    private static ContributionContextTypeRegistry fRegistry;

    /** Key to store custom templates. */
    public static final String CUSTOM_TEMPLATES_PY_KEY = "org.python.pydev.editor.templates.PyTemplatePreferencesPage";

    /**
     * Returns this plug-in's template store.
     * 
     * @return the template store of this plug-in instance
     */
    public static TemplateStore getTemplateStore() {
        if (fStore == null) {
            fStore = new ContributionTemplateStore(TemplateHelper.getContextTypeRegistry(), PydevPlugin.getDefault()
                    .getPreferenceStore(), CUSTOM_TEMPLATES_PY_KEY);
            try {
                fStore.load();
            } catch (IOException e) {
                Log.log(e);
                throw new RuntimeException(e);
            }
        }
        return fStore;
    }

    /**
     * Returns this plug-in's context type registry.
     * 
     * @return the context type registry for this plug-in instance
     */
    public static ContextTypeRegistry getContextTypeRegistry() {
        if (fRegistry == null) {
            // create an configure the contexts available in the template editor
            fRegistry = new ContributionContextTypeRegistry();
            fRegistry.addContextType(PyContextType.PY_COMPLETIONS_CONTEXT_TYPE);
            fRegistry.addContextType(PyContextType.PY_MODULES_CONTEXT_TYPE);
        }
        return fRegistry;
    }

    /**
     * Used from jython scripts.
     */
    public static void clearTemplateRegistryCache() {
        fRegistry = null;
    }

}
