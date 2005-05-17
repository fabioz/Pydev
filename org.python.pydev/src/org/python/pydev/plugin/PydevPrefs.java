/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Scott Schlesier - Adapted for use in pydev
 *     Fabio Zadrozny 
 *******************************************************************************/

package org.python.pydev.plugin;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.editors.text.ITextEditorHelpContextIds;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;



/**
 * The preference page for setting the editor options.
 * <p>
 * This class is internal and not intended to be used by clients.</p>
 * 
 * @since 2.1
 */
public class PydevPrefs extends PreferencePage implements IWorkbenchPreferencePage {
	
//	 Preferences
	//To add a new preference it needs to be included in
	//createAppearancePage
	//createOverlayStore
	//initializeDefaultPreferences
	//declaration of fAppearanceColorListModel if it is a color
	//constants (here)
	
	//text
	public static final String TAB_WIDTH = "TAB_WIDTH";
	public static final int DEFAULT_TAB_WIDTH = 4;
	
	public static final String DEFAULT_EDITOR_PRINT_MARGIN_COLUMN = "80";
	
	public static final String BLOCK_COMMENT = "BLOCK_COMMENT";
	public static final String DEFAULT_BLOCK_COMMENT_STRING = "================================================================================";
	
	//checkboxes
	public static final String SUBSTITUTE_TABS = "SUBSTITUTE_TABS";
	public static final boolean DEFAULT_SUBSTITUTE_TABS = true;
	
	public static final String USE_CODE_FOLDING = "USE_CODE_FOLDING";
	public static final boolean DEFAULT_USE_CODE_FOLDING = true;
	
	public static final String GUESS_TAB_SUBSTITUTION = "GUESS_TAB_SUBSTITUTION";
	public static final boolean DEFAULT_GUESS_TAB_SUBSTITUTION = true;
	
	public static final boolean DEFAULT_EDITOR_OVERVIEW_RULER = false;
	public static final boolean DEFAULT_EDITOR_LINE_NUMBER_RULER = false;
	private static final boolean DEFAULT_EDITOR_CURRENT_LINE = true;
	public static final boolean DEFAULT_EDITOR_PRINT_MARGIN = true;
	public static final boolean DEFAULT_EDITOR_USE_CUSTOM_CARETS = false;
	public static final boolean DEFAULT_EDITOR_WIDE_CARET = false;
	
	//matching
    public static final boolean DEFAULT_USE_MATCHING_BRACKETS = true;
    public static final String USE_MATCHING_BRACKETS = "USE_MATCHING_BRACKETS";
    public static final RGB DEFAULT_MATCHING_BRACKETS_COLOR = new RGB(64,128,128);
    public static final String MATCHING_BRACKETS_COLOR = "EDITOR_MATCHING_BRACKETS_COLOR";
	
	//colors
    public static final String DECORATOR_COLOR = "DECORATOR_COLOR";
	private static final RGB DEFAULT_DECORATOR_COLOR = new RGB(125, 125, 125);

    public static final String NUMBER_COLOR = "NUMBER_COLOR";
	private static final RGB DEFAULT_NUMBER_COLOR = new RGB(128, 0, 0);

    public static final String CODE_COLOR = "CODE_COLOR";
	private static final RGB DEFAULT_CODE_COLOR = new RGB(0, 0, 0);
	
	public static final String KEYWORD_COLOR = "KEYWORD_COLOR";
	private static final RGB DEFAULT_KEYWORD_COLOR = new RGB(0, 0, 255);
	
	public static final String STRING_COLOR = "STRING_COLOR";
	private static final RGB DEFAULT_STRING_COLOR = new RGB(0, 170, 0);
	
	public static final String COMMENT_COLOR = "COMMENT_COLOR";
	private static final RGB DEFAULT_COMMENT_COLOR = new RGB(192, 192, 192);
	
	public static final String HYPERLINK_COLOR = "HYPERLINK_COLOR";
	private static final RGB DEFAULT_HYPERLINK_COLOR = new RGB(0, 0, 238);
	
	private static final RGB DEFAULT_PREFERENCE_COLOR_BACKGROUND = new RGB(255, 255, 255);	
	public static final boolean DEFAULT_EDITOR_BACKGROUND_COLOR_SYSTEM_DEFAULT = true;
	private static final RGB DEFAULT_EDITOR_CURRENT_LINE_COLOR = new RGB(244, 255, 255);	
	public static final RGB DEFAULT_EDITOR_LINE_NUMBER_RULER_COLOR = new RGB(0, 0, 0);
	public static final RGB DEFAULT_EDITOR_PRINT_MARGIN_COLOR = new RGB(192,192,192);
	
