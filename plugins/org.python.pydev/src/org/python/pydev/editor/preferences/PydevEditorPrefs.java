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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.StyledTextForShowingCodeFactory;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.AbstractPydevPrefs;
import org.python.pydev.plugin.preferences.ColorEditor;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.shared_ui.word_boundaries.SubWordPreferences;

/**
 * The preference page for setting the editor options.
 * <p>
 * This class is internal and not intended to be used by clients.</p>
 */
public class PydevEditorPrefs extends AbstractPydevPrefs {

    private static final String WORD_NAVIGATION_NATIVE_CAPTION = "Native";

    private static final String WORD_NAVIGATION_SUBWORD_CAPTION = "Subword";

    /**
     * Shows sample code with the new preferences.
     */
    private StyledText labelExample;

    /**
     * A local store that has the preferences set given the user configuration of colors.
     */
    private final IPreferenceStore localStore;

    /**
     * Helper to create the styled text and show the code later.
     */
    private StyledTextForShowingCodeFactory formatAndStyleRangeHelper;

    private IPropertyChangeListener updateLabelExampleOnPrefsChanges;

    private Combo comboNavigation;

    public PydevEditorPrefs() {
        setDescription("PyDev editor appearance settings");
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());

        fOverlayStore = createOverlayStore();
        localStore = new PreferenceStore();
    }

    @Override
    protected void initialize() {
        super.initialize();
        String caption = WORD_NAVIGATION_SUBWORD_CAPTION;
        if (fOverlayStore.getString(SubWordPreferences.WORD_NAVIGATION_STYLE)
                .equals(SubWordPreferences.WORD_NAVIGATION_STYLE_NATIVE)) {
            caption = WORD_NAVIGATION_NATIVE_CAPTION;
        }
        comboNavigation.setText(caption);
    }

    @Override
    protected Control createAppearancePage(Composite parent) {
        GridData gridData;
        Composite appearanceComposite = new Composite(parent, SWT.NONE);
        Composite wordNavigationComposite = new Composite(appearanceComposite, SWT.NONE);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.grabExcessHorizontalSpace = true;
        wordNavigationComposite.setLayoutData(gridData);

        GridLayout wordNavigationLayout = new GridLayout();
        wordNavigationLayout.marginWidth = 0;
        wordNavigationLayout.marginRight = 5;
        wordNavigationLayout.numColumns = 2;
        wordNavigationComposite.setLayout(wordNavigationLayout);

        Label label = new Label(wordNavigationComposite, SWT.NONE);
        label.setText("Word navigation");
        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = false;
        label.setLayoutData(gridData);

        comboNavigation = new Combo(wordNavigationComposite, SWT.CHECK);
        comboNavigation.add(WORD_NAVIGATION_SUBWORD_CAPTION);
        comboNavigation.add(WORD_NAVIGATION_NATIVE_CAPTION);
        comboNavigation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String text = comboNavigation.getText();
                String style = SubWordPreferences.WORD_NAVIGATION_STYLE_SUBWORD;
                if (WORD_NAVIGATION_NATIVE_CAPTION.equals(text)) {
                    style = SubWordPreferences.WORD_NAVIGATION_STYLE_NATIVE;
                }
                fOverlayStore.setValue(SubWordPreferences.WORD_NAVIGATION_STYLE, style);
            }
        });
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.grabExcessHorizontalSpace = true;
        comboNavigation.setLayoutData(gridData);

        createColorOptions(appearanceComposite);

        Composite exampleComposite = new Composite(appearanceComposite, SWT.NONE);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.grabExcessHorizontalSpace = true;
        exampleComposite.setLayoutData(gridData);
        GridLayout exampleGridLayout = new GridLayout();
        exampleGridLayout.marginWidth = 0;
        exampleGridLayout.marginRight = 5;
        exampleComposite.setLayout(exampleGridLayout);
        formatAndStyleRangeHelper = new StyledTextForShowingCodeFactory();
        labelExample = formatAndStyleRangeHelper.createStyledTextForCodePresentation(exampleComposite);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.grabExcessHorizontalSpace = true;
        labelExample.setLayoutData(gridData);
        updateLabelExample(PyFormatStd.getFormat(null), PydevPrefs.getChainedPrefStore());

        LinkFieldEditor tabsFieldEditor = new LinkFieldEditor("UNUSED",
                "Other settings:\n\n<a>Tabs</a>: tab preferences for PyDev ...\n(note: 'Insert spaces for tabs' in the general settings is ignored).",
                appearanceComposite,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.python.pydev.editor.preferences.PyTabPreferencesPage";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        tabsFieldEditor.getLinkControl(appearanceComposite);

        LinkFieldEditor colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED",
                "<a>Text Editors</a>: print margin, line numbers ...", appearanceComposite,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.eclipse.ui.preferencePages.GeneralTextEditor";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(appearanceComposite);

        colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED",
                "<a>Colors and Fonts</a>: text font, content assist color ...", appearanceComposite,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.eclipse.ui.preferencePages.ColorsAndFonts";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(appearanceComposite);

        colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED", "<a>Annotations</a>: occurrences, markers ...",
                appearanceComposite, new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.eclipse.ui.editors.preferencePages.Annotations";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(appearanceComposite);

        return appearanceComposite;
    }

    private void createColorOptions(Composite appearanceComposite) {
        GridLayout layout;
        Label l = new Label(appearanceComposite, SWT.LEFT);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        gd.heightHint = convertHeightInCharsToPixels(1) / 2;
        l.setLayoutData(gd);

        l = new Label(appearanceComposite, SWT.LEFT);
        l.setText("Appearance color options:");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);

        Composite editorComposite = new Composite(appearanceComposite, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 2;
        layout.marginWidth = 0;
        editorComposite.setLayout(layout);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        editorComposite.setLayoutData(gd);

        fAppearanceColorList = new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = convertHeightInCharsToPixels(8);
        gd.grabExcessHorizontalSpace = true;
        fAppearanceColorList.setLayoutData(gd);

        Composite stylesComposite = new Composite(editorComposite, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        stylesComposite.setLayout(layout);

        l = new Label(stylesComposite, SWT.LEFT);
        l.setText("Color:");
        gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        l.setLayoutData(gd);

        fAppearanceColorEditor = new ColorEditor(stylesComposite);
        Button foregroundColorButton = fAppearanceColorEditor.getButton();
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.BEGINNING;
        foregroundColorButton.setLayoutData(gd);

        SelectionListener colorDefaultSelectionListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean systemDefault = fAppearanceColorDefault.getSelection();
                fAppearanceColorEditor.getButton().setEnabled(!systemDefault);

                int i = fAppearanceColorList.getSelectionIndex();
                String key = fAppearanceColorListModel[i][2];
                if (key != null) {
                    fOverlayStore.setValue(key, systemDefault);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };

        fAppearanceColorDefault = new Button(stylesComposite, SWT.CHECK);
        fAppearanceColorDefault.setText("System default");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.BEGINNING;
        gd.horizontalSpan = 2;
        fAppearanceColorDefault.setLayoutData(gd);
        fAppearanceColorDefault.setVisible(false);
        fAppearanceColorDefault.addSelectionListener(colorDefaultSelectionListener);

        fAppearanceColorList.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // do nothing
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAppearanceColorListSelection();
            }
        });
        foregroundColorButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // do nothing
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                int i = fAppearanceColorList.getSelectionIndex();
                String key = fAppearanceColorListModel[i][1];

                PreferenceConverter.setValue(fOverlayStore, key, fAppearanceColorEditor.getColorValue());
                onAppearanceRelatedPreferenceChanged();
            }
        });

        fFontBoldCheckBox = addStyleCheckBox(stylesComposite, "Bold");
        fFontItalicCheckBox = addStyleCheckBox(stylesComposite, "Italic");
    }

    public void updateLabelExample(FormatStd formatStd, IPreferenceStore store) {
        if (labelExample != null && !labelExample.isDisposed()) {
            String str = "class Example(object):\n" +
                    "\n" +
                    "    backquotes = `backquotes`\n" +
                    "\n" +
                    "    @memoize(size=10)\n" +
                    "    def Call(self, param1=None):\n" +
                    "        u'''unicode'''\n" +
                    "        return param1 + 10 * 10\n" +
                    "\n" +
                    "    def Call2(self):\n" +
                    "        b'''bytes'''\n" +
                    "        #Comment\n" +
                    "        return self.Call(param1=10)" +
                    "";
            Tuple<String, StyleRange[]> result = formatAndStyleRangeHelper.formatAndGetStyleRanges(formatStd, str,
                    store, false);
            labelExample.setText(result.o1);
            labelExample.setStyleRanges(result.o2);
        }
    }

    @Override
    protected void onAppearanceRelatedPreferenceChanged() {
        localStore.setValue(KEYWORD_COLOR, fOverlayStore.getString(KEYWORD_COLOR));
        localStore.setValue(SELF_COLOR, fOverlayStore.getString(SELF_COLOR));
        localStore.setValue(CODE_COLOR, fOverlayStore.getString(CODE_COLOR));
        localStore.setValue(DECORATOR_COLOR, fOverlayStore.getString(DECORATOR_COLOR));
        localStore.setValue(NUMBER_COLOR, fOverlayStore.getString(NUMBER_COLOR));
        localStore.setValue(FUNC_NAME_COLOR, fOverlayStore.getString(FUNC_NAME_COLOR));
        localStore.setValue(CLASS_NAME_COLOR, fOverlayStore.getString(CLASS_NAME_COLOR));
        localStore.setValue(STRING_COLOR, fOverlayStore.getString(STRING_COLOR));
        localStore.setValue(UNICODE_COLOR, fOverlayStore.getString(UNICODE_COLOR));
        localStore.setValue(COMMENT_COLOR, fOverlayStore.getString(COMMENT_COLOR));
        localStore.setValue(BACKQUOTES_COLOR, fOverlayStore.getString(BACKQUOTES_COLOR));
        localStore.setValue(PARENS_COLOR, fOverlayStore.getString(PARENS_COLOR));
        localStore.setValue(OPERATORS_COLOR, fOverlayStore.getString(OPERATORS_COLOR));
        localStore.setValue(DOCSTRING_MARKUP_COLOR, fOverlayStore.getString(DOCSTRING_MARKUP_COLOR));

        localStore.setValue(KEYWORD_STYLE, fOverlayStore.getInt(KEYWORD_STYLE));
        localStore.setValue(SELF_STYLE, fOverlayStore.getInt(SELF_STYLE));
        localStore.setValue(CODE_STYLE, fOverlayStore.getInt(CODE_STYLE));
        localStore.setValue(DECORATOR_STYLE, fOverlayStore.getInt(DECORATOR_STYLE));
        localStore.setValue(NUMBER_STYLE, fOverlayStore.getInt(NUMBER_STYLE));
        localStore.setValue(FUNC_NAME_STYLE, fOverlayStore.getInt(FUNC_NAME_STYLE));
        localStore.setValue(CLASS_NAME_STYLE, fOverlayStore.getInt(CLASS_NAME_STYLE));
        localStore.setValue(STRING_STYLE, fOverlayStore.getInt(STRING_STYLE));
        localStore.setValue(UNICODE_STYLE, fOverlayStore.getInt(UNICODE_STYLE));
        localStore.setValue(COMMENT_STYLE, fOverlayStore.getInt(COMMENT_STYLE));
        localStore.setValue(BACKQUOTES_STYLE, fOverlayStore.getInt(BACKQUOTES_STYLE));
        localStore.setValue(PARENS_STYLE, fOverlayStore.getInt(PARENS_STYLE));
        localStore.setValue(OPERATORS_STYLE, fOverlayStore.getInt(OPERATORS_STYLE));
        localStore.setValue(DOCSTRING_MARKUP_STYLE, fOverlayStore.getInt(DOCSTRING_MARKUP_STYLE));

        this.updateLabelExample(PyFormatStd.getFormat(null), localStore);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (formatAndStyleRangeHelper != null) {
            formatAndStyleRangeHelper.dispose();
            formatAndStyleRangeHelper = null;
        }
        if (updateLabelExampleOnPrefsChanges != null) {
            PydevPrefs.getChainedPrefStore().removePropertyChangeListener(updateLabelExampleOnPrefsChanges);
            updateLabelExampleOnPrefsChanges = null;
        }
        if (labelExample != null) {
            try {
                labelExample.dispose();
            } catch (Exception e) {
                Log.log(e);
            }
            labelExample = null;
        }
    }

    public void setUpdateLabelExampleOnPrefsChanges() {
        updateLabelExampleOnPrefsChanges = new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                RunInUiThread.async(new Runnable() {

                    @Override
                    public void run() {
                        updateLabelExample(PyFormatStd.getFormat(null), PydevPrefs.getChainedPrefStore());
                    }
                });
            }
        };
        PydevPrefs.getChainedPrefStore().addPropertyChangeListener(updateLabelExampleOnPrefsChanges);

    }
}