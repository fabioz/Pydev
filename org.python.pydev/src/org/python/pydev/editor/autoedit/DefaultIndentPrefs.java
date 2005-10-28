/*
 * Created on May 5, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.autoedit;

import org.python.pydev.plugin.PydevPrefs;

public class DefaultIndentPrefs extends AbstractIndentPrefs {
    /** 
     * Cache for indentation string 
     */
    private String indentString = null;

    private boolean useSpaces = PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS);

    private int tabWidth = PydevPrefs.getPreferences().getInt(PydevPrefs.TAB_WIDTH);

    public boolean getUseSpaces() {
        if(useSpaces != PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS)){
            useSpaces = PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS);
            regenerateIndetString();
        }
        return useSpaces;
    }

    public int getTabWidth() {
        if(tabWidth != PydevPrefs.getPreferences().getInt(PydevPrefs.TAB_WIDTH)){
            tabWidth = PydevPrefs.getPreferences().getInt(PydevPrefs.TAB_WIDTH);
            regenerateIndetString();
        }
        return tabWidth;
    }

    private void regenerateIndetString(){
        indentString = super.getIndentationString();
    }
    /**
     * This class also puts the indentation string in a cache and redoes it 
     * if the preferences are changed.
     * 
     * @return the indentation string. 
     */
    public String getIndentationString() {
        if (indentString == null){
            regenerateIndetString();
        }

        return indentString;
    }

    /** 
     * @see org.python.pydev.editor.autoedit.IIndentPrefs#getAutoParentesis()
     */
    public boolean getAutoParentesis() {
        return PydevPrefs.getPreferences().getBoolean(PydevPrefs.AUTO_PAR);
    }

    public boolean getAutoColon() {
        return PydevPrefs.getPreferences().getBoolean(PydevPrefs.AUTO_COLON);
    }

    public boolean getAutoBraces() {
        return PydevPrefs.getPreferences().getBoolean(PydevPrefs.AUTO_BRACES);
    }

    public boolean getAutoWriteImport() {
        return PydevPrefs.getPreferences().getBoolean(PydevPrefs.AUTO_WRITE_IMPORT_STR);
    }

    public boolean getSmartIndentPar() {
    	return PydevPrefs.getPreferences().getBoolean(PydevPrefs.SMART_INDENT_PAR);
    }

}