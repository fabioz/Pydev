package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ctrl_1.IAnalysisMarkersParticipant;

/**
 * This participant will add a suggestion to create class/methods/attributes when an undefined variable error is found.
 */
public class TddQuickFixParticipant implements IAnalysisMarkersParticipant{

    public void addProps(
            MarkerAnnotationAndPosition markerAnnotation, 
            IAnalysisPreferences analysisPreferences, 
            String line, 
            PySelection ps, 
            int offset, 
            IPythonNature nature,
            PyEdit edit, 
            List<ICompletionProposal> props) throws BadLocationException, CoreException {
        if(nature == null){
            return;
        }
        
        ICodeCompletionASTManager astManager = nature.getAstManager();
        if(astManager == null){
            return;
        }
        
        IMarker marker = markerAnnotation.markerAnnotation.getMarker();
        Integer id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        int start = markerAnnotation.position.offset;
        int end = start+markerAnnotation.position.length;
        ps.setSelection(start, end);
        String markerContents = ps.getSelectedText();
        
        IDocument doc = ps.getDoc();
        List<String> parametersAfterCall = ps.getParametersAfterCall(end);
        
        
        Image image = null;
        ImageCache imageCache = PydevPlugin.getImageCache();
        if(imageCache != null){ //making tests
            image = imageCache.get(UIConstants.CLASS_ICON);
        }
        switch(id){
        case IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE_IN_SELF:
            PyCreateMethod pyCreateMethod = new PyCreateMethod();
            pyCreateMethod.setCreateAs(PyCreateMethod.BOUND_METHOD);
            int firstCharPosition = PySelection.getFirstCharPosition(line);
            LineStartingScope scopeStart = ps.getPreviousLineThatStartsScope(
                    PySelection.CLASS_TOKEN, false, firstCharPosition);
            
            String startingScopeLineContents = ps.getLine(scopeStart.iLineStartingScope);
            String classNameInLine = PySelection.getClassNameInLine(startingScopeLineContents);
            if(classNameInLine != null && classNameInLine.length() > 0){
                pyCreateMethod.setCreateInClass(classNameInLine);
                
                TddRefactorCompletion tddRefactorCompletion = new TddRefactorCompletion(
                        markerContents, 
                        image, "Create "+markerContents+" method at "+classNameInLine, 
                        null, 
                        null, 
                        IPyCompletionProposal.PRIORITY_CREATE, 
                        edit,
                        PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT,
                        parametersAfterCall,
                        pyCreateMethod,
                        ps
                );
                props.add(tddRefactorCompletion);
            }
            break;
        case IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE:
            
            props.add(new TddRefactorCompletion(
                    markerContents, 
                    image, "Create "+markerContents+" class", 
                    null, 
                    null, 
                    IPyCompletionProposal.PRIORITY_CREATE, 
                    edit,
                    PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT,
                    parametersAfterCall,
                    new PyCreateClass(),
                    ps
            ));
            
            props.add(new TddRefactorCompletion(
                    markerContents, 
                    image, "Create "+markerContents+" method", 
                    null, 
                    null, 
                    IPyCompletionProposal.PRIORITY_CREATE, 
                    edit,
                    PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT,
                    parametersAfterCall,
                    new PyCreateMethod(),
                    ps
            ));
            break;
            
            
        case IAnalysisPreferences.TYPE_UNDEFINED_IMPORT_VARIABLE:
            //Say we had something as:
            //import sys
            //sys.Bar
            //in which case 'Bar' is undefined
            //in this situation, the activationTokenAndQual would be "sys." and "Bar" 
            //and we want to get the definition for "sys"
            String[] activationTokenAndQual = ps.getActivationTokenAndQual(true);
            
            if(activationTokenAndQual[0].endsWith(".")){
                ArrayList<IDefinition> selected = findDefinitions(nature, edit, start, doc);
                
                for (IDefinition iDefinition : selected) {
                    
                    IModule module = iDefinition.getModule();
                    if(module.getFile() != null){
                        Definition definition = (Definition) iDefinition;
                        File file = module.getFile();
                        if(definition.ast == null){
                            //if we have no ast in the definition, it means the module itself was found (global scope)
                            
                            //Add option to create class at the given module!
                            props.add(new TddRefactorCompletionInModule(
                                    markerContents, 
                                    image, 
                                    "Create "+markerContents+" class at "+file.getName(), 
                                    null, 
                                    "Create "+markerContents+" class at "+file, 
                                    IPyCompletionProposal.PRIORITY_CREATE, 
                                    edit,
                                    file,
                                    parametersAfterCall,
                                    new PyCreateClass(),
                                    ps
                            ));
                            
                            props.add(new TddRefactorCompletionInModule(
                                    markerContents, 
                                    image, 
                                    "Create "+markerContents+" method at "+file.getName(), 
                                    null, 
                                    "Create "+markerContents+" method at "+file, 
                                    IPyCompletionProposal.PRIORITY_CREATE, 
                                    edit,
                                    file,
                                    parametersAfterCall,
                                    new PyCreateMethod(),
                                    ps
                            ));
                        }else if(definition.ast instanceof ClassDef){
                            ClassDef classDef = (ClassDef) definition.ast;
                            //Ok, we should create a field or method in this case (accessing a classmethod or staticmethod)
                            pyCreateMethod = new PyCreateMethod();
                            String className = NodeUtils.getNameFromNameTok(classDef.name);
                            pyCreateMethod.setCreateInClass(className);
                            pyCreateMethod.setCreateAs(PyCreateMethod.CLASSMETHOD);
                            props.add(new TddRefactorCompletionInModule(
                                    markerContents, 
                                    image, 
                                    "Create "+markerContents+" classmethod at "+className+" in "+file.getName(), 
                                    null, 
                                    "Create "+markerContents+" classmethod at class: "+className+" in "+file, 
                                    IPyCompletionProposal.PRIORITY_CREATE, 
                                    edit,
                                    file,
                                    parametersAfterCall,
                                    pyCreateMethod,
                                    ps
                            ));
                        }
                    }
                }
            }
            break;
        }
        
    }

    protected ArrayList<IDefinition> findDefinitions(IPythonNature nature, PyEdit edit, int start, IDocument doc) {
        CompletionCache completionCache = new CompletionCache();
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
        
        RefactoringRequest request = new RefactoringRequest(
                edit.getEditorFile(), 
                new PySelection(doc, new TextSelection(doc, start-2, 0)), 
                new NullProgressMonitor(), 
                nature, 
                edit);
        
        try {
            PyRefactoringFindDefinition.findActualDefinition(request, completionCache, selected);
        } catch (CompletionRecursionException e1) {
            Log.log(e1);
        }
        return selected;
    }

}
