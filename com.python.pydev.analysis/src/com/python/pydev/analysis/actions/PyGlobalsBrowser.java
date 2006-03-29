package com.python.pydev.analysis.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class PyGlobalsBrowser extends PyAction{

	public void run(IAction action) {
		//check org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog2 (this is the class that does it for java)
		IPythonNature pythonNature = getPyEdit().getPythonNature();
        List<AbstractAdditionalInterpreterInfo> additionalInfo = new ArrayList<AbstractAdditionalInterpreterInfo>();
        if(pythonNature != null){
            additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfo(pythonNature);
        }else{
            //is null
            Tuple<SystemPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(getPyEdit().getEditorFile());
            if(infoForFile != null){
                AbstractAdditionalInterpreterInfo additionalSystemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(infoForFile.o1.getRelatedInterpreterManager());
                additionalInfo.add(additionalSystemInfo);
                pythonNature = infoForFile.o1;
            }else{
                PydevPlugin.log("Unable to get the information on where we are in the system (or the interpreter has not been well configured).");
                return;
            }
        }
		TwoPaneElementSelector dialog = new GlobalsTwoPaneElementSelector(getShell());
        dialog.setTitle("Pydev: Globals Browser");
        dialog.setMessage("Filter");
        List<IInfo> lst = new ArrayList<IInfo>();
        
        for(AbstractAdditionalInterpreterInfo info:additionalInfo){
            lst.addAll(info.getAllTokens());
        }
        dialog.setElements(lst.toArray());
        dialog.open();
        Object[] result = dialog.getResult();
        if(result != null && result.length > 0){
            IInfo entry = (IInfo) result[0];
            List<ItemPointer> pointers = new ArrayList<ItemPointer>();
            ICodeCompletionASTManager astManager = pythonNature.getAstManager();
            AnalysisPlugin.getDefinitionFromIInfo(pointers, astManager, pythonNature, entry);
            if(pointers.size() > 0){
                new PyOpenAction().run(pointers.get(0));
            }else{
                PydevPlugin.logInfo("Unable to find the location of the entry:"+entry);
                IModule module = astManager.getModule(entry.getDeclaringModuleName(), pythonNature, true);
                if(module != null && module.getFile() != null){
                    new PyOpenAction().run(new ItemPointer(module.getFile()));
                }
            }
        }

	}

}