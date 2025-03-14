package com.python.pydev.analysis.refactoring.tdd;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.ast.codecompletion.revisited.CompletionCache;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.ast.codecompletion.revisited.visitors.AssignOrTypeAliasDefinition;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.item_pointer.ItemPointer;
import org.python.pydev.ast.refactoring.AbstractPyRefactoring;
import org.python.pydev.ast.refactoring.IPyRefactoring;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.core.IAssistProps;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.PySelection.TddPossibleMatches;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.parser.visitors.scope.ReturnVisitor;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.DummyImageCache;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.string.StringUtils;

public class TddCodeGenerationQuickFixWithoutMarkersParticipant implements IAssistProps {

    /**
     * Just to set in tests to raise an exception.
     */
    public static ICallback<Boolean, Exception> onGetTddPropsError;

    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        List<ICompletionProposalHandle> ret = new ArrayList<>();
        TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(ps, imageCache, f, nature, edit, offset, ret);
        return ret;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return ps.getSelLength() == 0;
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

    static boolean checkInitCreation(IPyEdit edit, PySelection callPs, ItemPointer[] pointers,
            List<ICompletionProposalHandle> ret, IImageCache imageCache) {
        for (ItemPointer pointer : pointers) {
            Definition definition = pointer.definition;
            if (definition != null && definition.ast instanceof ClassDef) {
                ClassDef d = (ClassDef) definition.ast;
                ASTEntry initEntry = TddCodeGenerationQuickFixWithoutMarkersParticipant.findInitInClass(d);

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
                    ICompletionProposalHandle completion = CompletionProposalFactory.get()
                            .createTddRefactorCompletionInModule("__init__",
                                    imageCache.get(UIConstants.CREATE_METHOD_ICON), displayString, null, displayString,
                                    IPyCompletionProposal.PRIORITY_CREATE, edit, definition.module.getFile(),
                                    parametersAfterCall, pyCreateMethod, callPs,
                                    AbstractPyCreateAction.LOCATION_STRATEGY_FIRST_METHOD);
                    ret.add(completion);
                    return true;
                }
            }
        }
        return false;
    }

    static void addCreateMethodOption(PySelection ps, IPyEdit edit, List<ICompletionProposalHandle> props,
            String markerContents, List<String> parametersAfterCall, PyCreateMethodOrField pyCreateMethod,
            String classNameInLine, IImageCache imageCache) {
        String displayString = StringUtils.format("Create %s %s at %s",
                markerContents,
                pyCreateMethod.getCreationStr(), classNameInLine);
        ICompletionProposalHandle tddRefactorCompletion = CompletionProposalFactory.get().createTddRefactorCompletion(
                markerContents,
                imageCache.get(UIConstants.CREATE_METHOD_ICON), displayString, null, null,
                IPyCompletionProposal.PRIORITY_CREATE,
                edit, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT, parametersAfterCall, pyCreateMethod, ps);
        props.add(tddRefactorCompletion);
    }

    static List<String> configCreateAsAndReturnParametersAfterCall(PySelection callPs, boolean isCall,
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

    public static boolean checkCreationBasedOnFoundPointers(IPyEdit edit, PySelection callPs,
            List<ICompletionProposalHandle> ret,
            TddPossibleMatches possibleMatch, ItemPointer[] pointers, String methodToCreate, PySelection newSelection,
            IPythonNature nature, IImageCache imageCache) throws MisconfigurationException, Exception {
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
                    parametersAfterCall = TddCodeGenerationQuickFixWithoutMarkersParticipant
                            .configCreateAsAndReturnParametersAfterCall(callPs, isCall, pyCreateMethod,
                                    parametersAfterCall, methodToCreate);
                    String className = NodeUtils.getRepresentationString(d);
                    pyCreateMethod.setCreateInClass(className);

                    String displayString = StringUtils.format(
                            "Create %s %s at %s (%s)", methodToCreate,
                            pyCreateMethod.getCreationStr(), className, definition.module.getName());

                    ICompletionProposalHandle completion = CompletionProposalFactory.get()
                            .createTddRefactorCompletionInModule(methodToCreate,
                                    imageCache.get(UIConstants.CREATE_METHOD_ICON), displayString,
                                    null, displayString, IPyCompletionProposal.PRIORITY_CREATE, edit,
                                    definition.module.getFile(), parametersAfterCall, pyCreateMethod, newSelection,
                                    AbstractPyCreateAction.LOCATION_STRATEGY_END);
                    ret.add(completion);
                }
                return true;
            }
        }
        return false;
    }

    static Definition rebaseToClassDefDefinition(IPythonNature nature, CompletionCache completionCache,
            Definition definition, CompletionState completionState) throws CompletionRecursionException, Exception {

        if (completionState == null) {
            completionState = new CompletionState();
        }

        if (definition.ast instanceof ClassDef) {
            return definition;
        }

        if (definition instanceof AssignOrTypeAliasDefinition || definition.ast instanceof FunctionDef) {
            //Avoid recursions.
            completionState.checkDefinitionMemory(definition.module, definition);
            if (definition instanceof AssignOrTypeAliasDefinition) {
                definition = rebaseAssignDefinition((AssignOrTypeAliasDefinition) definition, nature, completionCache);

            } else { // definition.ast MUST BE FunctionDef
                definition = rebaseFunctionDef(definition, nature, completionCache);
            }

            return rebaseToClassDefDefinition(nature, completionCache, definition, completionState);
        }

        return definition;
    }

    public static Definition rebaseFunctionDef(Definition definition, IPythonNature nature,
            ICompletionCache completionCache)
            throws Exception {
        ITypeInfo type = NodeUtils.getReturnTypeFromFuncDefAST(definition.ast);
        if (type != null) {
            // ok, go to the definition of whatever is set
            IDefinition[] definitions2 = definition.module.findDefinition(
                    CompletionStateFactory.getEmptyCompletionState(type.getActTok(), nature, completionCache),
                    definition.line,
                    definition.col, nature);
            if (definitions2.length == 1) {
                return (Definition) definitions2[0];
            }
        }

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

    public static Definition rebaseAssignDefinition(AssignOrTypeAliasDefinition assignDef, IPythonNature nature,
            ICompletionCache completionCache) throws Exception {
        IDefinition[] definitions2;
        if ("None".equals(assignDef.type)) {
            return assignDef; // keep the old one
        }
        if (assignDef.nodeType != null) {
            definitions2 = assignDef.module.findDefinition(
                    CompletionStateFactory.getEmptyCompletionState(assignDef.type, nature, completionCache),
                    assignDef.line, assignDef.col, nature);
            if (definitions2.length > 0) {
                return (Definition) definitions2[0];
            }
        }
        if ("None".equals(assignDef.value)) {
            return assignDef; // keep the old one
        }
        if (assignDef.nodeValue != null) {
            //ok, go to the definition of whatever is set
            definitions2 = assignDef.module.findDefinition(
                    CompletionStateFactory.getEmptyCompletionState(assignDef.value, nature, completionCache),
                    assignDef.line, assignDef.col, nature);
            if (definitions2.length > 0) {
                return (Definition) definitions2[0];
            }
        }

        return assignDef;
    }

    static boolean checkMethodCreationAtClass(IPyEdit edit, IPyRefactoring pyRefactoring,
            String callWithoutParens,
            PySelection callPs, List<ICompletionProposalHandle> ret, String lineContents,
            TddPossibleMatches possibleMatch,
            File f, IPythonNature nature, IImageCache imageCache) throws MisconfigurationException, Exception {
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
                        parametersAfterCall = TddCodeGenerationQuickFixWithoutMarkersParticipant
                                .configCreateAsAndReturnParametersAfterCall(callPs, isCall,
                                        pyCreateMethod, parametersAfterCall, methodToCreate);
                        String startingScopeLineContents = callPs.getLine(scopeStart.iLineStartingScope);
                        classNameInLine = PySelection.getClassNameInLine(startingScopeLineContents);
                        if (classNameInLine != null && classNameInLine.length() > 0) {
                            pyCreateMethod.setCreateInClass(classNameInLine);

                            TddCodeGenerationQuickFixWithoutMarkersParticipant.addCreateMethodOption(callPs, edit, ret,
                                    methodToCreate, parametersAfterCall,
                                    pyCreateMethod, classNameInLine, imageCache);
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
            request.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_FOLLOW_PARAM_DECLARATION, true);
            pointers = pyRefactoring.findDefinition(request);
            if (pointers.length == 1) {
                if (TddCodeGenerationQuickFixWithoutMarkersParticipant.checkCreationBasedOnFoundPointers(edit, callPs,
                        ret, possibleMatch, pointers, methodToCreate,
                        newSelection, nature, imageCache)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<ICompletionProposalHandle> getTddProps(
            PySelection ps,
            IImageCache imageCache,
            File f,
            IPythonNature nature,
            IPyEdit edit,
            int offset,
            List<ICompletionProposalHandle> ret) {
        if (ret == null) {
            ret = new ArrayList<ICompletionProposalHandle>();
        }
        if (imageCache == null) {
            imageCache = new DummyImageCache();
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
                        callPs = new PySelection(ps.getDoc(),
                                ps.getLineOffset() + indexOf + callWithoutParens.length());

                        RefactoringRequest request = new RefactoringRequest(f, callPs, null, nature, edit);
                        //Don't look in additional info.
                        request.setAdditionalInfo(
                                RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
                        pointers = pyRefactoring.findDefinition(request);

                        if (((pointers != null && pointers.length > 0)
                                || StringUtils.count(possibleMatch.full, '.') <= 1)) {
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

                        TddCodeGenerationQuickFixWithoutMarkersParticipant.checkCreationBasedOnFoundPointers(edit,
                                callPs,
                                ret, possibleMatch, pointers, methodToCreate,
                                newSelection, nature, imageCache);
                        continue CONTINUE_FOR;
                    }

                    if (pointers.length >= 1) {

                        //Ok, we found whatever was there, so, we don't need to create anything (except maybe do
                        //the __init__ or something at the class level).
                        if (!TddCodeGenerationQuickFixWithoutMarkersParticipant.checkInitCreation(edit, callPs,
                                pointers,
                                ret, imageCache)) {
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
                                TddCodeGenerationQuickFixWithoutMarkersParticipant.checkMethodCreationAtClass(edit,
                                        pyRefactoring, callWithoutParens, callPs, ret,
                                        lineContents, possibleMatch, f, nature, imageCache);
                            }
                        }

                    } else if (pointers.length == 0) {
                        TddCodeGenerationQuickFixWithoutMarkersParticipant.checkMethodCreationAtClass(edit,
                                pyRefactoring,
                                callWithoutParens, callPs, ret, lineContents,
                                possibleMatch, f, nature, imageCache);

                    }
                } catch (Exception e) {
                    if (TddCodeGenerationQuickFixWithoutMarkersParticipant.onGetTddPropsError != null) {
                        TddCodeGenerationQuickFixWithoutMarkersParticipant.onGetTddPropsError.call(e);
                    }
                    Log.log(e);
                }
            }
        }

        return ret;
    }

}
