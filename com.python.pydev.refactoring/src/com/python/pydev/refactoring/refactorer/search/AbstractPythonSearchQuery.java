package com.python.pydev.refactoring.refactorer.search;

import org.eclipse.search.ui.ISearchQuery;

public abstract class AbstractPythonSearchQuery implements ISearchQuery{

    protected String fSearchText;

    public AbstractPythonSearchQuery(String searchText) {
        fSearchText= searchText;
    }
    public boolean canRerun() {
        return true;
    }

    public boolean canRunInBackground() {
        return true;
    }

    public String getLabel() {
        return "Python Search"; 
    }

    public String getSearchString() {
        return fSearchText;
    }
    

    protected boolean isScopeAllFileTypes() {
        return false;
    }
    
    public abstract String getResultLabel(int nMatches);

}
