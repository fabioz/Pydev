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
		setTemplateStore(PydevPlugin.getDefault().getTemplateStore());
		setContextTypeRegistry(PydevPlugin.getDefault().getContextTypeRegistry());
	}

	protected boolean isShowFormatterSetting() {
		return true;
	}
	
	
	public boolean performOk() {
		boolean ok= super.performOk();
		
		PydevPlugin.getDefault().savePluginPreferences();
		
		return ok;
	}


}
