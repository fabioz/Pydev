/*
 * Created on May 21, 2004
 *
 */
package org.python.pydev.editor.actions;

import java.io.File;
import java.util.HashSet;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 *  
 */
public class PyGoToDefinition extends PyRefactorAction {

    protected boolean areRefactorPreconditionsOK(RefactoringRequest request) {
        try {
            IPyRefactoring pyRefactoring = getPyRefactoring("canFindDefinition");
            pyRefactoring.checkAvailableForRefactoring(request);
        } catch (Exception e) {
        	e.printStackTrace();
            ErrorDialog.openError(null, "Error", "Unable to do requested action", 
                    new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, e.getMessage(), null));
            return false;
        }

        if (request.pyEdit.isDirty())
        	request.pyEdit.doSave(null);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        final Shell shell = getShell();
        try {

            ps = new PySelection(getTextEditor());
            final PyEdit pyEdit = getPyEdit();
            if(areRefactorPreconditionsOK(getRefactoringRequest())){

                HashSet<ItemPointer> set = new HashSet<ItemPointer>();
                ItemPointer[] defs = findDefinition(pyEdit);
                if(defs == null){
                	shell.getDisplay().beep();
                	return;
                }
                for (ItemPointer pointer : defs) {
                    set.add(pointer);
                }
                final ItemPointer[] where = set.toArray(new ItemPointer[0]);
    
                if (where == null) {
                	shell.getDisplay().beep();
                	return;
                }
    
                if (where.length > 0){
                    if (where.length == 1){
                        ItemPointer itemPointer = where[0];
                        doOpen(itemPointer, pyEdit);
                    }else{
                        //the user has to choose which is the correct definition...
                        final Display disp = shell.getDisplay();
                        disp.syncExec(new Runnable(){

                            public void run() {
                                ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new ILabelProvider(){

                                    public Image getImage(Object element) {
                                        return PyCodeCompletion.getImageForType(PyCodeCompletion.TYPE_PACKAGE);
                                    }

                                    public String getText(Object element) {
                                        ItemPointer pointer = (ItemPointer)element;
                                        File f = (File) (pointer).file;
                                        int line = pointer.start.line;
                                        return f.getName() + "  ("+f.getParent()+") - line:"+line;
                                    }

                                    public void addListener(ILabelProviderListener listener) {
                                    }

                                    public void dispose() {
                                    }

                                    public boolean isLabelProperty(Object element, String property) {
                                        return false;
                                    }

                                    public void removeListener(ILabelProviderListener listener) {
                                    }}
                                );
                                dialog.setTitle("Found matches");
                                dialog.setTitle("Select the one you believe matches most your search.");
                                dialog.setElements(where);
                                dialog.open();
                                Object[] result = dialog.getResult();
                                if(result != null && result.length > 0){
                                    doOpen((ItemPointer) result[0], pyEdit);

                                }
                            }
                            
                        });
                    }
                } else {
                    shell.getDisplay().beep();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        	PydevPlugin.log(e);
            String msg = e.getMessage();
            if(msg == null){
                msg = "Unable to get error msg";
            }
            ErrorDialog.openError(shell, "Error", "Unable to do requested action", 
                    new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, msg, e));
            
        }
    }


    /**
     * @param openAction
     * @param itemPointer
     * @param pyEdit 
     */
    private void doOpen(ItemPointer itemPointer, PyEdit pyEdit) {
        File f = (File) itemPointer.file;
        if (PythonPathHelper.isValidSourceFile(REF.getFileAbsolutePath(f))){
            final PyOpenAction openAction = (PyOpenAction) pyEdit.getAction(PyEdit.ACTION_OPEN);
            
            openAction.run(itemPointer);
        }else{
            MessageDialog.openInformation(getPyEditShell(), "Compiled Extension file", 
                    "The definition was found at: "+f.toString()+"\n" +
                    "(which cannot be opened because it is a compiled extension).");
        }
    }

    /**
     * @param node
     * @return
     */
    private ItemPointer[] findDefinition(PyEdit pyEdit) {
        IPyRefactoring pyRefactoring = getPyRefactoring("canFindDefinition");
        return pyRefactoring.findDefinition(getRefactoringRequest());
    }

    protected String perform(IAction action, String name, Operation operation) throws Exception {
        return null;
    }

    protected String getInputMessage() {
        return null;
    }

}