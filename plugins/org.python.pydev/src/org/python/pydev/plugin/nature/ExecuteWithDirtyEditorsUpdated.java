/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author fabioz
 *
 */
public class ExecuteWithDirtyEditorsUpdated {

    private static int inExec = 0;
    
    /**
     * Note that it needs the UI thread, so, it doesn't need to be synchronized.
     */
    public static ArrayList<Tuple<IModulesManager, String>> start(){
        ArrayList<Tuple<IModulesManager, String>> pushed = new ArrayList<Tuple<IModulesManager, String>>();
        inExec += 1;
        
        if(inExec != 1){
            return pushed;
        }
        //Should only get here if we were inExec == 0 and went to inExec == 1.

        try {
            if(PydevPlugin.getDefault() == null){
                return pushed; //In tests
            }
            IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            IEditorReference[] editorReferences = workbenchWindow.getActivePage().getEditorReferences();
            for (IEditorReference iEditorReference : editorReferences) {
                IEditorPart editor = iEditorReference.getEditor(false);
                try {
                    if (editor != null) {
                        if (editor instanceof PyEdit) {
                            PyEdit edit = (PyEdit) editor;
                            IPythonNature pythonNature = edit.getPythonNature();
                            if (pythonNature != null) {
                                ICodeCompletionASTManager astManager = pythonNature.getAstManager();
                                if (astManager != null) {
                                    IModulesManager modulesManager = astManager.getModulesManager();
                                    if (modulesManager != null) {
                                        File editorFile = edit.getEditorFile();
                                        if (editorFile != null) {
                                            String resolveModule = pythonNature.resolveModule(editorFile);
                                            if (resolveModule != null) {
                                                pushed.add(
                                                        new Tuple<IModulesManager, String>(modulesManager, resolveModule));
                                                modulesManager.pushTemporaryModule(
                                                        resolveModule, 
                                                        new SourceModule(resolveModule, editorFile,
                                                        edit.getAST(), 
                                                        null)
                                                );
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        } catch (Throwable e) {
            //Yes, catch anything as we want to return any module that has already been pushed.
            Log.log(e);
        }
        return pushed;
    }

    public static void end(ArrayList<Tuple<IModulesManager, String>> pushed) {
        inExec -= 1;
        //undo any temporary push!
        for (Tuple<IModulesManager, String> push : pushed) {
            try {
                push.o1.popTemporaryModule(push.o2);
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

}
