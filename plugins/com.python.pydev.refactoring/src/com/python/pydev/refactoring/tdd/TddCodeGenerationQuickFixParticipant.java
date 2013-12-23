/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.PySelection.TddPossibleMatches;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.parser.visitors.scope.ReturnVisitor;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;

import com.python.pydev.analysis.ctrl_1.AbstractAnalysisMarkersParticipants;

public class TddCodeGenerationQuickFixParticipant extends AbstractAnalysisMarkersParticipants {

    private TddQuickFixParticipant tddQuickFixParticipant;

    @Override
    protected void fillParticipants() {
        tddQuickFixParticipant = new TddQuickFixParticipant();
        participants.add(tddQuickFixParticipant);
    }

    @Override
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature,
            PyEdit edit, int offset) throws BadLocationException {
        List<ICompletionProposal> ret = super.getProps(ps, imageCache, f, nature, edit, offset);
        this.getTddProps(ps, imageCache, f, nature, edit, offset, ret);
        return ret;
    }

    public List<ICompletionProposal> getTddProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature,
            PyEdit edit, int offset, List<ICompletionProposal> ret) {
        if (ret == null) {
            ret = new ArrayList<ICompletionProposal>();
        }
        //Additional option: Generate markers for 'self.' accesses
        int lineOfOffset = ps.getLineOfOffset(offset);
        String lineContents = ps.getLine(lineOfOffset);

        //Additional option: Generate methods for function calls
        List<TddPossibleMatches> callsAtLine = ps.getTddPossibleMatchesAtLine();
        if (callsAtLine.size() > 0) {
            //Make sure we don't check the same thing twice.
            Map<String, TddPossibleMatches> callsToCheck = new HashMap<String, TddPossibleMatches>();
            for (TddPossibleMatches call : callsAtLine) {
                String callString = call.initialPart + call.secondPart;
                callsToCheck.put(callString, call);
            }

            CONTINUE_FOR: for (Map.Entry<String, TddPossibleMatches> entry : callsToCheck.entrySet()) {
                //we have at least something as SomeClass(a=2,c=3) or self.bar or self.foo.bar() or just foo.bar, etc.
                IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                try {
                    TddPossibleMatches possibleMatch = entry.getValue();
                    String callWithoutParens = entry.getKey();

                    ItemPointer[] pointers = null;
                    PySelection callPs = null;
                    TddPossibleMatches lastPossibleMatchNotFound = possibleMatch;

                    for (int i = 0; i < 10; i++) { //more than 10 attribute accesses in a line? No way!

                        lastPossibleMatchNotFound = possibleMatch;
                        if (i > 0) {
                            //We have to take 1 level out of the match... i.e.: if it was self.foo.get(), search now for self.foo.
                            String line = FullRepIterable.getWithoutLastPart(possibleMatch.full);
                            List<TddPossibleMatches> tddPossibleMatchesAtLine = ps.getTddPossibleMatchesAtLine(line);
                            if (tddPossibleMatchesAtLine.size() > 0) {
                                possibleMatch = tddPossibleMatchesAtLine.get(0);
                                callWithoutParens = possibleMatch.initialPart + possibleMatch.secondPart;
                            } else {
                                continue CONTINUE_FOR;
                            }
                        }
                        String full = possibleMatch.full;
                        int indexOf = lineContents.indexOf(full);
                        if (indexOf < 0) {
                            Log.log("Did not expect index < 0.");
                            continue CONTINUE_FOR;
                        }
                        callPs = new PySelection(ps.getDoc(), ps.getLineOffset() + indexOf + callWithoutParens.length());

                        RefactoringRequest request = new RefactoringRequest(f, callPs, null, nature, edit);
                        //Don't look in additional info.
                        request.setAdditionalInfo(
                                RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
                        pointers = pyRefactoring.findDefinition(request);

                        if (((pointers != null && pointers.length > 0) || StringUtils.count(possibleMatch.full, '.') <= 1)) {
                            break;
                        }
                    }

                    if (pointers == null || callPs == null) {
                        continue CONTINUE_FOR;
                    }

                    if (lastPossibleMatchNotFound != null && lastPossibleMatchNotFound != possibleMatch
                            && pointers.length >= 1) {
                        //Ok, as we were analyzing a string as self.bar.foo, we didn't find something in a pass
                        //i.e.: self.bar.foo, but we found it in a second pass
                        //as self.bar, so, this means we have to open the chance to create the 'foo' in self.bar.
                        String methodToCreate = FullRepIterable.getLastPart(lastPossibleMatchNotFound.secondPart);
                        int absoluteCursorOffset = callPs.getAbsoluteCursorOffset();
                        absoluteCursorOffset = absoluteCursorOffset - (1 + methodToCreate.length()); //+1 for the dot removed too.
                        PySelection newSelection = new PySelection(callPs.getDoc(), absoluteCursorOffset);

                        checkCreationBasedOnFoundPointers(edit, callPs, ret, possibleMatch, pointers, methodToCreate,
                                newSelection, nature);
                        continue CONTINUE_FOR;
                    }

                    if (pointers.length >= 1) {

                        //Ok, we found whatever was there, so, we don't need to create anything (except maybe do
                        //the __init__ or something at the class level).
                        if (!checkInitCreation(edit, callPs, pointers, ret)) {
                            //This was called only when isCall == false
                            //Ok, if it's not a call and we found a field, it's still possible that we may want to create
                            //a field if it wasn't found in the __init__
                            boolean foundInInit = false;
                            for (ItemPointer p : pointers) {
                                Definition definition = p.definition;
                                try {
                                    Object peek = definition.scope.getScopeStack().peek();
                                    if (peek instanceof FunctionDef) {
                                        FunctionDef functionDef = (FunctionDef) peek;
                                        String rep = NodeUtils.getRepresentationString(functionDef);
                                        if (rep != null && rep.equals("__init__")) {
                                            foundInInit = true;
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                            if (!foundInInit) {
                                checkMethodCreationAtClass(edit, pyRefactoring, callWithoutParens, callPs, ret,
                                        lineContents, possibleMatch, f, nature);
                            }
                        }

                    } else if (pointers.length == 0) {
                        checkMethodCreationAtClass(edit, pyRefactoring, callWithoutParens, callPs, ret, lineContents,
                                possibleMatch, f, nature);

                    }
                } catch (Exception e) {
                    if (onGetTddPropsError != null) {
                        onGetTddPropsError.call(e);
                    }
                    Log.log(e);
                }
            }
        }

        return ret;
    }

    public static ICallback<Boolean, Exception> onGetTddPropsError;

    private boolean checkMethodCreationAtClass(PyEdit edit, IPyRefactoring pyRefactoring, String callWithoutParens,
            PySelection callPs, List<ICompletionProposal> ret, String lineContents, TddPossibleMatches possibleMatch,
            File f, IPythonNature nature) throws MisconfigurationException, Exception {
        RefactoringRequest request;
        ItemPointer[] pointers;
        //Ok, no definition found for the full string, so, check if we have a dot there and check
        //if it could be a method in a local variable.
        String[] headAndTail = FullRepIterable.headAndTail(callWithoutParens);
        if (headAndTail[0].length() > 0) {

            String methodToCreate = headAndTail[1];
            if (headAndTail[0].equals("self")) {
                //creating something in the current class -- note that if it was self.bar here, we'd treat it as regular
                //(i.e.: no special support for self), so, we wouldn't enter here!
                int firstCharPosition = PySelection.getFirstCharPosition(lineContents);
                LineStartingScope scopeStart = callPs.getPreviousLineThatStartsScope(PySelection.CLASS_TOKEN, false,
                        firstCharPosition);
                String classNameInLine = null;
                if (scopeStart != null) {
                    for (Boolean isCall : new Boolean[] { true, false }) {
                        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();
                        List<String> parametersAfterCall = null;
                        parametersAfterCall = configCreateAsAndReturnParametersAfterCall(callPs, isCall,
                                pyCreateMethod, parametersAfterCall, methodToCreate);
                        String startingScopeLineContents = callPs.getLine(scopeStart.iLineStartingScope);
                        classNameInLine = PySelection.getClassNameInLine(startingScopeLineContents);
                        if (classNameInLine != null && classNameInLine.length() > 0) {
                            pyCreateMethod.setCreateInClass(classNameInLine);

                            addCreateMethodOption(callPs, edit, ret, methodToCreate, parametersAfterCall,
                                    pyCreateMethod, classNameInLine);
                        }
                    }
                }
                return true;
            }

            int absoluteCursorOffset = callPs.getAbsoluteCursorOffset();
            absoluteCursorOffset = absoluteCursorOffset - (1 + methodToCreate.length()); //+1 for the dot removed too.
            PySelection newSelection = new PySelection(callPs.getDoc(), absoluteCursorOffset);
            request = new RefactoringRequest(f, newSelection, null, nature, edit);
            //Don't look in additional info.
            request.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
            pointers = pyRefactoring.findDefinition(request);
            if (pointers.length == 1) {
                if (checkCreationBasedOnFoundPointers(edit, callPs, ret, possibleMatch, pointers, methodToCreate,
                        newSelection, nature)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Definition rebaseAssignDefinition(AssignDefinition assignDef, IPythonNature nature,
            ICompletionCache completionCache) throws Exception {
        //if the value is currently None, it will be set later on
        if (assignDef.value.equals("None")) {
            return assignDef; // keep the old one
        }

        //ok, go to the definition of whatever is set
        IDefinition[] definitions2 = assignDef.module.findDefinition(
                CompletionStateFactory.getEmptyCompletionState(assignDef.value, nature, completionCache),
                assignDef.line, assignDef.col, nature);

        if (definitions2.length > 0) {
            return (Definition) definitions2[0];
        }
        return assignDef;
    }

    public Definition rebaseFunctionDef(Definition definition, IPythonNature nature, ICompletionCache completionCache)
            throws Exception {
        List<Return> returns = ReturnVisitor.findReturns((FunctionDef) definition.ast);
        for (Return returnFound : returns) {
            String act = NodeUtils.getFullRepresentationString(returnFound.value);
            if (act == null) {
                continue;
            }
            //ok, go to the definition of whatever is set
            IDefinition[] definitions2 = definition.module.findDefinition(
                    CompletionStateFactory.getEmptyCompletionState(act, nature, completionCache), definition.line,
                    definition.col, nature);
            if (definitions2.length == 1) {
                return (Definition) definitions2[0];
            }
        }
        return definition;
    }

    private Definition rebaseToClassDefDefinition(IPythonNature nature, CompletionCache completionCache,
            Definition definition, CompletionState completionState) throws CompletionRecursionException, Exception {

        if (completionState == null) {
            completionState = new CompletionState();
        }

        if (definition.ast instanceof ClassDef) {
            return definition;
        }

        if (definition instanceof AssignDefinition || definition.ast instanceof FunctionDef) {
            //Avoid recursions.
            completionState.checkDefinitionMemory(definition.module, definition);
            if (definition instanceof AssignDefinition) {
                definition = rebaseAssignDefinition((AssignDefinition) definition, nature, completionCache);

            } else { // definition.ast MUST BE FunctionDef
                definition = rebaseFunctionDef(definition, nature, completionCache);
            }

            return rebaseToClassDefDefinition(nature, completionCache, definition, completionState);
        }

        return definition;
    }

    public boolean checkCreationBasedOnFoundPointers(PyEdit edit, PySelection callPs, List<ICompletionProposal> ret,
            TddPossibleMatches possibleMatch, ItemPointer[] pointers, String methodToCreate, PySelection newSelection,
            IPythonNature nature) throws MisconfigurationException, Exception {
        CompletionCache completionCache = new CompletionCache();
        for (ItemPointer pointer : pointers) {
            Definition definition = pointer.definition;

            try {
                definition = rebaseToClassDefDefinition(nature, completionCache, definition, null);
            } catch (CompletionRecursionException e) {
                Log.log(e); //Just keep going.
            }

            if (definition.ast instanceof ClassDef) {
                ClassDef d = (ClassDef) definition.ast;
                String fullName = NodeUtils.getRepresentationString(d) + "." + methodToCreate;
                IToken repInModule = nature.getAstManager().getRepInModule(definition.module, fullName, nature);
                if (repInModule != null) {
                    //System.out.println("Skipping creation of: " + fullName); //We found it, so, don't suggest it.
                    continue;
                }

                for (Boolean isCall : new Boolean[] { true, false }) {
                    //Give the user a chance to create the method we didn't find.
                    PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();
                    List<String> parametersAfterCall = null;
                    parametersAfterCall = configCreateAsAndReturnParametersAfterCall(callPs, isCall, pyCreateMethod,
                            parametersAfterCall, methodToCreate);
                    String className = NodeUtils.getRepresentationString(d);
                    pyCreateMethod.setCreateInClass(className);

                    String displayString = StringUtils.format(
                            "Create %s %s at %s (%s)", methodToCreate,
                            pyCreateMethod.getCreationStr(), className, definition.module.getName());

                    TddRefactorCompletionInModule completion = new TddRefactorCompletionInModule(methodToCreate,
                            tddQuickFixParticipant != null ? tddQuickFixParticipant.imageMethod : null, displayString,
                            null, displayString, IPyCompletionProposal.PRIORITY_CREATE, edit,
                            definition.module.getFile(), parametersAfterCall, pyCreateMethod, newSelection);
                    completion.locationStrategy = AbstractPyCreateAction.LOCATION_STRATEGY_END;
                    ret.add(completion);
                }
                return true;
            }
        }
        return false;
    }

    private List<String> configCreateAsAndReturnParametersAfterCall(PySelection callPs, boolean isCall,
            PyCreateMethodOrField pyCreateMethod, List<String> parametersAfterCall, String methodToCreate) {
        if (isCall) {
            pyCreateMethod.setCreateAs(PyCreateMethodOrField.BOUND_METHOD);
            parametersAfterCall = callPs.getParametersAfterCall(callPs.getAbsoluteCursorOffset());
        } else {
            if (StringUtils.isAllUpper(methodToCreate)) {
                pyCreateMethod.setCreateAs(PyCreateMethodOrField.CONSTANT);

            } else {
                pyCreateMethod.setCreateAs(PyCreateMethodOrField.FIELD);
            }
        }
        return parametersAfterCall;
    }

    private void addCreateMethodOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, List<String> parametersAfterCall, PyCreateMethodOrField pyCreateMethod,
            String classNameInLine) {
        String displayString = StringUtils.format("Create %s %s at %s",
                markerContents,
                pyCreateMethod.getCreationStr(), classNameInLine);
        TddRefactorCompletion tddRefactorCompletion = new TddRefactorCompletion(markerContents,
                tddQuickFixParticipant.imageMethod, displayString, null, null, IPyCompletionProposal.PRIORITY_CREATE,
                edit, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT, parametersAfterCall, pyCreateMethod, ps);
        props.add(tddRefactorCompletion);
    }

    private boolean checkInitCreation(PyEdit edit, PySelection callPs, ItemPointer[] pointers,
            List<ICompletionProposal> ret) {
        for (ItemPointer pointer : pointers) {
            Definition definition = pointer.definition;
            if (definition != null && definition.ast instanceof ClassDef) {
                ClassDef d = (ClassDef) definition.ast;
                ASTEntry initEntry = findInitInClass(d);

                if (initEntry == null) {
                    //Give the user a chance to create the __init__.
                    PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();
                    pyCreateMethod.setCreateAs(PyCreateMethodOrField.BOUND_METHOD);
                    String className = NodeUtils.getRepresentationString(d);
                    pyCreateMethod.setCreateInClass(className);

                    List<String> parametersAfterCall = callPs.getParametersAfterCall(callPs.getAbsoluteCursorOffset());
                    String displayString = StringUtils.format(
                            "Create %s __init__ (%s)", className,
                            definition.module.getName());
                    TddRefactorCompletionInModule completion = new TddRefactorCompletionInModule("__init__",
                            tddQuickFixParticipant.imageMethod, displayString, null, displayString,
                            IPyCompletionProposal.PRIORITY_CREATE, edit, definition.module.getFile(),
                            parametersAfterCall, pyCreateMethod, callPs);
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

        for (Iterator<ASTEntry> it = visitor.getMethodsIterator(); it.hasNext();) {
            ASTEntry next = it.next();
            if (next.node != null) {
                String rep = NodeUtils.getRepresentationString(next.node);
                if ("__init__".equals(rep)) {
                    return next;
                }
            }
        }
        return null;
    }

}
