/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.PySelection.TddPossibleMatches;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

import com.python.pydev.analysis.ctrl_1.AbstractAnalysisMarkersParticipants;
import com.python.pydev.refactoring.refactorer.AstEntryRefactorerRequestConstants;

public class TddCodeGenerationQuickFixParticipant extends AbstractAnalysisMarkersParticipants{


    private TddQuickFixParticipant tddQuickFixParticipant;
    
    protected void fillParticipants() {
        tddQuickFixParticipant = new TddQuickFixParticipant();
        participants.add(tddQuickFixParticipant);
    }
    
    
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        List<ICompletionProposal> ret = super.getProps(ps, imageCache, f, nature, edit, offset);
        
        
        //Additional option: Generate markers for 'self.' accesses
        int lineOfOffset = ps.getLineOfOffset(offset);
        String lineContents = ps.getLine(lineOfOffset);
        
        //Additional option: Generate methods for function calls
        List<TddPossibleMatches> callsAtLine = ps.getTddPossibleMatchesAtLine();
        if (callsAtLine.size() > 0) {
            //Make sure we don't check the same thing twice.
            Map<String, TddPossibleMatches> callsToCheck = new HashMap<String, TddPossibleMatches>();
            for(TddPossibleMatches call:callsAtLine){
                String callString = call.initialPart+call.secondPart;
                callsToCheck.put(callString, call);
            }
            
            CONTINUE_FOR:
            for(Map.Entry<String, TddPossibleMatches> entry:callsToCheck.entrySet()){
                //we have at least something as SomeClass(a=2,c=3) or self.bar or self.foo.bar() or just foo.bar, etc.
                IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                try {
                    TddPossibleMatches possibleMatch = entry.getValue();
                    String callWithoutParens = entry.getKey();
                    
                    ItemPointer[] pointers = null;
                    PySelection callPs = null;
                    TddPossibleMatches lastPossibleMatchNotFound = possibleMatch;
                    String lastCallWithoutParensNotFound = callWithoutParens;
                    
                    for (int i=0;i<10;i++){ //more than 10 attribute accesses in a line? No way!

                        lastPossibleMatchNotFound = possibleMatch;
                        lastCallWithoutParensNotFound = callWithoutParens;
                        if(i > 0){
                            //We have to take 1 level out of the match... i.e.: if it was self.foo.get(), search now for self.foo.
                            String line = FullRepIterable.getWithoutLastPart(possibleMatch.full);
                            List<TddPossibleMatches> tddPossibleMatchesAtLine = ps.getTddPossibleMatchesAtLine(line);
                            if(tddPossibleMatchesAtLine.size() > 0){
                                possibleMatch = tddPossibleMatchesAtLine.get(0);
                                callWithoutParens = possibleMatch.initialPart+possibleMatch.secondPart;
                            }else{
                                continue CONTINUE_FOR;
                            }
                        }
                        String full = possibleMatch.full;
                        int indexOf = lineContents.indexOf(full);
                        if(indexOf < 0){
                            Log.log("Did not expect index < 0.");
                            continue CONTINUE_FOR;
                        }
                        callPs = new PySelection(
                                ps.getDoc(), ps.getLineOffset()+indexOf+callWithoutParens.length());
                        
                        RefactoringRequest request = new RefactoringRequest(edit, callPs);
                        //Don't look in additional info.
                        request.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
                        pointers = pyRefactoring.findDefinition(request);
                        if(i == 1){
                            if((pointers != null && pointers.length > 0)){
                                //The use-case is the following:
                                //We have self.bar.foo()
                                //and we found self.bar, so, we have to create the 'foo()', meaning that the current match
                                //should actually create a method and not a field as it was found now.
                                possibleMatch.isCall = lastPossibleMatchNotFound.isCall;
                            }
                        }
                        
                        if(((pointers != null && pointers.length > 0) || StringUtils.count(possibleMatch.full, '.') <= 1)){
                            break;
                        }
                    }
                    
                    if(pointers == null || callPs == null){
                        continue CONTINUE_FOR;
                    }
                    
                    if(lastPossibleMatchNotFound != null && lastPossibleMatchNotFound != possibleMatch && pointers.length >=1){
                        //Ok, as we were analyzing a string as self.bar.foo, we didn't find something in a pass
                        //i.e.: self.bar.foo, but we found it in a second pass
                        //as self.bar, so, this means we have to open the chance to create the 'foo' in self.bar.
                        String methodToCreate = FullRepIterable.getLastPart(lastPossibleMatchNotFound.secondPart);
                        int absoluteCursorOffset = callPs.getAbsoluteCursorOffset();
                        absoluteCursorOffset = absoluteCursorOffset - (1+methodToCreate.length()); //+1 for the dot removed too.
                        PySelection newSelection = new PySelection(callPs.getDoc(), absoluteCursorOffset);

                        checkCreationBasedOnFoundPointers(
                                edit, callPs, ret, possibleMatch, pointers, methodToCreate, newSelection);
                        return ret;
                    }
                    
                    if(possibleMatch.isCall && pointers.length >= 1){
                        //Ok, we found whatever was there, so, we don't need to create anything (except maybe do
                        //the __init__).
                        checkInitCreation(edit, callPs, pointers, ret);
                        
                    }else{
                        if(pointers.length == 0){
                            checkMethodCreationAtClass(edit, pyRefactoring, callWithoutParens, callPs, ret, lineContents, possibleMatch);
                            
                        }else if(!possibleMatch.isCall){
                            //Ok, if it's not a call and we found a field, it's still possible that we may want to create
                            //a field if it wasn't found in the __init__
                            boolean foundInInit = false;
                            for(ItemPointer p:pointers){
                                Definition definition = p.definition;
                                try {
                                    Object peek = definition.scope.getScopeStack().peek();
                                    if(peek instanceof FunctionDef){
                                        FunctionDef functionDef = (FunctionDef) peek;
                                        String rep = NodeUtils.getRepresentationString(functionDef);
                                        if(rep != null && rep.equals("__init__")){
                                            foundInInit = true;
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                            if(!foundInInit){
                                checkMethodCreationAtClass(edit, pyRefactoring, callWithoutParens, callPs, ret, lineContents, possibleMatch);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        return ret;
    }


    private boolean checkMethodCreationAtClass(PyEdit edit, IPyRefactoring pyRefactoring, String callWithoutParens,
            PySelection callPs, List<ICompletionProposal> ret, String lineContents, TddPossibleMatches possibleMatch) throws MisconfigurationException, Exception {
        RefactoringRequest request;
        ItemPointer[] pointers;
        //Ok, no definition found for the full string, so, check if we have a dot there and check
        //if it could be a method in a local variable.
        String[] headAndTail = FullRepIterable.headAndTail(callWithoutParens);
        if(headAndTail[0].length() > 0){

            String methodToCreate = headAndTail[1];
            if (headAndTail[0].equals("self")) {
                //creating something in the current class -- note that if it was self.bar here, we'd treat it as regular
                //(i.e.: no special support for self), so, we wouldn't enter here!
                int firstCharPosition = PySelection.getFirstCharPosition(lineContents);
                LineStartingScope scopeStart = callPs.getPreviousLineThatStartsScope(PySelection.CLASS_TOKEN, false, firstCharPosition);
                String classNameInLine = null;
                if (scopeStart != null) {
                    PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();
                    List<String> parametersAfterCall = null;
                    parametersAfterCall = configCreateAsAndReturnParaetersAfterCall(callPs, possibleMatch.isCall, pyCreateMethod,
                            parametersAfterCall, methodToCreate);
                    String startingScopeLineContents = callPs.getLine(scopeStart.iLineStartingScope);
                    classNameInLine = PySelection.getClassNameInLine(startingScopeLineContents);
                    if (classNameInLine != null && classNameInLine.length() > 0) {
                        pyCreateMethod.setCreateInClass(classNameInLine);

                        addCreateMethodOption(
                                callPs, edit, ret, methodToCreate, parametersAfterCall, pyCreateMethod, classNameInLine);
                    }
                }
                return true;
            }

            int absoluteCursorOffset = callPs.getAbsoluteCursorOffset();
            absoluteCursorOffset = absoluteCursorOffset - (1+methodToCreate.length()); //+1 for the dot removed too.
            PySelection newSelection = new PySelection(callPs.getDoc(), absoluteCursorOffset);
            request = new RefactoringRequest(edit, newSelection);
            //Don't look in additional info.
            request.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
            pointers = pyRefactoring.findDefinition(request);
            if(pointers.length == 1){
                if(checkCreationBasedOnFoundPointers(edit, callPs, ret, possibleMatch, pointers, methodToCreate, newSelection)){
                    return true;
                }
            }
        }
        return false;
    }


    public boolean checkCreationBasedOnFoundPointers(PyEdit edit, PySelection callPs, List<ICompletionProposal> ret,
            TddPossibleMatches possibleMatch, ItemPointer[] pointers, String methodToCreate, PySelection newSelection)
            throws MisconfigurationException, Exception {
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
                PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();
                List<String> parametersAfterCall = null;
                parametersAfterCall = configCreateAsAndReturnParaetersAfterCall(callPs, possibleMatch.isCall, pyCreateMethod,
                        parametersAfterCall, methodToCreate);
                String className = NodeUtils.getRepresentationString(d);
                pyCreateMethod.setCreateInClass(className);
                
                String displayString = StringUtils.format("Create %s %s at %s (%s)", 
                        methodToCreate, pyCreateMethod.getCreationStr(), className, definition.module.getName());
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
                        newSelection
                );
                completion.locationStrategy = AbstractPyCreateAction.LOCATION_STRATEGY_END;
                ret.add(completion);
                return true;
            }
        }
        return false;
    }


    private List<String> configCreateAsAndReturnParaetersAfterCall(PySelection callPs, boolean isCall,
            PyCreateMethodOrField pyCreateMethod, List<String> parametersAfterCall, String methodToCreate) {
        if(isCall){
            pyCreateMethod.setCreateAs(PyCreateMethodOrField.BOUND_METHOD);
            parametersAfterCall = callPs.getParametersAfterCall(callPs.getAbsoluteCursorOffset());
        }else{
            if(StringUtils.isAllUpper(methodToCreate)){
                pyCreateMethod.setCreateAs(PyCreateMethodOrField.CONSTANT);
                
            }else{
                pyCreateMethod.setCreateAs(PyCreateMethodOrField.FIELD);
            }
        }
        return parametersAfterCall;
    }
    
    private void addCreateMethodOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props, String markerContents,
            List<String> parametersAfterCall, PyCreateMethodOrField pyCreateMethod, String classNameInLine) {
        String displayString = StringUtils.format("Create %s %s at %s", markerContents, pyCreateMethod.getCreationStr(), classNameInLine);
        TddRefactorCompletion tddRefactorCompletion = new TddRefactorCompletion(
                markerContents, 
                tddQuickFixParticipant.imageMethod, 
                displayString, 
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


    private boolean checkInitCreation(PyEdit edit, PySelection callPs, ItemPointer[] pointers, List<ICompletionProposal> ret) {
        for (ItemPointer pointer : pointers) {
            Definition definition = pointer.definition;
            if(definition != null && definition.ast instanceof ClassDef){
                ClassDef d = (ClassDef) definition.ast;
                ASTEntry initEntry = findInitInClass(d);
                
                if(initEntry == null){
                    //Give the user a chance to create the __init__.
                    PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();
                    pyCreateMethod.setCreateAs(PyCreateMethodOrField.BOUND_METHOD);
                    String className = NodeUtils.getRepresentationString(d);
                    pyCreateMethod.setCreateInClass(className);
                    
                    
                    List<String> parametersAfterCall = callPs.getParametersAfterCall(callPs.getAbsoluteCursorOffset());
                    String displayString = StringUtils.format("Create %s __init__ (%s)", className, definition.module.getName());
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
                    completion.locationStrategy = AbstractPyCreateAction.LOCATION_STRATEGY_FIRST_METHOD;
                    ret.add(completion);
                    return true;
                }
            }
        }
        return false;
    }


    public static ASTEntry findInitInClass(ClassDef d) {
        EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(d);
        
        for(Iterator<ASTEntry> it = visitor.getMethodsIterator();it.hasNext();){
            ASTEntry next = it.next();
            if(next.node != null){
                String rep = NodeUtils.getRepresentationString(next.node);
                if("__init__".equals(rep)){
                    return next;
                }
            }
        }
        return null;
    }


}
