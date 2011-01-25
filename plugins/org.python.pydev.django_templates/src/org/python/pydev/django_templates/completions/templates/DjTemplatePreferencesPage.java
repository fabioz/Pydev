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

import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.python.pydev.django_templates.DjPlugin;

/**
 * @author Fabio Zadrozny
 */
public class DjTemplatePreferencesPage extends TemplatePreferencePage implements IWorkbenchPreferencePage {

    public DjTemplatePreferencesPage() {
        setPreferenceStore(DjPlugin.getDefault().getPreferenceStore());
        setTemplateStore(TemplateHelper.getTemplateStore());
        setContextTypeRegistry(TemplateHelper.getContextTypeRegistry());
        setDescription("Templates for editor and new modules");
    }

    protected boolean isShowFormatterSetting() {
        return false;
    }

    public boolean performOk() {
        boolean ok = super.performOk();

        DjPlugin.getDefault().savePluginPreferences();

        return ok;
    }

}
