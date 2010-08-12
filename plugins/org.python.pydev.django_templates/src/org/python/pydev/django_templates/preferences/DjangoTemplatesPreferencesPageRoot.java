package org.python.pydev.django_templates.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class DjangoTemplatesPreferencesPageRoot extends PreferencePage implements IWorkbenchPreferencePage {

    public void init(IWorkbench workbench) {
        
    }

    protected Control createContents(Composite parent) {
        return parent;
    }

}
