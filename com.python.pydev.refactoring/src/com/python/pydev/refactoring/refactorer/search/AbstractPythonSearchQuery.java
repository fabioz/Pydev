package com.python.pydev.refactoring.refactorer.search;

import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.ISearchQuery;

public abstract class AbstractPythonSearchQuery extends FileSearchQuery implements ISearchQuery{

    public AbstractPythonSearchQuery(String searchText) {
    	super(searchText, false, true, null);
    }
    public boolean canRerun() {
        return false;
    }

    public boolean canRunInBackground() {
        return true;
    }

    public String getLabel() {
        return "Python Search"; 
    }


    protected boolean isScopeAllFileTypes() {
        return false;
    }
    
    public abstract String getResultLabel(int nMatches);

}
