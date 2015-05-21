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

public abstract class AbstractPydevPrefs extends PreferencePage implements IWorkbenchPreferencePage {

    //   Preferences
    //To add a new preference it needs to be included in
    //createAppearancePage
    //createOverlayStore
    //initializeDefaultPreferences
    //declaration of fAppearanceColorListModel if it is a color
    //constants (here)
    public static final int TOOLTIP_WIDTH = 80;

    /*
     * If you just want to add some option, you will need to:
     * - create fields for it, as seen here
     * - add to overlay store in createOverlayStore()
     * - add what appears in the Preferences page at createAppearancePage()
     * - add the function to the org.python.pydev.editor.autoedit.IIndentPrefs interface
     * - probably add that function to org.python.pydev.editor.autoedit.DefaultIndentPrefs
     *
     */

    /**
     * Edition of translation paths.
     */
    public static final String SOURCE_LOCATION_PATHS = "SOURCE_LOCATION_PATHS";

    //text
    public static final String TAB_WIDTH = "TAB_WIDTH";
    public static final int DEFAULT_TAB_WIDTH = 4;

    //checkboxes
    public static final String SUBSTITUTE_TABS = "SUBSTITUTE_TABS";
    public static final boolean DEFAULT_SUBSTITUTE_TABS = true;

    public static final String GUESS_TAB_SUBSTITUTION = "GUESS_TAB_SUBSTITUTION";
    public static final boolean DEFAULT_GUESS_TAB_SUBSTITUTION = true;

    public static final String TAB_STOP_IN_COMMENT = "TAB_STOP_IN_COMMENT";
    public static final boolean DEFAULT_TAB_STOP_IN_COMMENT = true;

    public static final String USE_VERTICAL_INDENT_GUIDE = "USE_VERTICAL_INDENT_GUIDE";
    public static final boolean DEFAULT_USE_VERTICAL_INDENT_GUIDE = true;

    public static final String USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND = "USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND";
    public static final boolean DEFAULT_USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND = true;

    public static final String VERTICAL_INDENT_COLOR = "VERTICAL_INDENT_COLOR";
    public static final RGB DEFAULT_VERTICAL_INDENT_COLOR = new RGB(125, 125, 125);

    public static final String VERTICAL_INDENT_TRANSPARENCY = "VERTICAL_INDENT_TRANSPARENCY";
    public static final int DEFAULT_VERTICAL_INDENT_TRANSPARENCY = 100;

    public static final boolean DEFAULT_EDITOR_USE_CUSTOM_CARETS = false;
    public static final boolean DEFAULT_EDITOR_WIDE_CARET = false;

    //matching
    public static final String USE_MATCHING_BRACKETS = "USE_MATCHING_BRACKETS";
    public static final boolean DEFAULT_USE_MATCHING_BRACKETS = true;

    public static final String MATCHING_BRACKETS_COLOR = "EDITOR_MATCHING_BRACKETS_COLOR";
    public static final RGB DEFAULT_MATCHING_BRACKETS_COLOR = new RGB(64, 128, 128);

    public static final String MATCHING_BRACKETS_STYLE = "EDITOR_MATCHING_BRACKETS_STYLE";
    public static final int DEFAULT_MATCHING_BRACKETS_STYLE = SWT.NORMAL;

    //colors
    public static final String DECORATOR_COLOR = "DECORATOR_COLOR";
    public static final RGB DEFAULT_DECORATOR_COLOR = new RGB(125, 125, 125);

    public static final String NUMBER_COLOR = "NUMBER_COLOR";
    public static final RGB DEFAULT_NUMBER_COLOR = new RGB(128, 0, 0);

    public static final String CODE_COLOR = "CODE_COLOR";
    public static final RGB DEFAULT_CODE_COLOR = new RGB(0, 0, 0);

    public static final String KEYWORD_COLOR = "KEYWORD_COLOR";
    public static final RGB DEFAULT_KEYWORD_COLOR = new RGB(0, 0, 255);

    public static final String SELF_COLOR = "SELF_COLOR";
    public static final RGB DEFAULT_SELF_COLOR = new RGB(0, 0, 0);

    public static final String STRING_COLOR = "STRING_COLOR";
    public static final RGB DEFAULT_STRING_COLOR = new RGB(201, 128, 43);

