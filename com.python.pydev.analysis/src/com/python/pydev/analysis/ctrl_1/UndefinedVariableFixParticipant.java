/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis.ctrl_1;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class UndefinedVariableFixParticipant implements IAnalysisMarkersParticipant{

    public void addProps(IMarker marker, IAnalysisPreferences analysisPreferences, String line, PySelection ps, int offset, PyEdit edit, List<ICompletionProposal> props) throws BadLocationException, CoreException {
        Integer id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_PROBLEM_ID_MARKER_INFO);
        Integer start = (Integer) marker.getAttribute(IMarker.CHAR_START);
        Integer end = (Integer) marker.getAttribute(IMarker.CHAR_END);
        String markerContents = ps.getDoc().get(start, end-start);

        PythonNature nature = PythonNature.getPythonNature(edit.getProject());
        if(nature == null){
            return;
        }
        
        ProjectModulesManager projectModulesManager = nature.getAstManager().getProjectModulesManager();
        Set<String> allModules = projectModulesManager.getAllModuleNames();

        if(id == IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE){
            //when an undefined variable is found, we can:
            // - add an auto import (if it is a class or a method or some global attribute)
            // - declare it as a local or global variable
            // - change its name to some other global or local (mistyped)
            // - create a method or class for it (if it is a call)
            
            Set<String> mods = new HashSet<String>();
            //1. check if it is some module
            for (String completeName : allModules) {
                FullRepIterable iterable = new FullRepIterable(completeName);

                for (String mod : iterable) {
                    
                    String[] strings = FullRepIterable.headAndTail(mod);
                    String packageName = strings[0];
                    String realImportRep = "import "+strings[1];
                    String importRep = strings[1];
                    
                    if(importRep.equals(markerContents)){
                        String displayString = importRep;
                        if(packageName.length() > 0){
                            realImportRep = "from "+packageName+" "+realImportRep;
                            displayString += " - "+ packageName;
                        }
                        mods.add(realImportRep);
                    }
                }
            }
            int lineAvailableForImport = ps.getLineAvailableForImport();
            
            for (String string : mods) {
                props.add(new CtxInsensitiveImportComplProposal(
                        "",
                        offset,
                        0,
                        0,
                        null,
                        string,
                        null,
                        "",
                        IPyCompletionProposal.PRIORITY_LOCALS,
                        string,
                        lineAvailableForImport
                        ));
                System.out.println(string);
            }
        }
    }

    

}
