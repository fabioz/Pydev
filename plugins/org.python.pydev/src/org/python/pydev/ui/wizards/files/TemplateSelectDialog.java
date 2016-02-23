/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.wizards.files;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.editor.templates.TemplateHelper;
import org.python.pydev.plugin.PydevPlugin;

public class TemplateSelectDialog extends SelectionDialog {

    private Label label;
    private Table templateList;
    private TreeMap<String, TemplatePersistenceData> nameToTemplateData;
    private IDialogSettings fDialogSettings;

    protected TemplateSelectDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite topLevel = (Composite) super.createDialogArea(parent);
        createTemplateOptions(topLevel);
        return topLevel;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control ret = super.createContents(parent);
        org.python.pydev.plugin.PydevPlugin.setCssId(parent, "py-template-select-dialog", true);
        return ret;
    }

    private void createTemplateOptions(Composite topLevel) {
        final TemplateStore templateStore = TemplateHelper.getTemplateStore();
        if (templateStore != null) {
            TemplatePersistenceData[] templateData = templateStore.getTemplateData(false);
            if (templateData != null && templateData.length > 0) {
                //create the template selection
                label = new Label(topLevel, SWT.NONE);
                label.setText("Template");

                templateList = new Table(topLevel, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
                fillTemplateOptions(templateData, templateList);

                templateList.addMouseListener(new MouseListener() {

                    @Override
                    public void mouseUp(MouseEvent e) {
                    }

                    @Override
                    public void mouseDown(MouseEvent e) {
                    }

                    @Override
                    public void mouseDoubleClick(MouseEvent e) {
                        okPressed();
                    }
                });

                Link link = new Link(topLevel, SWT.NONE);
                link.setText("<a>Config available templates...</a>");

                link.addSelectionListener(new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null,
                                "org.python.pydev.prefs.template", null, null);
                        dialog.open();
                        //Fill it after having the settings edited.
                        TemplatePersistenceData[] templateData = templateStore.getTemplateData(false);
                        if (templateData != null && templateData.length > 0) {
                            fillTemplateOptions(templateData, templateList);
                        } else {
                            fillTemplateOptions(new TemplatePersistenceData[0], templateList);
                        }
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });

                setLayout(label, templateList, link);
            }
        }
    }

    private static final String DIALOG_HEIGHT = "DIALOG_HEIGHT"; //$NON-NLS-1$

    private static final String DIALOG_WIDTH = "DIALOG_WIDTH"; //$NON-NLS-1$

    private static final String DIALOG_BOUNDS_SETTINGS = "DialogBoundsSettings"; //$NON-NLS-1$

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Dialog#getDialogBoundsSettings()
     */
    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        IDialogSettings settings = getDialogSettings();
        IDialogSettings section = settings.getSection(DIALOG_BOUNDS_SETTINGS);
        if (section == null) {
            section = settings.addNewSection(DIALOG_BOUNDS_SETTINGS);
            section.put(DIALOG_HEIGHT, 500);
            section.put(DIALOG_WIDTH, 600);
        }
        return section;
    }

    /**
     * Returns the dialog settings. Returned object can't be null.
     * 
     * @return return dialog settings for this dialog
     */
    protected IDialogSettings getDialogSettings() {
        IDialogSettings settings = PydevPlugin.getDefault().getDialogSettings();
        fDialogSettings = settings.getSection(TemplateSelectDialog.class.getName());
        if (fDialogSettings == null) {
            fDialogSettings = settings.addNewSection(TemplateSelectDialog.class.getName());
        }
        return fDialogSettings;

    }

    /**
     * Sets the template options in the passed list (swt)
     */
    private void fillTemplateOptions(TemplatePersistenceData[] templateData, Table list) {
        nameToTemplateData = new TreeMap<String, TemplatePersistenceData>();

        for (TemplatePersistenceData data : templateData) {
            if (PyContextType.PY_MODULES_CONTEXT_TYPE.equals(data.getTemplate().getContextTypeId())) {
                String name = data.getTemplate().getName();
                nameToTemplateData.put(name, data);
            }
        }
        ArrayList<String> lst = new ArrayList<String>(nameToTemplateData.keySet());
        for (String string : lst) {
            new TableItem(list, SWT.NONE).setText(string);
        }
        list.setSelection(0);
    }

    @Override
    protected void okPressed() {
        TemplatePersistenceData selectedTemplate = getCurr();
        setResult(Arrays.asList(selectedTemplate));
        super.okPressed();
    }

    /**
     * @return the data for the selected template or null if not available.
     */
    private TemplatePersistenceData getCurr() {
        if (templateList != null && nameToTemplateData != null) {
            TableItem[] sel = templateList.getSelection();
            if (sel != null && sel.length > 0) {
                return nameToTemplateData.get(sel[0].getText(0));
            }
        }
        return null;
    }

    public TemplatePersistenceData getSelectedTemplate() {
        Object[] r = this.getResult();
        if (r != null && r.length > 0) {
            return (TemplatePersistenceData) r[0];
        }
        return null;
    }

    private void setLayout(Label label, Control control, Control link) {
        GridData data;

        if (label != null) {
            data = new GridData();
            data.grabExcessHorizontalSpace = false;
            label.setLayoutData(data);
        }

        if (control != null) {
            data = new GridData(GridData.FILL_BOTH);
            data.grabExcessHorizontalSpace = true;
            data.grabExcessVerticalSpace = true;
            control.setLayoutData(data);
        }

        if (link != null) {
            data = new GridData();
            link.setLayoutData(data);
        }
    }

}
