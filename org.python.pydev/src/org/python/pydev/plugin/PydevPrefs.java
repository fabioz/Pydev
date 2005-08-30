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
import java.util.Collections;
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
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.docutils.WordUtils;


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
	
	public static final String AUTO_PAR = "AUTO_PAR";
	public static final boolean DEFAULT_AUTO_PAR = true;
	
	/**
	 * fields for automatically replacing a colon
	 * @see  
	 */
	public static final String AUTO_COLON = "AUTO_COLON";
	public static final boolean DEFAULT_AUTO_COLON = true;
	
	/**
	 * fields for automatically skipping braces
	 * @see  org.python.pydev.editor.autoedit.PyAutoIndentStrategy
	 */
	public static final String AUTO_BRACES = "AUTO_BRACES";
	public static final boolean DEFAULT_AUTO_BRACES = true;

    /**
     * Used if the 'import' should be written automatically in an from xxx import yyy
     */
    public static final String AUTO_WRITE_IMPORT_STR = "AUTO_WRITE_IMPORT_STR";
    public static final boolean DEFAULT_AUTO_WRITE_IMPORT_STR = true;

	//text
	public static final String TAB_WIDTH = "TAB_WIDTH";
	public static final int DEFAULT_TAB_WIDTH = 4;
	
	public static final String MULTI_BLOCK_COMMENT_CHAR = "MULTI_BLOCK_COMMENT_CHAR";
	public static final String DEFAULT_MULTI_BLOCK_COMMENT_CHAR = "=";
	
	public static final String SINGLE_BLOCK_COMMENT_CHAR = "SINGLE_BLOCK_COMMENT_CHAR";
	public static final String DEFAULT_SINGLE_BLOCK_COMMENT_CHAR = "-";
	
	//checkboxes
	public static final String SUBSTITUTE_TABS = "SUBSTITUTE_TABS";
	public static final boolean DEFAULT_SUBSTITUTE_TABS = true;
	
	public static final String USE_CODE_FOLDING = "USE_CODE_FOLDING";
	public static final boolean DEFAULT_USE_CODE_FOLDING = true;
	
	public static final String GUESS_TAB_SUBSTITUTION = "GUESS_TAB_SUBSTITUTION";
	public static final boolean DEFAULT_GUESS_TAB_SUBSTITUTION = true;
	
	public static final boolean DEFAULT_EDITOR_USE_CUSTOM_CARETS = false;
	public static final boolean DEFAULT_EDITOR_WIDE_CARET = false;
	
	//matching
	public static final String USE_MATCHING_BRACKETS = "USE_MATCHING_BRACKETS";
    public static final boolean DEFAULT_USE_MATCHING_BRACKETS = true;

    public static final String MATCHING_BRACKETS_COLOR = "EDITOR_MATCHING_BRACKETS_COLOR";
    public static final RGB DEFAULT_MATCHING_BRACKETS_COLOR = new RGB(64,128,128);
    
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
	
	public static final String STRING_COLOR = "STRING_COLOR";
	public static final RGB DEFAULT_STRING_COLOR = new RGB(0, 170, 0);
	
	public static final String COMMENT_COLOR = "COMMENT_COLOR";
	public static final RGB DEFAULT_COMMENT_COLOR = new RGB(192, 192, 192);
	
	public static final String BACKQUOTES_COLOR = "BACKQUOTES_COLOR";
	public static final RGB DEFAULT_BACKQUOTES_COLOR = new RGB(0, 0, 0);
	
	//see initializeDefaultColors for selection defaults
	public static final String CONNECT_TIMEOUT = "CONNECT_TIMEOUT";
	public static final int DEFAULT_CONNECT_TIMEOUT = 20000;

	//font
    public static final String DECORATOR_STYLE = "DECORATOR_STYLE";
	public static final int DEFAULT_DECORATOR_STYLE = SWT.ITALIC;

    public static final String NUMBER_STYLE = "NUMBER_STYLE";
	public static final int DEFAULT_NUMBER_STYLE = SWT.NORMAL;

    public static final String CODE_STYLE = "CODE_STYLE";
	public static final int DEFAULT_CODE_STYLE = SWT.NORMAL;
	
	public static final String KEYWORD_STYLE = "KEYWORD_STYLE";
	public static final int DEFAULT_KEYWORD_STYLE = SWT.NORMAL;
	
	public static final String STRING_STYLE = "STRING_STYLE";
	public static final int DEFAULT_STRING_STYLE = SWT.ITALIC;
	
	public static final String COMMENT_STYLE = "COMMENT_STYLE";
	public static final int DEFAULT_COMMENT_STYLE = SWT.NORMAL;
	
	public static final String BACKQUOTES_STYLE = "BACKQUOTES_STYLE";
	public static final int DEFAULT_BACKQUOTES_STYLE = SWT.BOLD;
		
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
		{"Backquotes", BACKQUOTES_COLOR, null},
	};
	
	private final String[][] fAppearanceFontListModel= new String[][] {
		{"Code", CODE_STYLE, null},
		{"Decorators", DECORATOR_STYLE, null},
		{"Numbers", NUMBER_STYLE, null},
		{"Matching brackets", MATCHING_BRACKETS_STYLE, null},
		{"Keywords", KEYWORD_STYLE, null},
		{"Strings", STRING_STYLE, null},
		{"Comments", COMMENT_STYLE, null},
		{"Backquotes", BACKQUOTES_STYLE, null},
	};
	
	private OverlayPreferenceStore fOverlayStore;
	
	private Map<Button, String> fCheckBoxes= Collections.checkedMap(new HashMap<Button, String>(), Button.class, String.class);
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};

	private Map<Text, String> fTextFields= Collections.checkedMap(new HashMap<Text, String>(), Text.class, String.class);
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			Text text= (Text) e.widget;
			fOverlayStore.setValue((String) fTextFields.get(text), text.getText());
		}
	};

	private java.util.List<Text> fNumberFields= Collections.checkedList(new ArrayList<Text>(), Text.class);
	private ModifyListener fNumberFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			numberFieldChanged((Text) e.widget);
		}
	};
	
	private List fAppearanceColorList;
	private ColorEditor fAppearanceColorEditor;
	private Button fAppearanceColorDefault;
	private Button fFontBoldCheckBox;
	private Button fFontItalicCheckBox;

	private SelectionListener fStyleCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
		public void widgetSelected(SelectionEvent e) {
			int i= fAppearanceColorList.getSelectionIndex();
			int style= SWT.NORMAL; 
			String styleKey= fAppearanceFontListModel[i][1];
			if (fFontBoldCheckBox.getSelection()) {
				style= style | SWT.BOLD;
			}
			if (fFontItalicCheckBox.getSelection()) {
				style= style | SWT.ITALIC;
			}
			fOverlayStore.setValue(styleKey, style);
		}
	};
	
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
	private java.util.List<SelectionListener> fMasterSlaveListeners= Collections.checkedList(new ArrayList<SelectionListener>(), SelectionListener.class);

	
	public PydevPrefs() {
		setDescription("Pydev Editor settings:"); 
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
		
		fOverlayStore= createOverlayStore();
	}
	
	private OverlayPreferenceStore createOverlayStore() {
		
		java.util.List<OverlayPreferenceStore.OverlayKey> overlayKeys= Collections.checkedList(
					new ArrayList<OverlayPreferenceStore.OverlayKey>(), 
					OverlayPreferenceStore.OverlayKey.class);
		
		//text
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, TAB_WIDTH));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AUTO_PAR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, MULTI_BLOCK_COMMENT_CHAR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, SINGLE_BLOCK_COMMENT_CHAR));
		
        //Auto eat colon and braces
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AUTO_COLON));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AUTO_BRACES));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AUTO_WRITE_IMPORT_STR));

        //matching
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_MATCHING_BRACKETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,  MATCHING_BRACKETS_COLOR));
		
		//checkbox		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, SUBSTITUTE_TABS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, GUESS_TAB_SUBSTITUTION));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_CODE_FOLDING));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET));
		
		//colors
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, CODE_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, NUMBER_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, DECORATOR_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, KEYWORD_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, STRING_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, COMMENT_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, BACKQUOTES_COLOR));
		
		//font style
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, CODE_STYLE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, NUMBER_STYLE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, DECORATOR_STYLE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, KEYWORD_STYLE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, STRING_STYLE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, COMMENT_STYLE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, BACKQUOTES_STYLE));
		
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
	}

	private void handleAppearanceColorListSelection() {	
		int i= fAppearanceColorList.getSelectionIndex();
		String key= fAppearanceColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fAppearanceColorEditor.setColorValue(rgb);
		String styleKey= fAppearanceFontListModel[i][1];
		int styleValue= fOverlayStore.getInt(styleKey);
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

        // simply a holder for the current reference for a Button, so you can input a tooltip
        Button b;
        

		addTextField(appearanceComposite, "Tab length:", TAB_WIDTH, 3, 0, true);

		addTextField(appearanceComposite, "Multi-block char:", MULTI_BLOCK_COMMENT_CHAR, 2, 0, false);
        
		addTextField(appearanceComposite, "Single-block char:", SINGLE_BLOCK_COMMENT_CHAR, 2, 0, false);
				
		//auto par
        b = addCheckBox(appearanceComposite, "Automatic parentheses insertion", AUTO_PAR, 0);
        b.setToolTipText(WordUtils.wrap("Enabling this option will enable automatic insertion of parentheses.  " +
                "Specifically, whenever you hit a brace such as '(', '{', or '[', its related peer will be inserted " +
                "and your cursor will be placed between the two braces.", TOOLTIP_WIDTH));
        
        //auto braces
        b = addCheckBox(appearanceComposite, "Automatically skip matching braces when typing", AUTO_BRACES, 0);
        b.setToolTipText(WordUtils.wrap("Enabling this option will enable automatically skipping matching braces " +
                "if you try to insert them.  For example, if you have the following code:\n\n" +
                "def function(self):\n\n" +
                "...with your cursor before the end parenthesis (after the 'f' in \"self\"), typing a ')' will " +
                "simply move the cursor to the position after the ')' without inserting a new one.", TOOLTIP_WIDTH));
        
        //auto colon
        b = addCheckBox(appearanceComposite, "Automatic colon detection", AUTO_COLON, 0);
        b.setToolTipText(WordUtils.wrap("Enabling this feature will enable the editor to detect if you are trying " +
                "to enter a colon which is already there.  Instead of inserting another colon, the editor will " +
                "simply move your cursor to the next position after the colon.", TOOLTIP_WIDTH));

        //auto import str
        b = addCheckBox(appearanceComposite, "Automatic insertion of the 'import' string on 'from xxx' ", AUTO_WRITE_IMPORT_STR, 0);
        b.setToolTipText(WordUtils.wrap("Enabling this will allow the editor to automatically write the" +
                "'import' string when you write a space after you've written 'from xxx '.", TOOLTIP_WIDTH));
        
        
		addCheckBox(appearanceComposite, "Substitute spaces for tabs?", SUBSTITUTE_TABS, 0);
		
		addCheckBox(appearanceComposite, "Assume tab spacing when files contain tabs?", GUESS_TAB_SUBSTITUTION, 0);
		
		addCheckBox(appearanceComposite, "Use code folding?", USE_CODE_FOLDING, 0);
		
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
		gd.heightHint= convertHeightInCharsToPixels(8);
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

		fFontBoldCheckBox = addStyleCheckBox(stylesComposite, "Bold");
		fFontItalicCheckBox = addStyleCheckBox(stylesComposite, "Italic");
		
		return appearanceComposite;
	}

	private Button addStyleCheckBox(Composite parent, String text) {
		Button result= new Button(parent, SWT.CHECK);
		result.setText(text);
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		result.setLayoutData(gd);
		result.addSelectionListener(fStyleCheckBoxListener);
		return result;
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
	 * @return the place where this plugin preferences are stored.
	 */
	public static Preferences getPreferences() {
		return 	PydevPlugin.getDefault().getPluginPreferences();
	}
	
	/**
	 * @return an array of strings with the available interpreters.
	 */
	public static String[] getInterpreters() {
		return PydevPlugin.getPythonInterpreterManager().getInterpreters();
	}

}