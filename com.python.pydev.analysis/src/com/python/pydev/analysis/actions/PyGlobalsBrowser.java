package com.python.pydev.analysis.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class PyGlobalsBrowser extends PyAction{

	public void run(IAction action) {
		//check org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog2 (this is the class that does it for java)
        
		IPythonNature pythonNature = getPyEdit().getPythonNature();
        PySelection ps = new PySelection(this.getPyEdit());
        String selectedText = ps.getSelectedText();

        if(pythonNature != null){
            Tuple<List<AbstractAdditionalInterpreterInfo>, List<IPythonNature>> tup = AdditionalProjectInterpreterInfo.getAdditionalInfoAndNature(pythonNature, true, true);
            List<AbstractAdditionalInterpreterInfo> additionalInfo = tup.o1;
            doSelect(tup.o2, additionalInfo, selectedText);
            
        }else{
            getFromSystemManager(selectedText);
        }
        

	}

    /**
     * @param selectedText the text that should be selected in the begginning (may be null)
     */
    public void getFromSystemManager(String selectedText) {
        List<AbstractAdditionalInterpreterInfo> additionalInfo = new ArrayList<AbstractAdditionalInterpreterInfo>();
        List<IPythonNature> pythonNatures = new ArrayList<IPythonNature>();
        //is null
        Tuple<SystemPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(getPyEdit().getEditorFile());
        if(infoForFile != null){
            IPythonNature systemPythonNature = infoForFile.o1;
            if(systemPythonNature == null){
                return;
            }
            
            AbstractAdditionalInterpreterInfo additionalSystemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(infoForFile.o1.getRelatedInterpreterManager());
            if(additionalSystemInfo == null){
                return;
            }
            additionalInfo.add(additionalSystemInfo);
            pythonNatures.add(systemPythonNature);
            doSelect(pythonNatures, additionalInfo, selectedText);
            
        }else{
            getFromWorkspace(selectedText);
        }
    }

    /**
     * This method will check if the user has python and/or the jython interpreter configured. If it has only
     * one of those, it will get the info for it and the related projects.
     * 
     * If both are configured, default is python
     * 
     * If none is configured, it will show an error saying so.
     *  
     * @param selectedText the text that should be initally set as the filter
     */
    public static void getFromWorkspace(String selectedText) {
        IInterpreterManager pyManager = PydevPlugin.getPythonInterpreterManager();
        IInterpreterManager jyManager = PydevPlugin.getJythonInterpreterManager();
        IInterpreterManager useManager = null;
        if(pyManager.isConfigured()){
            //default is python, so that's it
            useManager = pyManager;
        }else if(jyManager.isConfigured()){
            //ok, no python... go for jython
            useManager = jyManager;
        }
        
        if(useManager == null){
            MessageDialog.openError(getShell(), "No configured manager", "Neither the python nor the jython\ninterpreter is configured.");
            return;
        }
        
        AbstractAdditionalInterpreterInfo additionalSystemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(useManager);
        if(additionalSystemInfo == null){
            MessageDialog.openError(getShell(), "Error", "Additional info is null.");
            return;
        }
        
        List<AbstractAdditionalInterpreterInfo> additionalInfo = new ArrayList<AbstractAdditionalInterpreterInfo>();
        additionalInfo.add(additionalSystemInfo);
        
        List<IPythonNature> natures = PythonNature.getPythonNaturesRelatedTo(useManager.getRelatedId());
        for (IPythonNature nature : natures) {
            AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
            if(info != null){
                additionalInfo.add(info);
            }
        }
        doSelect(natures, additionalInfo, selectedText);
        
    }

    /**
     * @param pythonNatures the natures from were we can get info
     * @param additionalInfo the additional informations 
     * @param selectedText the text that should be initially set as a filter
     */
    public static void doSelect(List<IPythonNature> pythonNatures, List<AbstractAdditionalInterpreterInfo> additionalInfo, String selectedText) {
        TwoPaneElementSelector dialog = new GlobalsTwoPaneElementSelector(getShell());
        dialog.setTitle("Pydev: Globals Browser");
        dialog.setMessage("Filter");
        if(selectedText != null && selectedText.length() > 0){
            dialog.setFilter(selectedText);
        }
        
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
            
            for(IPythonNature pythonNature:pythonNatures){
                //try to find in one of the natures...
                ICodeCompletionASTManager astManager = pythonNature.getAstManager();
                if(astManager == null){
                	return;
                }
                AnalysisPlugin.getDefinitionFromIInfo(pointers, astManager, pythonNature, entry);
                if(pointers.size() > 0){
                    new PyOpenAction().run(pointers.get(0));
                    return; //don't check the other natures
                }
            }
        }
    }

}