	//see initializeDefaultColors for selection defaults
	public static final boolean DEFAULT_EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR = true;
	public static final boolean DEFAULT_EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR = true;
	
	
	public static final String CONNECT_TIMEOUT = "CONNECT_TIMEOUT";
	public static final int DEFAULT_CONNECT_TIMEOUT = 20000;
	
	public static final String RUN_MANY_SCRIPT_LOCATION = "RUN_MANY_SCRIPT_LOCATION";
	public static final String DEFAULT_RUN_MANY_SCRIPT_LOCATION = "";
	
		
	/**
	 * Defaults
	 */
	private final String[][] fAppearanceColorListModel= new String[][] {
		{"Code", CODE_COLOR, null},
		{"Decorators", DECORATOR_COLOR, null},
		{"Numbers", NUMBER_COLOR, null},
		{"Matching brackets", MATCHING_BRACKETS_COLOR, null},
		{"Keywords", KEYWORD_COLOR, null},
		{"Strings", STRING_COLOR, null},
		{"Comments", COMMENT_COLOR, null},
		{"Hyperlink", HYPERLINK_COLOR, null},
		{"Background", AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT},
		{"Current line highlight", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR, null},
		{"Line numbers", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR, null},		 
		{"Print margin", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR, null}, 
		{"Selection foreground", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR}, 
		{"Selection background", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR}, 
	};
	
