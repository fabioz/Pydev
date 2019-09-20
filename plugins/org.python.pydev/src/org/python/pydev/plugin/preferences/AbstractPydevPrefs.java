/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 29, 2006
 */
package org.python.pydev.plugin.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.StatusInfo;
import org.python.pydev.shared_ui.word_boundaries.SubWordPreferences;

public abstract class AbstractPydevPrefs extends PreferencePage implements IWorkbenchPreferencePage {

    /**
     * Defaults
     */
    protected final String[][] fAppearanceColorListModel = new String[][] { { "Code", PyDevEditorPreferences.CODE_COLOR, null },
            { "Decorators", PyDevEditorPreferences.DECORATOR_COLOR, null }, { "Numbers", PyDevEditorPreferences.NUMBER_COLOR, null },
            { "Matching brackets", PyDevEditorPreferences.MATCHING_BRACKETS_COLOR, null }, { "Keywords", PyDevEditorPreferences.KEYWORD_COLOR, null },
            { "self", PyDevEditorPreferences.SELF_COLOR, null }, { "Bytes", PyDevEditorPreferences.STRING_COLOR, null }, { "Unicode", PyDevEditorPreferences.UNICODE_COLOR, null },
            { "Docstring markup", PyDevEditorPreferences.DOCSTRING_MARKUP_COLOR, null }, { "Comments", PyDevEditorPreferences.COMMENT_COLOR, null },
            { "Backquotes", PyDevEditorPreferences.BACKQUOTES_COLOR, null }, { "Class Name", PyDevEditorPreferences.CLASS_NAME_COLOR, null },
            { "Function Name", PyDevEditorPreferences.FUNC_NAME_COLOR, null }, { "(), [], {}", PyDevEditorPreferences.PARENS_COLOR, null },
            { "Operators (+,-,*,...)", PyDevEditorPreferences.OPERATORS_COLOR, null },
            { "Variable (approx.)", PyDevEditorPreferences.VARIABLE_COLOR, null },
            { "Properties (approx.)", PyDevEditorPreferences.PROPERTY_COLOR, null }, };

    protected final String[][] fAppearanceFontListModel = new String[][] { { "Code", PyDevEditorPreferences.CODE_STYLE, null },
            { "Decorators", PyDevEditorPreferences.DECORATOR_STYLE, null }, { "Numbers", PyDevEditorPreferences.NUMBER_STYLE, null },
            { "Matching brackets", PyDevEditorPreferences.MATCHING_BRACKETS_STYLE, null }, { "Keywords", PyDevEditorPreferences.KEYWORD_STYLE, null },
            { "self", PyDevEditorPreferences.SELF_STYLE, null }, { "Bytes", PyDevEditorPreferences.STRING_STYLE, null }, { "Unicode", PyDevEditorPreferences.UNICODE_STYLE, null },
            { "Docstring markup", PyDevEditorPreferences.DOCSTRING_MARKUP_STYLE, null }, { "Comments", PyDevEditorPreferences.COMMENT_STYLE, null },
            { "Backquotes", PyDevEditorPreferences.BACKQUOTES_STYLE, null }, { "Class Name", PyDevEditorPreferences.CLASS_NAME_STYLE, null },
            { "Function Name", PyDevEditorPreferences.FUNC_NAME_STYLE, null }, { "(), [], {}", PyDevEditorPreferences.PARENS_STYLE, null },
            { "Operators (+,-,*,...)", PyDevEditorPreferences.OPERATORS_STYLE, null },
            { "Variable (approx.)", PyDevEditorPreferences.VARIABLE_STYLE, null },
            { "Properties (approx.)", PyDevEditorPreferences.PROPERTY_STYLE, null }, };

    protected OverlayPreferenceStore fOverlayStore;