    public static final String UNICODE_COLOR = "UNICODE_COLOR";
    public static final RGB DEFAULT_UNICODE_COLOR = new RGB(0, 170, 0);

    public static final String COMMENT_COLOR = "COMMENT_COLOR";
    public static final RGB DEFAULT_COMMENT_COLOR = new RGB(192, 192, 192);

    public static final String BACKQUOTES_COLOR = "BACKQUOTES_COLOR";
    public static final RGB DEFAULT_BACKQUOTES_COLOR = new RGB(0, 0, 0);

    public static final String CLASS_NAME_COLOR = "CLASS_NAME_COLOR";
    public static final RGB DEFAULT_CLASS_NAME_COLOR = new RGB(0, 0, 0);

    public static final String FUNC_NAME_COLOR = "FUNC_NAME_COLOR";
    public static final RGB DEFAULT_FUNC_NAME_COLOR = new RGB(0, 0, 0);

    public static final String PARENS_COLOR = "PARENS_COLOR";
    public static final RGB DEFAULT_PARENS_COLOR = new RGB(0, 0, 0);

    public static final String OPERATORS_COLOR = "OPERATORS_COLOR";
    public static final RGB DEFAULT_OPERATORS_COLOR = new RGB(0, 0, 0);

    public static final String DOCSTRING_MARKUP_COLOR = "DOCSTRING_MARKUP_COLOR";
    public static final RGB DEFAULT_DOCSTRING_MARKUP_COLOR = new RGB(0, 170, 0);

    //see initializeDefaultColors for selection defaults
    public static final String CONNECT_TIMEOUT = "CONNECT_TIMEOUT";
    public static final int DEFAULT_CONNECT_TIMEOUT = 20000;

    public static final String RELOAD_MODULE_ON_CHANGE = "RELOAD_MODULE_ON_CHANGE";
    public static final boolean DEFAULT_RELOAD_MODULE_ON_CHANGE = true;

    public static final String DONT_TRACE_ENABLED = "DONT_TRACE_ENABLED";
    public static final boolean DEFAULT_DONT_TRACE_ENABLED = true;

    public static final String TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS = "TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS";
    public static final boolean DEFAULT_TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS = false;

    public static final String DEBUG_MULTIPROCESSING_ENABLED = "DEBUG_MULTIPROCESSING_ENABLED";
    public static final boolean DEFAULT_DEBUG_MULTIPROCESSING_ENABLED = true;

    public static final String KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS = "KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS";
    public static final boolean DEFAULT_KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS = true;

    public static final String GEVENT_DEBUGGING = "GEVENT_DEBUGGING";
    public static final boolean DEFAULT_GEVENT_DEBUGGING = false;

    //font
    public static final String DECORATOR_STYLE = "DECORATOR_STYLE";
    public static final int DEFAULT_DECORATOR_STYLE = SWT.ITALIC;

    public static final String NUMBER_STYLE = "NUMBER_STYLE";
    public static final int DEFAULT_NUMBER_STYLE = SWT.NORMAL;

    public static final String CODE_STYLE = "CODE_STYLE";
    public static final int DEFAULT_CODE_STYLE = SWT.NORMAL;

    public static final String KEYWORD_STYLE = "KEYWORD_STYLE";
    public static final int DEFAULT_KEYWORD_STYLE = SWT.NORMAL;

    public static final String SELF_STYLE = "SELF_STYLE";
    public static final int DEFAULT_SELF_STYLE = SWT.ITALIC;

    public static final String STRING_STYLE = "STRING_STYLE";
    public static final int DEFAULT_STRING_STYLE = SWT.ITALIC;

    public static final String UNICODE_STYLE = "UNICODE_STYLE";
    public static final int DEFAULT_UNICODE_STYLE = SWT.ITALIC;

    public static final String COMMENT_STYLE = "COMMENT_STYLE";
    public static final int DEFAULT_COMMENT_STYLE = SWT.NORMAL;

    public static final String BACKQUOTES_STYLE = "BACKQUOTES_STYLE";
    public static final int DEFAULT_BACKQUOTES_STYLE = SWT.BOLD;

    public static final String CLASS_NAME_STYLE = "CLASS_NAME_STYLE";
    public static final int DEFAULT_CLASS_NAME_STYLE = SWT.BOLD;

