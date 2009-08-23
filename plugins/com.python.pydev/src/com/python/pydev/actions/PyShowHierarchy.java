package com.python.pydev.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;
import com.python.pydev.ui.hierarchy.PyHierarchyView;

/**
 * 
 * Based on 
 * org.eclipse.jdt.ui.actions.OpenTypeHierarchyAction
 * org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil
 * @author fabioz
 *
 */
public class PyShowHierarchy extends PyRefactorAction{


    IPyRefactoring pyRefactoring;
    /**
     * @return
     */
    protected IPyRefactoring getPyRefactoring() {
        if(pyRefactoring == null){
            pyRefactoring = AbstractPyRefactoring.getPyRefactoring(); //the one provided by the com part (no need to check for it).
        }
        return pyRefactoring;
    }

    private PyHierarchyView view;
    private HierarchyNodeModel model;

    @Override
    protected String perform(IAction action, String name, IProgressMonitor monitor) throws Exception {
        Runnable r = new Runnable() {
            public void run() {
                IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                try {
                    IWorkbenchPage page= workbenchWindow.getActivePage();
                    view = (PyHierarchyView) page.showView("com.python.pydev.ui.hierarchy.PyHierarchyView", null, IWorkbenchPage.VIEW_VISIBLE);
                    
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        };
        
        Display.getDefault().syncExec(r); //sync it here, so that we can check it later...
        if(view != null){
            //set whatever is needed for the hierarchy
            IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
            if(pyRefactoring instanceof IPyRefactoring2){
                RefactoringRequest refactoringRequest = getRefactoringRequest(monitor);
                IPyRefactoring2 r2 = (IPyRefactoring2) pyRefactoring;
                model = r2.findClassHierarchy(refactoringRequest);
            }
            if(model != null){
                
                r = new Runnable() {
                    public void run() {
                        view.setHierarchy(model);
                    }
                };
                Display.getDefault().asyncExec(r); //this can be 'not synched'
            }
        }
        return "";
    }

    @Override
    protected String getInputMessage() {
        return null;
    }

}
