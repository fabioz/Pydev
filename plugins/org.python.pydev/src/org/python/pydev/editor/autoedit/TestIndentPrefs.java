/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.autoedit;

import org.python.pydev.core.ITabChangedListener;

/**
 * Code to be used in tests.
 */
public class TestIndentPrefs extends AbstractIndentPrefs {

    private boolean useSpaces;
    private int tabWidth;
    public boolean autoPar = true;
    public boolean autoColon = true;
    public boolean autoBraces = true;
    public boolean autoWriteImport = true;
    public boolean smartIndentAfterPar = true;
    public boolean autoAddSelf = true;
    public boolean autoElse;
    public boolean indentToParLevel = true;
    public int indentAfterParWidth = 1;
    public boolean autoAddLiterals = true;
    public boolean autoLink = true;
    public boolean tabStopInComment = false;

    public TestIndentPrefs(boolean useSpaces, int tabWidth) {
        this.useSpaces = useSpaces;
        this.tabWidth = tabWidth;
    }

    @Override
    public boolean getGuessTabSubstitution() {
        return false;
    }

    @Override
    public void addTabChangedListener(ITabChangedListener listener) {
        // No-op for testing.
    }

    public TestIndentPrefs(boolean useSpaces, int tabWidth, boolean autoPar) {
        this(useSpaces, tabWidth, autoPar, true);
    }

    public TestIndentPrefs(boolean useSpaces, int tabWidth, boolean autoPar, boolean autoElse) {
        this(useSpaces, tabWidth);
        this.autoPar = autoPar;
        this.autoElse = autoElse;
    }

    public boolean getUseSpaces(boolean considerForceTabs) {
        if (considerForceTabs && getForceTabs()) {
            return false;//force use tabs
        }
        return useSpaces;
    }

    public boolean getAutoLink() {
        return autoLink;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public boolean getAutoParentesis() {
        return autoPar;
    }

    public boolean getAutoColon() {
        return autoColon;
    }

    public boolean getAutoBraces() {
        return autoBraces;
    }

    public boolean getAutoWriteImport() {
        return autoWriteImport;
    }

    public boolean getSmartIndentPar() {
        return smartIndentAfterPar;
    }

    public boolean getAutoAddSelf() {
        return autoAddSelf;
    }

    public boolean getAutoDedentElse() {
        return autoElse;
    }

    public boolean getIndentToParLevel() {
        return indentToParLevel;
    }

    public int getIndentAfterParWidth() {
        return indentAfterParWidth;
    }

    public boolean getSmartLineMove() {
        return true;
    }

    public boolean getAutoLiterals() {
        return autoAddLiterals;
    }

    public boolean getTabStopInComment() {
        return tabStopInComment;
    }

    public void regenerateIndentString() {
        //ignore it
    }

}
