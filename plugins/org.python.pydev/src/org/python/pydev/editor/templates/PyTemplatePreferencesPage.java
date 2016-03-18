/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Aug 6, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.templates;

import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyTemplatePreferencesPage extends TemplatePreferencePage implements IWorkbenchPreferencePage {

    public PyTemplatePreferencesPage() {
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setTemplateStore(TemplateHelper.getTemplateStore());
        setContextTypeRegistry(TemplateHelper.getContextTypeRegistry());
        setDescription("Templates for editor and new modules");
    }

    @Override
    protected boolean isShowFormatterSetting() {
        return true;
    }

    @Override
    public boolean performOk() {
        boolean ok = super.performOk();

        PydevPlugin.getDefault().savePluginPreferences();

        return ok;
    }

}
