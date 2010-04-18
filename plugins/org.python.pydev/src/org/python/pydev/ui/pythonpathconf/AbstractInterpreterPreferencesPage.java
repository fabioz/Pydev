/*
 * Created on May 23, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractInterpreterPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    protected AbstractInterpreterEditor pathEditor;
	private boolean inApply = false;

    /**
     * Initializer sets the preference store
     */
    public AbstractInterpreterPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    protected abstract AbstractInterpreterEditor getInterpreterEditor(Composite p);
    
    /**
     * Creates a dialog that'll choose from a list of interpreter infos.
     */
    public static ListDialog createChooseIntepreterInfoDialog(
            IWorkbenchWindow workbenchWindow, IInterpreterInfo[] interpreters, String msg) {
        ListDialog listDialog = new ListDialog(workbenchWindow.getShell());
        listDialog.setContentProvider(new IStructuredContentProvider(){

            public Object[] getElements(Object inputElement) {
                if(inputElement instanceof IInterpreterInfo[]){
                    return (IInterpreterInfo[]) inputElement;
                }
                return new Object[0];
            }

            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                
            }}
        );
        listDialog.setLabelProvider(new LabelProvider(){
            public Image getImage(Object element) {
                return PydevPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON);
            }
            public String getText(Object element) {
                if(element != null && element instanceof IInterpreterInfo){
                    IInterpreterInfo info = (IInterpreterInfo) element;
                    return info.getNameForUI();
                }
                return super.getText(element);
            }
        });
        listDialog.setInput(interpreters);
        listDialog.setMessage(msg);
        return listDialog;
    }
    
    public void init(IWorkbench workbench) {
    }
    
    
    /**
     * Applies changes (if any) 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    protected void performApply() {
        this.inApply = true;
        try{
        	super.performApply(); //calls performOk()
        }finally{
        	this.inApply = false;
        }
    }
    

    /**
     * Restores the default values 
     *  
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        //don't do anything on defaults...
    }
    
    /**
     * Cancels any change
     *  
     * @see org.eclipse.jface.preference.IPreferencePage#performCancel()
     */
    public boolean performCancel() {
        return super.performCancel();
    }
    
    /**
     * Applies changes (if any)
     * 
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        //IMPORTANT: we must call the perform before restoring the modules because this
        //info is going to be used when restoring them.
        super.performOk();
        
        boolean editorChanged = pathEditor.checkChangedAndMarkUnchanged();
		if(editorChanged || inApply){
            if(restoreModules(editorChanged)){
	            
	            //When we call performOk, the editor is going to store its values, but after actually restoring the modules, we
	            //need to serialize the SystemModulesManager to be used when reloading the PydevPlugin
	            this.getInterpreterManager().saveInterpretersInfoModulesManager();
            }
        }
        
        
        return true;
    }

    
    /**
     * @return the interpreter manager associated to this page.
     */
    protected abstract IInterpreterManager getInterpreterManager();

    /**
     * Creates the editors - also provides a hook for getting a different interpreter editor
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        pathEditor = getInterpreterEditor(p);
        addField(pathEditor);
    }

    
    
    
    
    /**
     * @param defaultSelectedInterpreter this is the path to the default selected file (interpreter)
     * @param monitor a monitor to display the progress to the user.
     * @param interpreterNamesToRestore 
     */
    protected void doRestore(IProgressMonitor monitor, Set<String> interpreterNamesToRestore) {
        IInterpreterManager iMan = getInterpreterManager();
        iMan.restorePythopathForInterpreters(monitor, interpreterNamesToRestore);
        
        //We also need to restart our code-completion shell after doing that, as we may have new environment variables!
        //And in jython, changing the classpath also needs to restore it.
        for(IInterpreterInfo interpreter:iMan.getInterpreterInfos()){
            AbstractShell.stopServerShell(interpreter, AbstractShell.COMPLETION_SHELL);
        }
    }

    
    /**
     * all the information should be cleared but the related to the interpreters passed
     * @param allButTheseInterpreters
     * @param monitor
     */
    protected void setInfos(final List<IInterpreterInfo> allButTheseInterpreters, IProgressMonitor monitor){
        IInterpreterManager iMan = getInterpreterManager();
        iMan.setInfos(allButTheseInterpreters);
    }

    /**
     * Restores the modules. Is called when the user changed something in the editor and applies the change.
     * 
     * Gathers all the info and calls the hook that really restores things within a thread, so that the user can 
     * get information on the progress.
     * 
     * Only the information on the default interpreter is stored.
     * 
     * @param editorChanged whether the editor was changed (if it wasn't, we'll ask the user what to restore). 
     * @return true if the info was restored and false otherwise.
     */
    protected boolean restoreModules(boolean editorChanged) {
    	final Set<String> interpreterNamesToRestore = pathEditor.getInterpreterNamesToRestoreAndClear();
        final IInterpreterInfo[] exesList = pathEditor.getExesList();
        
        if(!editorChanged && interpreterNamesToRestore.size() == 0){
        	IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        	ListDialog listDialog = createChooseIntepreterInfoDialog(workbenchWindow, exesList, "Select interpreters to be restored");
        	
            int open = listDialog.open();
            if(open != ListDialog.OK){
                return false;
            }
            Object[] result = (Object[]) listDialog.getResult();
            if(result == null || result.length == 0){
                return false;
                
            }
            for(Object o:result){
            	interpreterNamesToRestore.add(((IInterpreterInfo)o).getName());
            }
        	
        }
        
        //this is the default interpreter
        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(this.getShell());
        monitorDialog.setBlockOnOpen(false);

        try {
            IRunnableWithProgress operation = new IRunnableWithProgress(){

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
                    //clear all but the ones that appear
                    setInfos(Arrays.asList(exesList), monitor);
                    
                    //restore the default
                    doRestore(monitor, interpreterNamesToRestore);
                    monitor.done();
                }};
                
            monitorDialog.run(true, true, operation);
            
        }catch (Exception e) {
            PydevPlugin.log(e);
        }            
        return true;
    }

    
    


}
