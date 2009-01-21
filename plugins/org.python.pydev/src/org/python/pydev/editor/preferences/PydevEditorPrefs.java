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

package org.python.pydev.editor.preferences;


import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.AbstractPydevPrefs;
import org.python.pydev.plugin.preferences.ColorEditor;


/**
 * The preference page for setting the editor options.
 * <p>
 * This class is internal and not intended to be used by clients.</p>
 */
public class PydevEditorPrefs extends AbstractPydevPrefs {


    public PydevEditorPrefs() {
        setDescription("Pydev editor appearance settings:\nNote: Pydev ignores the 'Insert spaces for tabs' in the general settings."); 
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        
        fOverlayStore= createOverlayStore();
    }

    protected Control createAppearancePage(Composite parent) {
        Composite appearanceComposite= new Composite(parent, SWT.NONE );
        GridLayout layout= new GridLayout(); 
        layout.numColumns= 2;
        appearanceComposite.setLayout(layout);

        
        addTextField(appearanceComposite, "Tab length:", TAB_WIDTH, 3, 0, true);
        
        addCheckBox(appearanceComposite, "Replace tabs with spaces when typing?", SUBSTITUTE_TABS, 0);
        
        
        addCheckBox(appearanceComposite, "Assume tab spacing when files contain tabs?", GUESS_TAB_SUBSTITUTION, 0);
        
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

}