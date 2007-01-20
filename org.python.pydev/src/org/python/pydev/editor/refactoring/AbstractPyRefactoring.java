/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.IPropertyListener;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractPyRefactoring implements IPyRefactoring{

    /**
     * Instead of making all static, let's use a singleton... it may be useful...
     */
    private volatile static IPyRefactoring pyRefactoring;
    private volatile static IPyRefactoring defaultPyRefactoring;
    private List<IPropertyListener> propChangeListeners = new ArrayList<IPropertyListener>();
    private Tuple<IPyRefactoring, List<String>> lastRefactorResults;

    
    
    
    //---------------------------------------- static methods for handling multiple refactorers
    public static void firePropertiesChanged() {
        if(defaultPyRefactoring != null){
            defaultPyRefactoring.firePropertyChange(); 
        }
        if(pyRefactoring != null && pyRefactoring != defaultPyRefactoring){
            pyRefactoring.firePropertyChange();
        }
    }

    public static void restartShells() {
        if(defaultPyRefactoring != null){
           defaultPyRefactoring.restartShell(); 
        }
        if(pyRefactoring != null && pyRefactoring != defaultPyRefactoring){
            pyRefactoring.restartShell();
        }
    }

    public static void addPropertiesListener(IPropertyListener listener) {
        if(defaultPyRefactoring != null){
            defaultPyRefactoring.addPropertyListener(listener); 
        }
        if(pyRefactoring != null && pyRefactoring != defaultPyRefactoring){
            pyRefactoring.addPropertyListener(listener);
        }
    }
    //------------------------------------ end static methods for handling multiple refactorers


    
    /**
     * @return the default pyrefactoring (even if some other plugin contributes it).
     */
    public synchronized static IPyRefactoring getDefaultPyRefactoring(){
        if (AbstractPyRefactoring.defaultPyRefactoring == null){
            AbstractPyRefactoring.defaultPyRefactoring = new PyRefactoring();
        }
        return AbstractPyRefactoring.defaultPyRefactoring;
        
    }
    public synchronized static void setPyRefactoring(IPyRefactoring ref){
        AbstractPyRefactoring.pyRefactoring = ref;
    }
    
    /**
     * 
     * @return the pyrefactoring instance that is available (can be some plugin contribution). 
     */
    public synchronized static IPyRefactoring getPyRefactoring(){
        if (AbstractPyRefactoring.pyRefactoring == null){
        	IPyRefactoring r = (IPyRefactoring) ExtensionHelper.getParticipant(ExtensionHelper.PYDEV_REFACTORING);
        	if(r != null){
        		AbstractPyRefactoring.pyRefactoring = r;
        	}else{
        		//default one (provided by BRM)
                AbstractPyRefactoring.pyRefactoring = getDefaultPyRefactoring();
        	}
        }
        return AbstractPyRefactoring.pyRefactoring;
    }


    /**
     * 
     * @param lastRefactorResults: 
     * - First element should be an IPyRefactoring object
     * - Second element should be a collection with a list of strings pointing to the changed files.
     */
    public void setLastRefactorResults(Tuple<IPyRefactoring, List<String>> lastRefactorResults) {
        this.lastRefactorResults = lastRefactorResults;

        firePropertyChange();
        
    }

    public void firePropertyChange() {
        if(this.lastRefactorResults != null){
            for (Iterator iter = this.propChangeListeners.iterator(); iter.hasNext();) {
                IPropertyListener element = (IPropertyListener) iter.next();
                element.propertyChanged(this.lastRefactorResults, REFACTOR_RESULT_PROP);
            }
        }
    }

    public Tuple<IPyRefactoring, List<String>> getLastRefactorResults() {
        return lastRefactorResults;
    }


    public void addPropertyListener(IPropertyListener l) {
    	propChangeListeners.add(l);
    }


    public void checkAvailableForRefactoring(RefactoringRequest request) {
        PyRefactorAction.checkAvailableForRefactoring(request, pyRefactoring);
    }
    


}
