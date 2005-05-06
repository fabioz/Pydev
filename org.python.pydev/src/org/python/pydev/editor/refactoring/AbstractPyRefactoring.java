/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.IPropertyListener;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractPyRefactoring implements IPyRefactoring{

    /**
     * Instead of making all static, let's use a singleton... it may be useful...
     */
    private static IPyRefactoring pyRefactoring;
    private List propChangeListeners = new ArrayList();
    private Object[] lastRefactorResults;

    public synchronized static IPyRefactoring getPyRefactoring(){
        if (AbstractPyRefactoring.pyRefactoring == null){
            AbstractPyRefactoring.pyRefactoring = new PyRefactoring();
        }
        return AbstractPyRefactoring.pyRefactoring;
    }

    public synchronized static void setPyRefactoring(IPyRefactoring pyRefactoring){
        if (AbstractPyRefactoring.pyRefactoring != null){
            AbstractPyRefactoring.pyRefactoring.killShell();
            AbstractPyRefactoring.pyRefactoring = null;
        }
        AbstractPyRefactoring.pyRefactoring = pyRefactoring;
    }

    public void setLastRefactorResults(Object[] lastRefactorResults) {
        if(lastRefactorResults.length != 2){
            throw new RuntimeException("Refactor Results should be a 2 elements tuple.");
        }
        if(lastRefactorResults[0] != null && !(lastRefactorResults[0] instanceof IPyRefactoring)){
            throw new RuntimeException("First argument should be an IPyRefactoring object");
        }
        if(lastRefactorResults[1] != null && !(lastRefactorResults[1] instanceof Collection)){
            throw new RuntimeException("Second argument should be a collection with a list of strings pointing to the changed files.");
        }
        this.lastRefactorResults = lastRefactorResults;

        for (Iterator iter = this.propChangeListeners.iterator(); iter.hasNext();) {
            IPropertyListener element = (IPropertyListener) iter.next();
            element.propertyChanged(this.lastRefactorResults, REFACTOR_RESULT_PROP);
        }
        
    }

    public Object []getLastRefactorResults() {
        return lastRefactorResults;
    }


    public void addPropertyListener(IPropertyListener l) {
    	propChangeListeners.add(l);
    }

}
