/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;

/**
 * @author Fabio Zadrozny
 */
public interface IIndentPrefs {

    /**
     * @return True if we should substitute tabs for spaces.
     */
    public boolean getUseSpaces(boolean considerForceTabs);

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
    public void setForceTabs(boolean forceTabs);

    public boolean getForceTabs();

    /**
     * @return the width a tab should have.
     */
    public int getTabWidth();

    public void addTabChangedListener(ITabChangedListener listener);

    /**
     * @return the indentation string based on the current settings.
     */
    public String getIndentationString();

    /**
     * Given the current settings, convert the current string to tabs or spaces.
     */
    public void convertToStd(IDocument document, DocumentCommand command);

    /**
     * @return whether we should auto-close parentesis
     */
    public boolean getAutoParentesis();

    /**
     * Get whether or not to do colon detection.
     * @return true iff colon detection is turned on
     */
    public boolean getAutoColon();

    /**
     * Get whether or not to auto-skip braces insertion
     * @return if auto-skip braces is ENABLED
     */
    public boolean getAutoBraces();

    /**
     * Get whether we should auto-write 'import' if we are in a from xxx import fff
     */
    public boolean getAutoWriteImport();

    /**
     * Get whether we should smart-indent after a '('
     */
    public boolean getSmartIndentPar();

    /**
     * Get whether we should add 'self' automatically when declaring method
     */
    public boolean getAutoAddSelf();

    /**
     * Get whether we should auto-dedent 'else:'
     */
    public boolean getAutoDedentElse();

    /**
     * @return whether we should indent to a parenthesis level on auto-indent or only add 1 tab to the indent).
     */
    public boolean getIndentToParLevel();

    /**
     * @return indentation width after parenthesis if not indenting to a parenthesis (in number of tabs).
     */
    public int getIndentAfterParWidth();

    /**
     * Should be called to regenerate the indent string that's in the cache.
     */
    public void regenerateIndentString();

    /**
     * Should we make alt+up / alt+down considering indentation?
     */
    public boolean getSmartLineMove();

    /**
     * Should we close literals?
     */
    public boolean getAutoLiterals();

    /**
     * Allow tab stops in comments?
     */
    public boolean getTabStopInComment();

    /**
     * Should we do the link on auto-close?
     */
    public boolean getAutoLink();

    public boolean getGuessTabSubstitution();
}
