/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.MarkerStub;
import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ctrl_1.AbstractAnalysisMarkersParticipants;
import com.python.pydev.refactoring.refactorer.AstEntryRefactorerRequestConstants;

public class TddCodeGenerationQuickFixParticipant extends AbstractAnalysisMarkersParticipants{


    private TddQuickFixParticipant tddQuickFixParticipant;
    private List<ICompletionProposal> propsComputedOnIsValid = new ArrayList<ICompletionProposal>();
    
    protected void fillParticipants() {
        tddQuickFixParticipant = new TddQuickFixParticipant();
        participants.add(tddQuickFixParticipant);
    }
    
    
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        List<ICompletionProposal> ret = super.getProps(ps, imageCache, f, nature, edit, offset);
        ret.addAll(propsComputedOnIsValid);
        propsComputedOnIsValid.clear();
        return ret;
    }
    
    /**
     * It is valid if any marker generated from the analysis is found
     *  
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection, java.lang.String, org.python.pydev.editor.PyEdit, int)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean isValid(final PySelection ps, String sel, PyEdit edit, int offset) {
        boolean isValid = super.isValid(ps, sel, edit, offset);
        
        //Additional option: Generate markers for 'self.' accesses
        int lineOfOffset = ps.getLineOfOffset(offset);
        String lineContents = ps.getLine(lineOfOffset);
        
        if(lineContents.indexOf("self.") != -1){
            HashSet<String> selfAttributeAccesses = PySelection.getSelfAttributeAccesses(lineContents);
            int lineOffset = ps.getLineOffset(lineOfOffset);
            List<MarkerAnnotationAndPosition> localMarkersAtLine = new ArrayList<MarkerAnnotationAndPosition>();
            for (String string : selfAttributeAccesses) {
                Map attrs = new HashMap();
                attrs.put(AnalysisRunner.PYDEV_ANALYSIS_TYPE, IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE_IN_SELF);
                SimpleMarkerAnnotation markerAnnotation = new SimpleMarkerAnnotation(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, new MarkerStub(attrs));
                
                int indexOf = lineContents.indexOf("self."+string);
                if(indexOf >= 0){
                    Position position = new Position(lineOffset+indexOf+5, string.length());
                    localMarkersAtLine.add(new MarkerAnnotationAndPosition(markerAnnotation, position));
                }
            }
            if(localMarkersAtLine.size() > 0){
                if(this.markersAtLine == null){
                    this.markersAtLine = localMarkersAtLine;
                }else{
                    this.markersAtLine.addAll(localMarkersAtLine);
                }
                isValid = true;
            }
        }
        

        //Additional option: Generate methods for function calls
        List<String> callsAtLine = ps.getFunctionCallsAtLine();
        if (callsAtLine.size() > 0) {
            //Make sure we don't check the same thing twice.
            Map<String, String> callsToCheck = new HashMap<String, String>();
            for(String call:callsAtLine){
                String s = call.substring(0, call.length()-1); // Remove the ending '('
                s = StringUtils.rightTrim(s);
                callsToCheck.put(s, call);
            }
            
            for(Map.Entry<String, String> entry:callsToCheck.entrySet()){
                //we have at least something as SomeClass(a=2,c=3)
                IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                try {
                    String call = entry.getValue();
                    String callWithoutParens = entry.getKey();
                    PySelection callPs = new PySelection(ps.getDoc(), ps.getLineOffset()+lineContents.indexOf(call)+callWithoutParens.length());
                    
                    RefactoringRequest request = new RefactoringRequest(edit, callPs);
                    //Don't look in additional info.
                    request.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
                    ItemPointer[] pointers = pyRefactoring.findDefinition(request);
                    if(pointers.length == 1){
                        if(checkInitCreation(edit, isValid, callPs, pointers)){
                            isValid = true;
                        }
                        
                    }else if(pointers.length == 0){
                        //Ok, no definition found for the full string, so, check if we have a dot there and check
                        //if it could be a method in a local variable.
                        String[] headAndTail = FullRepIterable.headAndTail(callWithoutParens);
                        if(headAndTail[0].length() > 0){
                            String methodToCreate = headAndTail[1];
                            int absoluteCursorOffset = callPs.getAbsoluteCursorOffset();
                            absoluteCursorOffset = absoluteCursorOffset - (1+methodToCreate.length()); //+1 for the dot removed too.
                            callPs.setSelection(absoluteCursorOffset, absoluteCursorOffset);
                            request = new RefactoringRequest(edit, callPs);
                            //Don't look in additional info.
                            request.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
                            pointers = pyRefactoring.findDefinition(request);
                            if(pointers.length == 1){
                                for (ItemPointer pointer : pointers) {
                                    Definition definition = pointer.definition;
                                    
                                    if(definition instanceof AssignDefinition){
                                        AssignDefinition assignDef = (AssignDefinition) definition;
                                        
                                        //if the value is currently None, it will be set later on
                                        if(assignDef.value.equals("None")){
                                            continue;
                                        }
                                        IPythonNature nature = edit.getPythonNature(); 
                                        
                                        //ok, go to the definition of whatever is set
                                        IDefinition[] definitions2 = assignDef.module.findDefinition(
                                                CompletionStateFactory.getEmptyCompletionState(assignDef.value, nature, new CompletionCache()), 
                                                assignDef.line, assignDef.col, nature);
                                        
                                        if(definitions2.length == 1){
                                            definition = (Definition) definitions2[0];
                                        }
                                    }

                                    
                                    if(definition != null && definition.ast instanceof ClassDef){
                                        ClassDef d = (ClassDef) definition.ast;
                                        
                                        //Give the user a chance to create the method we didn't find.
                                        PyCreateMethod pyCreateMethod = new PyCreateMethod();
                                        pyCreateMethod.setCreateAs(PyCreateMethod.BOUND_METHOD);
                                        String className = NodeUtils.getRepresentationString(d);
                                        pyCreateMethod.setCreateInClass(className);
                                        
                                        List<String> parametersAfterCall = callPs.getParametersAfterCall(callPs.getAbsoluteCursorOffset());
                                        String displayString = "Create "+methodToCreate+" method at "+className+" ("+definition.module.getName()+")";
                                        TddRefactorCompletionInModule completion = new TddRefactorCompletionInModule(
                                                methodToCreate, 
                                                tddQuickFixParticipant.imageMethod, 
                                                displayString, 
                                                null, 
                                                displayString, 
                                                IPyCompletionProposal.PRIORITY_CREATE, 
                                                edit,
                                                definition.module.getFile(),
                                                parametersAfterCall,
                                                pyCreateMethod,
                                                callPs
                                        );
                                        completion.locationStrategy = PyCreateAction.LOCATION_STRATEGY_END;
                                        propsComputedOnIsValid.add(completion);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            
        }

        return isValid;
    }


    private boolean checkInitCreation(PyEdit edit, boolean isValid, PySelection callPs, ItemPointer[] pointers) {
        for (ItemPointer pointer : pointers) {
            Definition definition = pointer.definition;
            if(definition != null && definition.ast instanceof ClassDef){
                ClassDef d = (ClassDef) definition.ast;
                EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(d);
                
                boolean foundInit = false;
                for(Iterator<ASTEntry> it = visitor.getMethodsIterator();it.hasNext();){
                    ASTEntry next = it.next();
                    if(next.node != null){
                        String rep = NodeUtils.getRepresentationString(next.node);
                        if("__init__".equals(rep)){
                            foundInit = true;
                            break;
                        }
                    }
                }
                
                if(!foundInit){
                    //Give the user a chance to create the __init__.
                    PyCreateMethod pyCreateMethod = new PyCreateMethod();
                    pyCreateMethod.setCreateAs(PyCreateMethod.BOUND_METHOD);
                    String className = NodeUtils.getRepresentationString(d);
                    pyCreateMethod.setCreateInClass(className);
                    
                    
                    List<String> parametersAfterCall = callPs.getParametersAfterCall(callPs.getAbsoluteCursorOffset());
                    String displayString = "Create "+className+" __init__ ("+definition.module.getName()+")";
                    TddRefactorCompletionInModule completion = new TddRefactorCompletionInModule(
                            "__init__", 
                            tddQuickFixParticipant.imageMethod, 
                            displayString, 
                            null, 
                            displayString, 
                            IPyCompletionProposal.PRIORITY_CREATE, 
                            edit,
                            definition.module.getFile(),
                            parametersAfterCall,
                            pyCreateMethod,
                            callPs
                    );
                    completion.locationStrategy = PyCreateAction.LOCATION_STRATEGY_FIRST_METHOD;
                    propsComputedOnIsValid.add(completion);
                    return true;
                }
            }
        }
        return false;
    }


}
