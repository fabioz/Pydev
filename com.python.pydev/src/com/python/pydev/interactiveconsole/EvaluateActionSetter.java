/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.util.ListResourceBundle;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;

/**
 * This class will setup the editor so that we can evaluate commands on new lines.
 * 
 * It is as a 'singleton' for all PyEdit editors.
 */
public class EvaluateActionSetter implements IPyEditListener{
    
    private class EvaluateAction extends Action {
		private final PyEdit edit;

		private EvaluateAction(PyEdit edit) {
			super();
			this.edit = edit;
		}

		public  void run(){
		    try {
                PySelection selection = new PySelection(edit);
                String code = selection.getTextSelection().getText();
                try {
                    getConsoleEnv(edit.getProject(), edit).execute(code, selection.getEndLineDelim());
                } catch (UserCanceledException e) {
                    //ok
                }
            } catch (Exception e) {
                Log.log(e);
            }
		}
	}

	private static final String EVALUATE_ACTION_ID = "org.python.pydev.interactiveconsole.evaluateActionSetter";

    /**
     * As this class is a 'singleton', this means that we will only have 1 active console at any time (or at least
     * one for each type of interpreter: jython or python).
     */
    private Map<PyEdit, ConsoleEnv> fConsoleEnv = new WeakHashMap<PyEdit, ConsoleEnv>();

    /**
     * @return a console environment for a given project and editor. If it still does not exist or is
     * already terminated, a new console env will be created.
     * @throws UserCanceledException 
     */
    public synchronized ConsoleEnv getConsoleEnv(IProject project, PyEdit edit) throws UserCanceledException {
        try {
            ConsoleEnv consoleEnv = fConsoleEnv.get(edit);

            if (consoleEnv == null || consoleEnv.isTerminated()) {
                
                consoleEnv = new ConsoleEnv(project, edit.getIFile(), InteractiveConsolePreferencesPage.showConsoleInput(), 
                        edit.getPythonNature().getRelatedInterpreterManager(), edit);
                
                fConsoleEnv.put(edit, consoleEnv);
                
            }
            return consoleEnv;
            
        } catch (UserCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return whether we have an active console linked to some edit.
     */
    public boolean isConsoleEnvActive(PyEdit edit) {
        try {
            ConsoleEnv consoleEnv = fConsoleEnv.get(edit);
            boolean notNull = consoleEnv != null;
            boolean ret = notNull && !consoleEnv.isTerminated();
            if(notNull && ret == false){
                //it exists but is already terminated (so, let's remove it from the cache)
                consoleEnv = fConsoleEnv.remove(edit);
            }
            return ret;
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method associates Ctrl+new line with the evaluation of commands in the console. 
     */
    public void onCreateActions(ListResourceBundle resources, final PyEdit edit) {
        new PyEditConsoleListener(this, edit);
        final EvaluateAction evaluateAction = new EvaluateAction(edit);
        evaluateAction.setActionDefinitionId(EVALUATE_ACTION_ID);
        evaluateAction.setId(EVALUATE_ACTION_ID);
		Runnable runnable = new Runnable() {
			public void run() {
				if(!edit.isDisposed()){
					edit.setAction(EVALUATE_ACTION_ID, evaluateAction);
				}
			}
		};
		Display.getDefault().syncExec(runnable);
    }

    
    public void onSave(PyEdit edit) {
        //ignore
    }

    public void onDispose(PyEdit edit) {
        if(isConsoleEnvActive(edit)){
            ConsoleEnv env = fConsoleEnv.get(edit);
            if(env != null){
                env.terminate();
            }
        }
    }

    public void onSetDocument(IDocument document, PyEdit edit) {
        //ignore
    }



}
