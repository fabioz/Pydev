/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.debug.newconsole.PydevConsole;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.dltk.console.ui.ScriptConsole;
import org.python.pydev.dltk.console.ui.internal.ScriptConsoleViewer;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.runners.SimpleRunner;

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
                
                ScriptConsole console = getActiveScriptConsole(PydevConsoleConstants.CONSOLE_TYPE);
                
                if(console == null){
                    //if no console is available, create it (if possible).
                    PydevConsoleFactory factory = new PydevConsoleFactory();
                    console = factory.createConsole();
                }
                
                if(console instanceof PydevConsole){
                    //ok, console available 
                    PydevConsole pydevConsole = (PydevConsole) console;
                    IDocument document = pydevConsole.getDocument();
                    
                    String code = selection.getTextSelection().getText();
                    if(code.length() != 0){
                        document.replace(document.getLength(), 0, code+"\n");
                    }else{
                        //no code available: do an execfile in the current context
                        File editorFile = this.edit.getEditorFile();
                        
                        if(editorFile != null){
                            String fileStr = SimpleRunner.getArgumentsAsStr(new String[]{editorFile.toString()});
                            
                            char[] characters = fileStr.trim().toCharArray();
                            FastStringBuffer buf = new FastStringBuffer(characters.length+characters.length/2);
                            for (int i = 0; i < characters.length; i++) {
                                char character= characters[i];
                                if (character == '\\') {
                                    buf.append("\\");
                                }
                                buf.append(character);
                            }
                            document.replace(document.getLength(), 0, StringUtils.format("execfile('%s')\n", buf.toString()));
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
		}
	}

	private static final String EVALUATE_ACTION_ID = "org.python.pydev.interactiveconsole.evaluateActionSetter";

	

    /**
     * @param consoleType the console type we're searching for
     * @return the currently active console.
     */
    private ScriptConsole getActiveScriptConsole(String consoleType) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                
                List<IViewPart> consoleParts = getConsoleParts(page, false);
                if(consoleParts.size() == 0){
                    consoleParts = getConsoleParts(page, true);
                }
                
                
                if (consoleParts.size() > 0) {
                    IConsoleView view = null;
                    long lastChangeMillis = Long.MIN_VALUE;
                    
                    if(consoleParts.size() == 1){
                        view = (IConsoleView) consoleParts.get(0);
                    }else{
                        //more than 1 view available
                        for(int i=0;i<consoleParts.size();i++){
                            IConsoleView temp = (IConsoleView) consoleParts.get(i);
                            IConsole console = temp.getConsole();
                            if(console instanceof PydevConsole){
                                PydevConsole tempConsole = (PydevConsole) console;
                                ScriptConsoleViewer viewer = tempConsole.getViewer();
                                
                                long tempLastChangeMillis = viewer.getLastChangeMillis();
                                if(tempLastChangeMillis > lastChangeMillis){
                                    lastChangeMillis = tempLastChangeMillis;
                                    view = temp;
                                }
                            }
                        }
                    }
                    
                    if(view != null){
                        IConsole console = view.getConsole();
    
                        if (console instanceof ScriptConsole && console.getType().equals(consoleType)) {
                            return (ScriptConsole) console;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param page the page where the console view is
     * @param restore whether we should try to restore it
     * @return a list with the parts containing the console
     */
    private List<IViewPart> getConsoleParts(IWorkbenchPage page, boolean restore) {
        List<IViewPart> consoleParts = new ArrayList<IViewPart>();
        
        IViewReference[] viewReferences = page.getViewReferences();
        for(IViewReference ref:viewReferences){
            if(ref.getId().equals(IConsoleConstants.ID_CONSOLE_VIEW)){
                IViewPart part = ref.getView(restore);
                if(part != null){
                    consoleParts.add(part);
                    if(restore){
                        return consoleParts;
                    }
                }
            }
        }
        return consoleParts;
    }


    /**
     * This method associates Ctrl+new line with the evaluation of commands in the console. 
     */
    public void onCreateActions(ListResourceBundle resources, final PyEdit edit, IProgressMonitor monitor) {
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

    
    public void onSave(PyEdit edit, IProgressMonitor monitor) {
        //ignore
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
        //ignore
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
        //ignore
    }



}
