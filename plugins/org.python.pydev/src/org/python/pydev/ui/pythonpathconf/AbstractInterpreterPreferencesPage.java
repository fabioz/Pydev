/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 23, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.ast.interpreter_managers.AbstractInterpreterManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.ui.dialogs.PyDialogHelpers;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractInterpreterPreferencesPage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    protected AbstractInterpreterEditor pathEditor;
    private boolean inApply = false;

    /**
     * Initializer sets the preference store
     */
    public AbstractInterpreterPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        PyDialogHelpers.enableAskInterpreterStep(false);
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
    }

    protected abstract AbstractInterpreterEditor getInterpreterEditor(Composite p);

    public void setDefaultProjectLocation(String defaultProjectLocation) {
        Assert.isNotNull(this.pathEditor, "This method may only be called after the pathEditor is already created.");
        this.pathEditor.setDefaultProjectLocation(defaultProjectLocation);
    }

    /**
     * Creates a dialog that'll choose from a list of interpreter infos.
     */
    public static SelectionDialog createChooseIntepreterInfoDialog(IWorkbenchWindow workbenchWindow,
            IInterpreterInfo[] interpreters, String msg, boolean selectMultiple) {

        IStructuredContentProvider contentProvider = new IStructuredContentProvider() {

            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof IInterpreterInfo[]) {
                    return (IInterpreterInfo[]) inputElement;
                }
                return new Object[0];
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

            }
        };

        LabelProvider labelProvider = new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                return ImageCache.asImage(SharedUiPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON));
            }

            @Override
            public String getText(Object element) {
                if (element != null && element instanceof IInterpreterInfo) {
                    IInterpreterInfo info = (IInterpreterInfo) element;
                    return info.getNameForUI();
                }
                return super.getText(element);
            }
        };

        SelectionDialog selectionDialog;
        if (selectMultiple) {
            selectionDialog = new ListSelectionDialog(workbenchWindow.getShell(), interpreters, contentProvider,
                    labelProvider, msg) {
                @Override
                protected Control createContents(Composite parent) {
                    Control ret = super.createContents(parent);
                    org.python.pydev.plugin.PydevPlugin.setCssId(parent, "py-select-dialog", true);
                    return ret;
                }
            };
        } else {

            ListDialog listDialog = new ListDialog(workbenchWindow.getShell()) {
                @Override
                protected Control createContents(Composite parent) {
                    Control ret = super.createContents(parent);
                    org.python.pydev.plugin.PydevPlugin.setCssId(parent, "py-select-dialog", true);
                    return ret;
                }
            };

            listDialog.setContentProvider(contentProvider);
            listDialog.setLabelProvider(labelProvider);
            listDialog.setInput(interpreters);
            listDialog.setMessage(msg);
            selectionDialog = listDialog;
        }
        return selectionDialog;
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    /**
     * Applies changes (if any)
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        this.inApply = true;
        try {
            super.performApply(); //calls performOk()
        } finally {
            this.inApply = false;
        }
    }

    /**
     * Restores the default values
     *
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        //don't do anything on defaults...
    }

    /**
     * Cancels any change
     *
     * @see org.eclipse.jface.preference.IPreferencePage#performCancel()
     */
    @Override
    public boolean performCancel() {
        //re-enable "configure interpreter" dialogs
        PyDialogHelpers.enableAskInterpreterStep(true);
        return super.performCancel();
    }

    /**
     * Applies changes (if any)
     *
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        //IMPORTANT: we must call the perform before restoring the modules because this
        //info is going to be used when restoring them.
        super.performOk();

        //we need to update the tree so that the environment variables stay correct.
        pathEditor.updateTree();

        IInterpreterManager interpreterManager = getInterpreterManager();
        String newStringToPersist = AbstractInterpreterManager.getStringToPersist(pathEditor.getExesList());
        String oldStringToPersist = AbstractInterpreterManager.getStringToPersist(interpreterManager
                .getInterpreterInfos());
        boolean changed;
        if (!newStringToPersist.equals(oldStringToPersist)) {
            changed = true;
        } else {
            changed = false;
        }

        if (changed || inApply) {
            //If the user just presses 'apply' and nothing changed, he'll be asked to restore information on one of
            //the current interpreters.
            restoreInterpreterInfos(changed);
        }

        if (!inApply) {
            //re-enable "configure interpreter" dialogs, but only upon exiting the wizard
            PyDialogHelpers.enableAskInterpreterStep(true);
        }
        return true;
    }

    /**
     * @return the interpreter manager associated to this page.
     */
    protected abstract IInterpreterManager getInterpreterManager();

    /**
     * Creates the editors - also provides a hook for getting a different interpreter editor
     */
    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        pathEditor = getInterpreterEditor(p);
        addField(pathEditor);
    }

    /**
     * Restores the modules. Is called when the user changed something in the editor and applies the change.
     *
     * Gathers all the info and calls the hook that really restores things within a thread, so that the user can
     * get information on the progress.
     *
     * Only the information on the default interpreter is stored.
     *
     * @param editorChanged whether the editor was changed (if it wasn't, we'll ask the user what to restore).
     * @return true if the info was restored and false otherwise.
     */
    protected void restoreInterpreterInfos(boolean editorChanged) {
        pathEditor.restoreInterpreterInfos(editorChanged, getShell(), getInterpreterManager());
    }

}