    public static final String FUNC_NAME_STYLE = "FUNC_NAME_STYLE";
    public static final int DEFAULT_FUNC_NAME_STYLE = SWT.BOLD;

    public static final String PARENS_STYLE = "PARENS_STYLE";
    public static final int DEFAULT_PARENS_STYLE = SWT.NORMAL;

    public static final String OPERATORS_STYLE = "OPERATORS_STYLE";
    public static final int DEFAULT_OPERATORS_STYLE = SWT.NORMAL;

    public static final String DOCSTRING_MARKUP_STYLE = "DOCSTRING_MARKUP_STYLE";
    public static final int DEFAULT_DOCSTRING_MARKUP_STYLE = SWT.BOLD;

    /**
     * Defaults
     */
    protected final String[][] fAppearanceColorListModel = new String[][] { { "Code", CODE_COLOR, null },
            { "Decorators", DECORATOR_COLOR, null }, { "Numbers", NUMBER_COLOR, null },
            { "Matching brackets", MATCHING_BRACKETS_COLOR, null }, { "Keywords", KEYWORD_COLOR, null },
            { "self", SELF_COLOR, null }, { "Bytes", STRING_COLOR, null }, { "Unicode", UNICODE_COLOR, null },
            { "Docstring markup", DOCSTRING_MARKUP_COLOR, null }, { "Comments", COMMENT_COLOR, null },
            { "Backquotes", BACKQUOTES_COLOR, null }, { "Class Name", CLASS_NAME_COLOR, null },
            { "Function Name", FUNC_NAME_COLOR, null }, { "(), [], {}", PARENS_COLOR, null },
            { "Operators (+,-,*,...)", OPERATORS_COLOR, null }, };

    protected final String[][] fAppearanceFontListModel = new String[][] { { "Code", CODE_STYLE, null },
            { "Decorators", DECORATOR_STYLE, null }, { "Numbers", NUMBER_STYLE, null },
            { "Matching brackets", MATCHING_BRACKETS_STYLE, null }, { "Keywords", KEYWORD_STYLE, null },
            { "self", SELF_STYLE, null }, { "Bytes", STRING_STYLE, null }, { "Unicode", UNICODE_STYLE, null },
            { "Docstring markup", DOCSTRING_MARKUP_STYLE, null }, { "Comments", COMMENT_STYLE, null },
            { "Backquotes", BACKQUOTES_STYLE, null }, { "Class Name", CLASS_NAME_STYLE, null },
            { "Function Name", FUNC_NAME_STYLE, null }, { "(), [], {}", PARENS_STYLE, null },
            { "Operators (+,-,*,...)", OPERATORS_STYLE, null }, };

    protected OverlayPreferenceStore fOverlayStore;

    protected Map<Button, String> fCheckBoxes = new HashMap<Button, String>();
    protected SelectionListener fCheckBoxListener = new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
            Button button = (Button) e.widget;
            fOverlayStore.setValue(fCheckBoxes.get(button), button.getSelection());
        }
    };

    protected Map<Text, String> fTextFields = new HashMap<Text, String>();
    protected ModifyListener fTextFieldListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
            Text text = (Text) e.widget;
            fOverlayStore.setValue(fTextFields.get(text), text.getText());
        }
    };

    protected java.util.List<Text> fNumberFields = new ArrayList<Text>();
    protected ModifyListener fNumberFieldListener = new ModifyListener() {
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
        public void widgetDefaultSelected(SelectionEvent e) {
            // do nothing
        }

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

        //matching
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_MATCHING_BRACKETS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, MATCHING_BRACKETS_COLOR));

        //colors
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CODE_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, NUMBER_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, DECORATOR_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, KEYWORD_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, SELF_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, STRING_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, UNICODE_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, COMMENT_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, BACKQUOTES_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CLASS_NAME_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, FUNC_NAME_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PARENS_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, OPERATORS_COLOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, DOCSTRING_MARKUP_COLOR));

        //font style
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, CODE_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, NUMBER_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, DECORATOR_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, KEYWORD_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, SELF_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, STRING_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, UNICODE_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, COMMENT_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, BACKQUOTES_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, CLASS_NAME_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, FUNC_NAME_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PARENS_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, OPERATORS_STYLE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, DOCSTRING_MARKUP_STYLE));

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
            public void widgetSelected(SelectionEvent e) {
                slave.setEnabled(master.getSelection() == enableIf);
            }

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