	private OverlayPreferenceStore fOverlayStore;
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	private Map fTextFields= new HashMap();
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			Text text= (Text) e.widget;
			fOverlayStore.setValue((String) fTextFields.get(text), text.getText());
		}
	};

	private ArrayList fNumberFields= new ArrayList();
	private ModifyListener fNumberFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			numberFieldChanged((Text) e.widget);
		}
	};
	
	private List fAppearanceColorList;
	private ColorEditor fAppearanceColorEditor;
	private Button fAppearanceColorDefault;
	
	/**
	 * Tells whether the fields are initialized.
	 * @since 3.0
	 */
	private boolean fFieldsInitialized= false;
	
	/**
	 * List of master/slave listeners when there's a dependency.
	 * 
	 * @see #createDependency(Button, String, Control)
	 * @since 3.0
	 */
	private ArrayList fMasterSlaveListeners= new ArrayList();

	
	public PydevPrefs() {
		setDescription("Pydev Editor settings:"); 
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
		
		fOverlayStore= createOverlayStore();
	}
	
	private OverlayPreferenceStore createOverlayStore() {
		
		ArrayList overlayKeys= new ArrayList();
		
		//text
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, TAB_WIDTH));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, BLOCK_COMMENT));
		
		//matching
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_MATCHING_BRACKETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,  MATCHING_BRACKETS_COLOR));
		
		//checkbox		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, SUBSTITUTE_TABS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, GUESS_TAB_SUBSTITUTION));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_CODE_FOLDING));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET));
		
		//colors
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CODE_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, NUMBER_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, DECORATOR_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, KEYWORD_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, STRING_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, COMMENT_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, HYPERLINK_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR));
		
		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return new OverlayPreferenceStore(getPreferenceStore(), keys);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init()
	 */	
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), ITextEditorHelpContextIds.TEXT_EDITOR_PREFERENCE_PAGE);
	}

	private void handleAppearanceColorListSelection() {	
		int i= fAppearanceColorList.getSelectionIndex();
		String key= fAppearanceColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fAppearanceColorEditor.setColorValue(rgb);		
		updateAppearanceColorWidgets(fAppearanceColorListModel[i][2]);
	}

	private void updateAppearanceColorWidgets(String systemDefaultKey) {
		if (systemDefaultKey == null) {
			fAppearanceColorDefault.setSelection(false);
			fAppearanceColorDefault.setVisible(false);
			fAppearanceColorEditor.getButton().setEnabled(true);
		} else {
			boolean systemDefault= fOverlayStore.getBoolean(systemDefaultKey);
			fAppearanceColorDefault.setSelection(systemDefault);
			fAppearanceColorDefault.setVisible(true);
			fAppearanceColorEditor.getButton().setEnabled(!systemDefault);
		}
	}
	
	private Control createAppearancePage(Composite parent) {

		Composite appearanceComposite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		appearanceComposite.setLayout(layout);

		addTextField(appearanceComposite, "Tab length:", TAB_WIDTH, 3, 0, true);

		addTextField(appearanceComposite, "Print margin column:", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, 3, 0, true);
		
		addTextField(appearanceComposite, "Block comment seperator:", BLOCK_COMMENT, 50, 0, false);
				
		addCheckBox(appearanceComposite, "Substitute spaces for tabs?", SUBSTITUTE_TABS, 0);
		
		addCheckBox(appearanceComposite, "Assume tab spacing when files contain tabs?", GUESS_TAB_SUBSTITUTION, 0);
		
		addCheckBox(appearanceComposite, "Use code folding?", USE_CODE_FOLDING, 0);
		
		addCheckBox(appearanceComposite, "Show overview ruler", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER, 0);
				
		addCheckBox(appearanceComposite, "Show line numbers", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER, 0);

		addCheckBox(appearanceComposite, "Highlight current line", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE, 0);
				
		addCheckBox(appearanceComposite, "Show print margin", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN, 0);

		Button master= addCheckBox(appearanceComposite, "Use custom caret", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS, 0);

		Button slave= addCheckBox(appearanceComposite, "Enable thick caret", AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET, 0);
		createDependency(master, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS, slave);

		Label l= new Label(appearanceComposite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
		
		l= new Label(appearanceComposite, SWT.LEFT);
		l.setText("Appearance color options:"); 
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		l.setLayoutData(gd);

		Composite editorComposite= new Composite(appearanceComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		

		fAppearanceColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fAppearanceColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l= new Label(stylesComposite, SWT.LEFT);
		l.setText("Color:"); 
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		l.setLayoutData(gd);

		fAppearanceColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fAppearanceColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		SelectionListener colorDefaultSelectionListener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean systemDefault= fAppearanceColorDefault.getSelection();
				fAppearanceColorEditor.getButton().setEnabled(!systemDefault);
				
				int i= fAppearanceColorList.getSelectionIndex();
				String key= fAppearanceColorListModel[i][2];
				if (key != null)
					fOverlayStore.setValue(key, systemDefault);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		
		fAppearanceColorDefault= new Button(stylesComposite, SWT.CHECK);
		fAppearanceColorDefault.setText("System default"); 
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fAppearanceColorDefault.setLayoutData(gd);
		fAppearanceColorDefault.setVisible(false);
		fAppearanceColorDefault.addSelectionListener(colorDefaultSelectionListener);
		
		fAppearanceColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleAppearanceColorListSelection();
			}
		});
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fAppearanceColorList.getSelectionIndex();
				String key= fAppearanceColorListModel[i][1];
				
				PreferenceConverter.setValue(fOverlayStore, key, fAppearanceColorEditor.getColorValue());
			}
		});
		
		return appearanceComposite;
	}
	
	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		initializeDefaultColors();

		fOverlayStore.load();
		fOverlayStore.start();
		
		Control control= createAppearancePage(parent);

		initialize();
		Dialog.applyDialogFont(control);
		return control;
	}
	
	private void initialize() {
		
		initializeFields();
		
		for (int i= 0; i < fAppearanceColorListModel.length; i++)
			fAppearanceColorList.add(fAppearanceColorListModel[i][0]);
		fAppearanceColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fAppearanceColorList != null && !fAppearanceColorList.isDisposed()) {
					fAppearanceColorList.select(0);
					handleAppearanceColorListSelection();
				}
			}
		});
	}
	
	private void initializeFields() {
		
		Iterator e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
		
		e= fTextFields.keySet().iterator();
		while (e.hasNext()) {
			Text t= (Text) e.next();
			String key= (String) fTextFields.get(t);
			t.setText(fOverlayStore.getString(key));
		}
		
		fFieldsInitialized= true;
		updateStatus(validatePositiveNumber("0")); 
		
        // Update slaves
        Iterator iter= fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener= (SelectionListener)iter.next();
            listener.widgetSelected(null);
        }
	}
	
	private void initializeDefaultColors() {	
		if (!getPreferenceStore().contains(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, rgb);
		}
		if (!getPreferenceStore().contains(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, rgb);
		}
	}
	
	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fOverlayStore.propagate();
		PydevPlugin.getDefault().savePluginPreferences();
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {

		fOverlayStore.loadDefaults();
		
		initializeFields();

		handleAppearanceColorListSelection();

		super.performDefaults();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		
		super.dispose();
	}
	
	private Button addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		
		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
	}
	
	private Control addTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		
		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		Text textControl= new Text(composite, SWT.BORDER | SWT.SINGLE);		
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint= convertWidthInCharsToPixels(textLimit + 1);
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
	
	private void createDependency(final Button master, String masterKey, final Control slave) {
		indent(slave);
		
		boolean masterState= fOverlayStore.getBoolean(masterKey);
		slave.setEnabled(masterState);
		
		SelectionListener listener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		master.addSelectionListener(listener);
		fMasterSlaveListeners.add(listener);
	}
	
	private static void indent(Control control) {
		GridData gridData= new GridData();
		gridData.horizontalIndent= 20;
		control.setLayoutData(gridData);		
	}
	
	private void numberFieldChanged(Text textControl) {
		String number= textControl.getText();
		IStatus status= validatePositiveNumber(number);
		if (!status.matches(IStatus.ERROR))
			fOverlayStore.setValue((String) fTextFields.get(textControl), number);
		updateStatus(status);
	}
	
	private IStatus validatePositiveNumber(String number) {
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError("empty_input??"); 
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0)
					status.setError("invalid_input??"); 
			} catch (NumberFormatException e) {
				status.setError("invalid_input??"); 
			}
		}
		return status;
	}
	
	void updateStatus(IStatus status) {
		if (!fFieldsInitialized)
			return;
		
		if (!status.matches(IStatus.ERROR)) {
			for (int i= 0; i < fNumberFields.size(); i++) {
				Text text= (Text) fNumberFields.get(i);
				IStatus s= validatePositiveNumber(text.getText());
				status= s.getSeverity() > status.getSeverity() ? s : status;
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
		String message= status.getMessage();
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
					message= null;
				}
				page.setMessage(null);
				page.setErrorMessage(message);
				break;		
		}
	}
	
	/**
	 * Sets default preference values
	 */
	protected static void initializeDefaultPreferences(Preferences prefs) {
		//text
		prefs.setDefault(TAB_WIDTH, DEFAULT_TAB_WIDTH);
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, DEFAULT_EDITOR_PRINT_MARGIN_COLUMN);
		prefs.setDefault(BLOCK_COMMENT, DEFAULT_BLOCK_COMMENT_STRING);
		
		//checkboxes
		prefs.setDefault(SUBSTITUTE_TABS, DEFAULT_SUBSTITUTE_TABS);
		prefs.setDefault(GUESS_TAB_SUBSTITUTION, DEFAULT_GUESS_TAB_SUBSTITUTION);
		prefs.setDefault(USE_CODE_FOLDING, DEFAULT_USE_CODE_FOLDING);
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER, StringConverter.asString(DEFAULT_EDITOR_OVERVIEW_RULER));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER, StringConverter.asString(DEFAULT_EDITOR_LINE_NUMBER_RULER));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE, StringConverter.asString(DEFAULT_EDITOR_CURRENT_LINE));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN, StringConverter.asString(DEFAULT_EDITOR_PRINT_MARGIN));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS, StringConverter.asString(DEFAULT_EDITOR_USE_CUSTOM_CARETS));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET, StringConverter.asString(DEFAULT_EDITOR_WIDE_CARET));
		
		//matching
		prefs.setDefault(USE_MATCHING_BRACKETS, DEFAULT_USE_MATCHING_BRACKETS);
		prefs.setDefault(MATCHING_BRACKETS_COLOR, StringConverter.asString(DEFAULT_MATCHING_BRACKETS_COLOR));
		
		//colors
		prefs.setDefault(CODE_COLOR,StringConverter.asString(DEFAULT_CODE_COLOR));
		prefs.setDefault(NUMBER_COLOR,StringConverter.asString(DEFAULT_NUMBER_COLOR));
		prefs.setDefault(DECORATOR_COLOR,StringConverter.asString(DEFAULT_DECORATOR_COLOR));
		prefs.setDefault(KEYWORD_COLOR,StringConverter.asString(DEFAULT_KEYWORD_COLOR));
		prefs.setDefault(STRING_COLOR,StringConverter.asString(DEFAULT_STRING_COLOR));
		prefs.setDefault(COMMENT_COLOR,StringConverter.asString(DEFAULT_COMMENT_COLOR));
		prefs.setDefault(HYPERLINK_COLOR, StringConverter.asString(DEFAULT_HYPERLINK_COLOR));
		prefs.setDefault(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, StringConverter.asString(DEFAULT_PREFERENCE_COLOR_BACKGROUND));
		prefs.setDefault(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT, StringConverter.asString(DEFAULT_EDITOR_BACKGROUND_COLOR_SYSTEM_DEFAULT));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR, StringConverter.asString(DEFAULT_EDITOR_CURRENT_LINE_COLOR));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR, StringConverter.asString(DEFAULT_EDITOR_LINE_NUMBER_RULER_COLOR));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR, StringConverter.asString(DEFAULT_EDITOR_PRINT_MARGIN_COLOR));
		//for selection colors see initializeDefaultColors()
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR, StringConverter.asString(DEFAULT_EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR));
		prefs.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR, StringConverter.asString(DEFAULT_EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR));
		
		//no UI
		prefs.setDefault(CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
		prefs.setDefault(RUN_MANY_SCRIPT_LOCATION, DEFAULT_RUN_MANY_SCRIPT_LOCATION);		
	}
	

    /**
	 * @return the place where this plugin preferences are stored.
	 */
	public static Preferences getPreferences() {
		return 	PydevPlugin.getDefault().getPluginPreferences();
	}
	
	/**
	 * @return an array of strings with the available interpreters.
	 */
	public static String[] getInterpreters() {
		return PydevPlugin.interpreterManager.getInterpreters();
	}

}