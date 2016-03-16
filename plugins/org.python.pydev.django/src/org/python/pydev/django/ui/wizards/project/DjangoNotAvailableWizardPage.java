/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.ui.wizards.project;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;

public class DjangoNotAvailableWizardPage extends WizardPage {

    public DjangoNotAvailableWizardPage(String pageName, IInterpreterInfo interpreterInfo) {
        super(pageName);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setFont(parent.getFont());

        LinkFieldEditor colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED",
                "To get started with Django in Pydev, a pre-requisite is that Django is \n"
                        + "installed in the Python / Jython / IronPython interpreter you want to use \n"
                        + "(so, \"import django\" must properly work). \n" + "\n"
                        + "It seems that the selected interpreter does not have Django available, so, please\n"
                        + "install Django, reconfigure the interpreter so that Django is recognized\n"
                        + "and then come back to this wizard.\n" + "\n"
                        + "An introduction on how to get started with Django in Pydev is available at:\n"
                        + "<a>http://pydev.org/manual_adv_django.html</a>.\n", composite, new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Program.launch("http://pydev.org/manual_adv_django.html");
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(composite);

        setErrorMessage("Django not found.");
        setControl(composite);
    }

}