    protected Map<Button, String> fCheckBoxes = new HashMap<Button, String>();
    protected SelectionListener fCheckBoxListener = new SelectionListener() {
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            Button button = (Button) e.widget;
            fOverlayStore.setValue(fCheckBoxes.get(button), button.getSelection());
        }
    };

    protected Map<Text, String> fTextFields = new HashMap<Text, String>();
    protected ModifyListener fTextFieldListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            Text text = (Text) e.widget;
            fOverlayStore.setValue(fTextFields.get(text), text.getText());
        }
    };

    protected java.util.List<Text> fNumberFields = new ArrayList<Text>();
    protected ModifyListener fNumberFieldListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            numberFieldChanged((Text) e.widget);
        }
    };

    protected List fAppearanceColorList;
    protected ColorEditor fAppearanceColorEditor;
    protected Button fAppearanceColorDefault;
    protected Button fFontBoldCheckBox;
    protected Button fFontItalicCheckBox;

    protected SelectionListener fStyleCheckBoxListener = new SelectionListener() {
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            // do nothing
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            int i = fAppearanceColorList.getSelectionIndex();
            int style = SWT.NORMAL;
            String styleKey = fAppearanceFontListModel[i][1];
            if (fFontBoldCheckBox.getSelection()) {
                style = style | SWT.BOLD;
            }
            if (fFontItalicCheckBox.getSelection()) {
                style = style | SWT.ITALIC;
            }
            fOverlayStore.setValue(styleKey, style);
            onAppearanceRelatedPreferenceChanged();
        }
    };

    /**
     * Tells whether the fields are initialized.
     * @since 3.0
     */
    protected boolean fFieldsInitialized = false;

    /**
     * List of master/slave listeners when there's a dependency.
     *
     * @see #createDependency(Button, String, Control)
     * @since 3.0
     */
    protected java.util.List<SelectionListener> fMasterSlaveListeners = new ArrayList<SelectionListener>();

    protected OverlayPreferenceStore createOverlayStore() {

        java.util.List<OverlayPreferenceStore.OverlayKey> overlayKeys = new ArrayList<OverlayPreferenceStore.OverlayKey>();

        overlayKeys
                .add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
                        SubWordPreferences.WORD_NAVIGATION_STYLE));

        //matching
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PyDevEditorPreferences.USE_MATCHING_BRACKETS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.MATCHING_BRACKETS_COLOR));

        //colors
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.CODE_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.NUMBER_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.DECORATOR_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.KEYWORD_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.SELF_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.STRING_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.UNICODE_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.COMMENT_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.BACKQUOTES_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.CLASS_NAME_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.FUNC_NAME_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.PARENS_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.OPERATORS_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.DOCSTRING_MARKUP_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.VARIABLE_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PyDevEditorPreferences.PROPERTY_COLOR));

        //font style
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.CODE_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.NUMBER_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.DECORATOR_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.KEYWORD_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.SELF_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.STRING_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.UNICODE_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.COMMENT_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.BACKQUOTES_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.CLASS_NAME_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.FUNC_NAME_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.PARENS_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.OPERATORS_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.DOCSTRING_MARKUP_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.VARIABLE_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PyDevEditorPreferences.PROPERTY_STYLE));

        OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
        overlayKeys.toArray(keys);
        return new OverlayPreferenceStore(getPreferenceStore(), keys);
    }

    /**
     * This method should be called when the preferences change for some appearance option
     * (color, bold, italic).
     */
    protected void onAppearanceRelatedPreferenceChanged() {
    }

    /*
     * @see IWorkbenchPreferencePage#init()
     */
    @Override
    public void init(IWorkbench workbench) {
    }

    protected Button addStyleCheckBox(Composite parent, String text) {
        Button result = new Button(parent, SWT.CHECK);
        result.setText(text);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        result.setLayoutData(gd);
        result.addSelectionListener(fStyleCheckBoxListener);
        return result;
    }

    /*
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {

        initializeDefaultColors();

        fOverlayStore.load();
        fOverlayStore.start();

        Control control = createAppearancePage(parent);

        initialize();
        Dialog.applyDialogFont(control);
        return control;
    }

    protected abstract Control createAppearancePage(Composite parent);

    protected void initialize() {

        initializeFields();
        if (fAppearanceColorList != null) {
            for (int i = 0; i < fAppearanceColorListModel.length; i++) {
                fAppearanceColorList.add(fAppearanceColorListModel[i][0]);
            }

            fAppearanceColorList.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (fAppearanceColorList != null && !fAppearanceColorList.isDisposed()) {
                        fAppearanceColorList.select(0);
                        handleAppearanceColorListSelection();
                    }
                }
            });
        }
    }

    protected void initializeFields() {

        Iterator e = fCheckBoxes.keySet().iterator();
        while (e.hasNext()) {
            Button b = (Button) e.next();
            String key = fCheckBoxes.get(b);
            b.setSelection(fOverlayStore.getBoolean(key));
        }

        e = fTextFields.keySet().iterator();
        while (e.hasNext()) {
            Text t = (Text) e.next();
            String key = fTextFields.get(t);
            t.setText(fOverlayStore.getString(key));
        }

        fFieldsInitialized = true;
        updateStatus(validatePositiveNumber("0"));

        // Update slaves
        Iterator<SelectionListener> iter = fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener = iter.next();
            listener.widgetSelected(null);
        }
    }

    protected void initializeDefaultColors() {
        if (!getPreferenceStore().contains(
                AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR)) {
            RGB rgb = getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();
            PreferenceConverter.setDefault(fOverlayStore,
                    AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, rgb);
            PreferenceConverter.setDefault(getPreferenceStore(),
                    AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, rgb);
        }
        if (!getPreferenceStore().contains(
                AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR)) {
            RGB rgb = getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT).getRGB();
            PreferenceConverter.setDefault(fOverlayStore,
                    AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, rgb);
            PreferenceConverter.setDefault(getPreferenceStore(),
                    AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, rgb);
        }
    }

    /*
     * @see PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        fOverlayStore.propagate();
        PydevPlugin.getDefault().savePluginPreferences();
        return true;
    }

    /*
     * @see PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {

        fOverlayStore.loadDefaults();

        initializeFields();

        handleAppearanceColorListSelection();

        super.performDefaults();

        onAppearanceRelatedPreferenceChanged();
    }

    /*
     * @see DialogPage#dispose()
     */
    @Override
    public void dispose() {

        if (fOverlayStore != null) {
            fOverlayStore.stop();
            fOverlayStore = null;
        }

        super.dispose();
    }

    protected Button addCheckBox(Composite parent, String label, String key, int indentation) {
        Button checkBox = new Button(parent, SWT.CHECK);
        checkBox.setText(label);

        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        gd.horizontalSpan = 2;
        checkBox.setLayoutData(gd);
        checkBox.addSelectionListener(fCheckBoxListener);

        fCheckBoxes.put(checkBox, key);

        return checkBox;
    }

    protected Label addLabel(Composite parent, String label, int indentation) {
        Label labelWidget = new Label(parent, SWT.None);
        labelWidget.setText(label);

        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        gd.horizontalSpan = 2;
        labelWidget.setLayoutData(gd);

        return labelWidget;
    }

    protected Control addTextField(Composite composite, String label, String key, int textLimit, int indentation,
            boolean isNumber) {

        Label labelControl = new Label(composite, SWT.NONE);
        labelControl.setText(label);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        labelControl.setLayoutData(gd);

        Text textControl = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = convertWidthInCharsToPixels(textLimit + 1);
        textControl.setLayoutData(gd);
        textControl.setTextLimit(textLimit);
        fTextFields.put(textControl, key);
        if (isNumber) {
            fNumberFields.add(textControl);
            textControl.addModifyListener(fNumberFieldListener);
        } else {
            textControl.addModifyListener(fTextFieldListener);
        }

        return textControl;
    }

    protected void createInverseDependency(final Button master, String masterKey, final Control slave) {
        doCreateDependency(master, masterKey, slave, false);
    }

    protected void createDependency(final Button master, String masterKey, final Control slave) {
        doCreateDependency(master, masterKey, slave, true);
    }

    private void doCreateDependency(final Button master, String masterKey, final Control slave,
            final boolean enableIf) {
        indent(slave);

        boolean masterState = fOverlayStore.getBoolean(masterKey);
        slave.setEnabled(masterState == enableIf);

        SelectionListener listener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                slave.setEnabled(master.getSelection() == enableIf);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };
        master.addSelectionListener(listener);
        fMasterSlaveListeners.add(listener);
    }

    protected static void indent(Control control) {
        GridData gridData = new GridData();
        gridData.horizontalIndent = 20;
        control.setLayoutData(gridData);
    }

    protected void numberFieldChanged(Text textControl) {
        String number = textControl.getText();
        IStatus status = validatePositiveNumber(number);
        if (!status.matches(IStatus.ERROR)) {
            fOverlayStore.setValue(fTextFields.get(textControl), number);
        }
        updateStatus(status);
    }

    protected IStatus validatePositiveNumber(String number) {
        StatusInfo status = new StatusInfo();
        if (number.length() == 0) {
            status.setError("empty_input??");
        } else {
            try {
                int value = Integer.parseInt(number);
                if (value < 0) {
                    status.setError("invalid_input??");
                }
            } catch (NumberFormatException e) {
                status.setError("invalid_input??");
            }
        }
        return status;
    }

    void updateStatus(IStatus status) {
        if (!fFieldsInitialized) {
            return;
        }

        if (!status.matches(IStatus.ERROR)) {
            for (int i = 0; i < fNumberFields.size(); i++) {
                Text text = fNumberFields.get(i);
                IStatus s = validatePositiveNumber(text.getText());
                status = s.getSeverity() > status.getSeverity() ? s : status;
            }
        }
        setValid(!status.matches(IStatus.ERROR));
        applyToStatusLine(this, status);
    }

    /**
     * Applies the status to the status line of a dialog page.
     *
     * @param page the dialog page
     * @param status the status
     */
    public void applyToStatusLine(DialogPage page, IStatus status) {
        String message = status.getMessage();
        switch (status.getSeverity()) {
            case IStatus.OK:
                page.setMessage(message, IMessageProvider.NONE);
                page.setErrorMessage(null);
                break;
            case IStatus.WARNING:
                page.setMessage(message, IMessageProvider.WARNING);
                page.setErrorMessage(null);
                break;
            case IStatus.INFO:
                page.setMessage(message, IMessageProvider.INFORMATION);
                page.setErrorMessage(null);
                break;
            default:
                if (message.length() == 0) {
                    message = null;
                }
                page.setMessage(null);
                page.setErrorMessage(message);
                break;
        }
    }

    protected void handleAppearanceColorListSelection() {
        int i = fAppearanceColorList.getSelectionIndex();
        String key = fAppearanceColorListModel[i][1];
        RGB rgb = PreferenceConverter.getColor(fOverlayStore, key);
        fAppearanceColorEditor.setColorValue(rgb);
        String styleKey = fAppearanceFontListModel[i][1];
        int styleValue = fOverlayStore.getInt(styleKey);
        if ((styleValue & SWT.BOLD) == 0) {
            fFontBoldCheckBox.setSelection(false);
        } else {
            fFontBoldCheckBox.setSelection(true);
        }
        if ((styleValue & SWT.ITALIC) == 0) {
            fFontItalicCheckBox.setSelection(false);
        } else {
            fFontItalicCheckBox.setSelection(true);
        }
        updateAppearanceColorWidgets(fAppearanceColorListModel[i][2]);
    }

    protected void updateAppearanceColorWidgets(String systemDefaultKey) {
        if (systemDefaultKey == null) {
            fAppearanceColorDefault.setSelection(false);
            fAppearanceColorDefault.setVisible(false);
            fAppearanceColorEditor.getButton().setEnabled(true);
        } else {
            boolean systemDefault = fOverlayStore.getBoolean(systemDefaultKey);
            fAppearanceColorDefault.setSelection(systemDefault);
            fAppearanceColorDefault.setVisible(true);
            fAppearanceColorEditor.getButton().setEnabled(!systemDefault);
        }
    }

}
