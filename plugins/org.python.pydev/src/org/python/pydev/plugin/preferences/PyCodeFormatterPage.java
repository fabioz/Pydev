/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.StyledTextForShowingCodeFactory;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeFormatterPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String FORMAT_BEFORE_SAVING = "FORMAT_BEFORE_SAVING";
    public static final boolean DEFAULT_FORMAT_BEFORE_SAVING = false;
    
    public static final String FORMAT_ONLY_CHANGED_LINES = "FORMAT_ONLY_CHANGED_LINES";
    public static final boolean DEFAULT_FORMAT_ONLY_CHANGED_LINES = false;
    
    
    public static final String TRIM_LINES = "TRIM_EMPTY_LINES";
    public static final boolean DEFAULT_TRIM_LINES = false;
    
    public static final String TRIM_MULTILINE_LITERALS = "TRIM_MULTILINE_LITERALS";
    public static final boolean DEFAULT_TRIM_MULTILINE_LITERALS = false;
    
    public static final String ADD_NEW_LINE_AT_END_OF_FILE = "ADD_NEW_LINE_AT_END_OF_FILE";
    public static final boolean DEFAULT_ADD_NEW_LINE_AT_END_OF_FILE = true;
    
    
    //a, b, c
    public static final String USE_SPACE_AFTER_COMMA = "USE_SPACE_AFTER_COMMA";
    public static final boolean DEFAULT_USE_SPACE_AFTER_COMMA = true;

    
    //call( a )
    public static final String USE_SPACE_FOR_PARENTESIS = "USE_SPACE_FOR_PARENTESIS";
    public static final boolean DEFAULT_USE_SPACE_FOR_PARENTESIS = false;
    
    
    //call(a = 1)
    public static final String USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS = "USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS";
    public static final boolean DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS = false;
    
    
    //operators =, !=, <, >, //, etc.
    public static final String USE_OPERATORS_WITH_SPACE = "USE_OPERATORS_WITH_SPACE";
    public static final boolean DEFAULT_USE_OPERATORS_WITH_SPACE = true;
    private StyledText labelExample;
    private BooleanFieldEditor spaceAfterComma;
    private BooleanFieldEditor onlyChangedLines;
    private BooleanFieldEditor spaceForParentesis;
    private BooleanFieldEditor assignWithSpaceInsideParentesis;
    private BooleanFieldEditor operatorsWithSpace;
    private BooleanFieldEditor rightTrimLines;
    private BooleanFieldEditor rightTrimMultilineLiterals;
    private BooleanFieldEditor addNewLineAtEndOfFile;
    private StyledTextForShowingCodeFactory formatAndStyleRangeHelper;
    

    public PyCodeFormatterPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(FORMAT_BEFORE_SAVING, "Auto-Format editor contents before saving?", p));
        
        onlyChangedLines = new BooleanFieldEditor(FORMAT_ONLY_CHANGED_LINES, "On save, only apply formatting in changed lines?", p);
        addField(onlyChangedLines);
        
        spaceAfterComma = new BooleanFieldEditor(USE_SPACE_AFTER_COMMA, "Use space after commas?", p);
        addField(spaceAfterComma);

        spaceForParentesis = new BooleanFieldEditor(USE_SPACE_FOR_PARENTESIS, "Use space before and after parenthesis?", p);
        addField(spaceForParentesis);
        
        assignWithSpaceInsideParentesis = new BooleanFieldEditor(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS, "Use space before and after assign for keyword arguments?", p);
        addField(assignWithSpaceInsideParentesis);
        
        operatorsWithSpace = new BooleanFieldEditor(USE_OPERATORS_WITH_SPACE, "Use space before and after operators? (+, -, /, *, //, **, etc.)", p);
        addField(operatorsWithSpace);
        
        rightTrimLines = new BooleanFieldEditor(TRIM_LINES, "Right trim lines?", p);
        addField(rightTrimLines);
        
        rightTrimMultilineLiterals = new BooleanFieldEditor(TRIM_MULTILINE_LITERALS, "Right trim multi-line string literals?", p);
        addField(rightTrimMultilineLiterals);
        
        addNewLineAtEndOfFile = new BooleanFieldEditor(ADD_NEW_LINE_AT_END_OF_FILE, "Add new line at end of file?", p);
        addField(addNewLineAtEndOfFile);
        
        formatAndStyleRangeHelper = new StyledTextForShowingCodeFactory();
        labelExample = formatAndStyleRangeHelper.createStyledTextForCodePresentation(p);
        
        updateLabelExample(PyFormatStd.getFormat());
    }

    private void updateLabelExample(FormatStd formatStd){
        String str= 
            "class Example(object):             \n"+
            "                                   \n"+
            "    def Call(self, param1=None):   \n"+
            "        '''docstring'''            \n"+
            "        return param1 + 10 * 10    \n"+
            "                                   \n"+
            "    def Call2(self):               \n"+
            "        #Comment                   \n"+
            "        return self.Call(param1=10)"+
            "";
        Tuple<String, StyleRange[]> result = formatAndStyleRangeHelper.formatAndGetStyleRanges(
                formatStd, str, PydevPrefs.getChainedPrefStore(), true);
        labelExample.setText(result.o1);
        labelExample.setStyleRanges(result.o2);
    }

    

    
    @Override
    public void propertyChange(PropertyChangeEvent event){
        super.propertyChange(event);
        FormatStd formatStd = new FormatStd();
        formatStd.assignWithSpaceInsideParens = this.assignWithSpaceInsideParentesis.getBooleanValue();
        formatStd.operatorsWithSpace = operatorsWithSpace.getBooleanValue();
        formatStd.parametersWithSpace = spaceForParentesis.getBooleanValue();
        formatStd.spaceAfterComma = spaceAfterComma.getBooleanValue();
        formatStd.addNewLineAtEndOfFile = addNewLineAtEndOfFile.getBooleanValue();
        formatStd.trimLines = rightTrimLines.getBooleanValue();
        formatStd.trimMultilineLiterals = rightTrimMultilineLiterals.getBooleanValue();
        updateLabelExample(formatStd);
    }


    
    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static boolean getFormatBeforeSaving() {
        return PydevPrefs.getPreferences().getBoolean(FORMAT_BEFORE_SAVING);
    }
    
    public static boolean getFormatOnlyChangedLines() {
        return PydevPrefs.getPreferences().getBoolean(FORMAT_ONLY_CHANGED_LINES);
    }
    
    public static boolean getAddNewLineAtEndOfFile() {
        return PydevPrefs.getPreferences().getBoolean(ADD_NEW_LINE_AT_END_OF_FILE);
    }
    
    public static boolean getTrimLines() {
        return PydevPrefs.getPreferences().getBoolean(TRIM_LINES);
    }
    
    public static boolean getTrimMultilineLiterals() {
        return PydevPrefs.getPreferences().getBoolean(TRIM_MULTILINE_LITERALS);
    }
    
    public static boolean useSpaceAfterComma() {
        return PydevPrefs.getPreferences().getBoolean(USE_SPACE_AFTER_COMMA);
    }

    public static boolean useSpaceForParentesis() {
        return PydevPrefs.getPreferences().getBoolean(USE_SPACE_FOR_PARENTESIS);
    }

    public static boolean useAssignWithSpacesInsideParenthesis() {
        return PydevPrefs.getPreferences().getBoolean(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS);
    }

    public static boolean useOperatorsWithSpace() {
        return PydevPrefs.getPreferences().getBoolean(USE_OPERATORS_WITH_SPACE);
    }

    @Override
    public void dispose(){
        super.dispose();
        formatAndStyleRangeHelper.dispose();
    }
    
    
    
    
        
}
