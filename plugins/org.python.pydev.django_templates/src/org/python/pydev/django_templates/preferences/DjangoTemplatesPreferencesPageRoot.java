/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.utils.LinkFieldEditor;

public class DjangoTemplatesPreferencesPageRoot extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public void init(IWorkbench workbench) {
        setDescription("Django Templates Editor");
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        LinkFieldEditor prefs = new LinkFieldEditor("PREF_TO_IGNORE_0",
                "\nColors may be changed through the <a>themes</a>.", p, new SelectionListener() {

                    public void widgetSelected(SelectionEvent e) {
                        String id = "com.aptana.theme.preferencePage";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        addField(prefs);

        prefs = new LinkFieldEditor("PREF_TO_IGNORE_0",
                "\nKeywords colored are defined through the\n<a>templates with the context 'Django tags'</a>.", p,
                new SelectionListener() {

                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.python.pydev.django_templates.templates";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        addField(prefs);

    }

}
