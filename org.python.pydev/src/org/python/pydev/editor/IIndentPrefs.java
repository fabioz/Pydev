/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;

/**
 * @author Fabio Zadrozny
 */
public interface IIndentPrefs {

    /**
     * @return True if we should substitute tabs for spaces.
     */
    public boolean getUseSpaces();
    //public void setUseSpaces(boolean useSpaces);
    
    /**
     * Sets the forceTabs preference for auto-indentation.
     * 
     * <p>
     * This is the preference that overrides "use spaces" preference when file
     * contains tabs (like mine do).
     * <p>
     * If the first indented line starts with a tab, then tabs override spaces.
     * 
     * @return True If tabs should be used even if it says we should use spaces.
     */
    public boolean getForceTabs();
    public void setForceTabs(boolean forceTabs);
    
    /**
     * @return the width a tab should have.
     */
    public int getTabWidth();
    //public void setTabWidth(int tabWidth);
    
    /**
     * @return the indentation string based on the current settings.
     */
	public String getIndentationString();

	/**
	 * Given the current settings, convert the current string to tabs or spaces.
	 */
	public void convertToStd(IDocument document, DocumentCommand command);
}
