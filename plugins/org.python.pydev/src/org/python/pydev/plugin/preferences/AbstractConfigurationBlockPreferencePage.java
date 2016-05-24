/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mark Leone - Modification for PyDev
 *******************************************************************************/
package org.python.pydev.plugin.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Abstract preference page which is used to wrap a
 * {@link org.python.pydev.plugin.preferences.IPreferenceConfigurationBlock}.
 *
 * @since 3.0
 */
public abstract class AbstractConfigurationBlockPreferencePage extends PreferencePage
        implements IWorkbenchPreferencePage {

    private IPreferenceConfigurationBlock fConfigurationBlock;

    /**
     * Creates a new preference page.
     */
    public AbstractConfigurationBlockPreferencePage() {
        setDescription();
        setPreferenceStore();
        fConfigurationBlock = createConfigurationBlock();
    }

    protected abstract IPreferenceConfigurationBlock createConfigurationBlock();

    protected abstract String getHelpId();

    protected abstract void setDescription();

    protected abstract void setPreferenceStore();

    /*
     * @see IWorkbenchPreferencePage#init()
     */
    public void init(IWorkbench workbench) {
    }

    /*
     * @see PreferencePage#createControl(Composite)
     */
    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpId());
    }

    /*
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {

        Control content = fConfigurationBlock.createControl(parent);

        initialize();

        Dialog.applyDialogFont(content);
        return content;
    }

    private void initialize() {
        fConfigurationBlock.initialize();
    }

    /*
     * @see PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {

        fConfigurationBlock.performOk();

        PydevPlugin.flushInstanceScope();

        return true;
    }

    @Override
    public boolean performCancel() {
        fConfigurationBlock.performCancel();
        return true;
    }

    /*
     * @see PreferencePage#performDefaults()
     */
    @Override
    public void performDefaults() {

        fConfigurationBlock.performDefaults();

        super.performDefaults();
    }

    /*
     * @see DialogPage#dispose()
     */
    @Override
    public void dispose() {

        fConfigurationBlock.dispose();

        super.dispose();
    }
}
