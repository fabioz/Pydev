package org.python.pydev.editor.autoedit;


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

    public TestIndentPrefs(boolean useSpaces, int tabWidth){
        this.useSpaces = useSpaces;
        this.tabWidth = tabWidth;
    }

    public TestIndentPrefs(boolean useSpaces, int tabWidth, boolean autoPar){
        this(useSpaces,tabWidth, autoPar, true);
    }

    public TestIndentPrefs(boolean useSpaces, int tabWidth, boolean autoPar, boolean autoElse){
        this(useSpaces,tabWidth);
        this.autoPar = autoPar;
        this.autoElse = autoElse;
    }
    
    public boolean getUseSpaces() {
        return useSpaces;
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

    public boolean getAutoBraces()
    {
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
    
    public void regenerateIndentString() {
        //ignore it
    }

}
