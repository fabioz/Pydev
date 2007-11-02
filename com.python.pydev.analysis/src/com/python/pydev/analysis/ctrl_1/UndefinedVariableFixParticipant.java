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
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.analysis.builder.AnalysisParserObserver;
import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;

public class UndefinedVariableFixParticipant implements IAnalysisMarkersParticipant{

    public void addProps(IMarker marker, 
            IAnalysisPreferences analysisPreferences, 
            String line, 
            PySelection ps, 
            int offset, 
            IPythonNature nature,
            PyEdit edit, 
            List<ICompletionProposal> props) throws BadLocationException, CoreException {
        
        Integer id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        if(id != IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE){
            return;
        }
        
        Integer start = (Integer) marker.getAttribute(IMarker.CHAR_START);
        Integer end = (Integer) marker.getAttribute(IMarker.CHAR_END);
        ps.setSelection(start, end);
        String markerContents = ps.getSelectedText();
        String fullRep = ps.getFullRepAfterSelection();
        
        ImageCache imageCache = PydevPlugin.getImageCache();
        Image importImage = null;
        if(imageCache != null){ //making tests
            importImage = imageCache.get(UIConstants.IMPORT_ICON);
        }
        IModulesManager projectModulesManager = nature.getAstManager().getModulesManager();
        Set<String> allModules = projectModulesManager.getAllModuleNames(true);

        //when an undefined variable is found, we can:
        // - add an auto import (if it is a class or a method or some global attribute)
        // - declare it as a local or global variable
        // - change its name to some other global or local (mistyped)
        // - create a method or class for it (if it is a call)
        
        Set<Tuple<String,String>> mods = new HashSet<Tuple<String,String>>();
        //1. check if it is some module
        for (String completeName : allModules) {
            FullRepIterable iterable = new FullRepIterable(completeName);

            for (String mod : iterable) {
                
                if(fullRep.startsWith(mod)){
                    
                    if(fullRep.length() == mod.length() //it does not only start with, but it is equal to it.
                       || (fullRep.length() > mod.length() && fullRep.charAt(mod.length()) == '.')
                       ){ 
                        mods.add(new Tuple<String,String>(new StringBuffer("import ").append(mod).toString(), 
                                new StringBuffer("Import ").append(mod).toString()));
                    }
                }
                
                String[] strings = FullRepIterable.headAndTail(mod);
                String packageName = strings[0];
                String importRep = strings[1];
                
                if(importRep.equals(markerContents)){
                    if(packageName.length() > 0){
                        String realImportRep = new StringBuffer("from ").append(packageName).append(" ").append("import ").append(strings[1]).toString();
                        String displayString = new StringBuffer("Import ").append(importRep).append(" (").append(packageName).append(")").toString();
                        mods.add(new Tuple<String,String>(realImportRep, displayString));
                        
                    }else{
                        String displayString = new StringBuffer("Import ").append(importRep).toString();
                        String realImportRep = new StringBuffer("import ").append(strings[1]).toString();
                        mods.add(new Tuple<String,String>(realImportRep, displayString));
                    }
                }
            }
        }
        
        
        //2. check if it is some global class or method
        List<AbstractAdditionalInterpreterInfo> additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfo(nature);
        for (AbstractAdditionalInterpreterInfo info : additionalInfo) {
            List<IInfo> tokensEqualTo = info.getTokensEqualTo(markerContents, AbstractAdditionalInterpreterInfo.TOP_LEVEL);
            for (IInfo found : tokensEqualTo) {
                //there always is a declaring module
                String name = found.getName();
                String declPackage = found.getDeclaringModuleName();
                String declPackageWithoutInit = declPackage;
                if(declPackageWithoutInit.endsWith(".__init__")){
                    declPackageWithoutInit = declPackageWithoutInit.substring(0, declPackageWithoutInit.length()-9);
                }
                
                declPackageWithoutInit = AutoImportsPreferencesPage.removeImportsStartingWithUnderIfNeeded(declPackageWithoutInit);
                String importDeclaration = new StringBuffer("from ").append(declPackageWithoutInit).append(" import ").append(name).toString();
                String displayImport = new StringBuffer("Import ").append(name).append(" (").append(declPackage).append(")").toString();
                
                mods.add(new Tuple<String,String>(importDeclaration, displayImport));
            }
        }
        
        for (Tuple<String,String> string : mods) {
            props.add(new CtxInsensitiveImportComplProposal(
                    "",
                    offset,
                    0,
                    0,
                    importImage,
                    string.o2,
                    null,
                    "",
                    IPyCompletionProposal.PRIORITY_LOCALS,
                    string.o1
                    ){
            	@Override
            	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
            		super.apply(viewer, trigger, stateMask, offset);
            		//and after aplying it, let's request a reanalysis
            		if(viewer instanceof PySourceViewer){
            			PySourceViewer sourceViewer = (PySourceViewer) viewer;
						PyEdit edit = sourceViewer.getEdit();
						if(edit != null){
							edit.getParser().forceReparse(new Tuple<String, Boolean>(AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
						}
            		}
            	}
            });
        }
    }

    

}
