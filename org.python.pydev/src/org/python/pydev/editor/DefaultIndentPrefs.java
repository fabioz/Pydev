/*
 * Created on May 5, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.python.pydev.plugin.PydevPrefs;

class DefaultIndentPrefs extends AbstractIndentPrefs {
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

}