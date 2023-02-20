/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.ast.codecompletion.PyCodeCompletionUtils.IFilter;
import org.python.pydev.ast.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.ast.codecompletion.revisited.AssignAnalysis;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.modules.ClassDefTokensExtractor;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.codecompletion.shell.CompletionsShell;
import org.python.pydev.ast.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ICompletionState.LookingFor;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITokenCompletionRequest;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.IterEntry;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.NoExceptionCloseable;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.TokensListMixedLookingFor;
import org.python.pydev.core.TokensOrProposalsList;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.logging.DebugSettings;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.ImmutableTuple;
import org.python.pydev.shared_core.structure.OrderedMap;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion extends AbstractPyCodeCompletion {

    /**
     * Called when a recursion exception is detected.
     */
    public static ICallback<Object, CompletionRecursionException> onCompletionRecursionException;

    @Override
    public TokensOrProposalsList getCodeCompletionProposals(CompletionRequest request) throws CoreException,
            BadLocationException, IOException, MisconfigurationException, PythonNatureWithoutProjectException {
        if (request.getPySelection().getCursorLineContents().trim().startsWith("#")) {
            //this may happen if the context is still not correctly computed in python
            return new PyStringCodeCompletion().getCodeCompletionProposals(request);
        }

        if (PySelection.isCompletionForLiteralNumber(request.getActivationToken())) {
            // suppress completions that would be invalid
            return new TokensOrProposalsList();
        }

        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "Starting getCodeCompletionProposals");
            org.python.pydev.shared_core.log.ToLogFile.addLogLevel();
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "Request:" + request);
        }

        ArrayList<ICompletionProposalHandle> ret = new ArrayList<ICompletionProposalHandle>();

        //let's see if we should do a code-completion in the current scope...

        //this engine does not work 'correctly' in the default scope on:
        //- class definitions - after 'class' and before '('
        //- method definitions - after 'def' and before '('
        PySelection ps = request.getPySelection();
        int lineCtx = ps.isRightAfterDeclarationInLine();
        if (lineCtx != PySelection.DECLARATION_NONE) {
            if (lineCtx == PySelection.DECLARATION_METHOD) {
                createOverrideCodeCompletions(request, ret, ps);
            }
            request.showTemplates = false;
            return new TokensOrProposalsList(ret);
        }

        try {
            IPythonNature nature = request.nature;
            checkPythonNature(nature);

            ICodeCompletionASTManager astManager = nature.getAstManager();
            if (astManager == null) {
                //we're probably still loading it.
                return new TokensOrProposalsList(ret);
            }

            //list of Object[], IToken or ICompletionProposalHandle
            TokensOrProposalsList tokensList = new TokensOrProposalsList();
            String trimmed = request.activationToken.replace('.', ' ').trim();

            ImportInfo importsTipper = getImportsTipperStr(request);

            int line = request.doc.getLineOfOffset(request.documentOffset);
            IRegion region = request.doc.getLineInformation(line);

            ICompletionState state = new CompletionState(line, request.documentOffset - region.getOffset(), null,
                    request.nature, request.qualifier);
            state.setCancelMonitor(request.getCancelMonitor());
            state.setIsInCalltip(request.isInCalltip);

            Map<String, IterTokenEntry> alreadyChecked = new HashMap<>();

            boolean importsTip = false;

            if (request.activationToken.startsWith("super()")) {
                createSuperCodeCompletions(request, tokensList, ps);

            } else if (importsTipper.importsTipperStr.length() != 0) {
                //code completion in imports
                request.isInCalltip = false; //if found after (, but in an import, it is not a calltip!
                request.isInMethodKeywordParam = false; //if found after (, but in an import, it is not a calltip!
                importsTip = doImportCompletion(request, astManager, tokensList, importsTipper);

            } else if (trimmed.length() > 0 && request.activationToken.indexOf('.') != -1) {
                //code completion for a token
                if (false) { //disabled for now.
                    fillTokensWithJediCompletions(request, request.getPySelection(), request.nature, astManager,
                            tokensList);
                } else {
                    TokensList tokenCompletions = new TokensList();
                    doTokenCompletion(request, astManager, tokenCompletions, trimmed, state);
                    tokensList.addAll(tokenCompletions);
                }
                handleKeywordParam(request, line, alreadyChecked);

            } else {
                //go to globals
                if (request.isInCalltip) {
                    // # SomeCall(|<- here)
                    state.setLookingFor(ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE);
                }
                doGlobalsCompletion(request, astManager, tokensList, state);

                //At this point, after doing the globals completion, we may also need to check if we need to show
                //keyword parameters to the user.
                handleKeywordParam(request, line, alreadyChecked);
            }

            List<Object> analyzedTokens = null;

            if (request.qualifier.length() >= PyCodeCompletionPreferences.getArgumentsDeepAnalysisNChars()) {
                //this can take some time on the analysis, so, let's let the user choose on how many chars does he
                //want to do the analysis...
                state.pushFindResolveImportMemoryCtx();
                try {
                    IFilter nameFilter = PyCodeCompletionUtils.getNameFilter(request.useSubstringMatchInCodeCompletion,
                            request.qualifier);
                    int i = 0;
                    analyzedTokens = new ArrayList<Object>();
                    // Note: later on we'll clear the tokensList and re-add the tokens on alreadyChecked.
                    for (Iterator<IterEntry> it = tokensList.iterator(); it.hasNext();) {
                        i++;
                        if (i > 10000) {
                            // Don't warn about it anymore (it's ok).
                            // Log.logWarn(StringUtils.format(
                            //         "Warning: computed %s completions (trimming to 10000).\nRequest: %s",
                            //         tokensList.size(), request));
                            break;
                        }

                        IterEntry entry = it.next();
                        Object o = entry.object;
                        if (!(o instanceof IToken)) {
                            analyzedTokens.add(o); // Don't check things that are not tokens
                        } else {
                            IToken initialToken = (IToken) o;

                            IToken token = initialToken;
                            String strRep = token.getRepresentation();
                            IterTokenEntry iterTokenEntry = alreadyChecked.get(strRep);
                            IToken prev;
                            if (iterTokenEntry == null) {
                                prev = null;
                            } else {
                                prev = iterTokenEntry.getToken();
                            }

                            if (prev != null) {
                                if (prev.getArgs().length() != 0) {
                                    continue; // we already have a version with args... just keep going
                                }
                            }
                            if (!nameFilter.acceptName(strRep)) {
                                //just re-add it if we're going to actually use it (depending on the qualifier)
                                continue;
                            }

                            IModule current = request.getModule();

                            while (token.isImportFrom()) {
                                //we'll only add it here if it is an import from (so, set the flag to false for the outer add)

                                if (token.getArgs().length() > 0) {
                                    //if we already have the args, there's also no reason to do it (that's what we'll do here)
                                    break;
                                }
                                ICompletionState s = state.getCopyForResolveImportWithActTok(token.getRepresentation());
                                s.checkFindResolveImportMemory(token);

                                ImmutableTuple<IModule, IToken> modTok = astManager.resolveImport(s, token, current);
                                IToken token2 = modTok.o2;
                                current = modTok.o1;
                                if (token2 != null && initialToken != token2) {
                                    String args = token2.getArgs();
                                    if (args.length() > 0) {
                                        //put it into the map (may override previous if it didn't have args)
                                        initialToken.setArgs(args);
                                        initialToken.setDocStr(token2.getDocStr());
                                        if (initialToken instanceof SourceToken && token2 instanceof SourceToken) {
                                            SourceToken initialSourceToken = (SourceToken) initialToken;
                                            SourceToken token2SourceToken = (SourceToken) token2;
                                            initialSourceToken.setAst(token2SourceToken.getAst());
                                        }
                                        break;
                                    }
                                    if (token2 == null
                                            || (token2.equals(token) && token2.getArgs().equals(token.getArgs())
                                                    && token2
                                                            .getParentPackage().equals(token.getParentPackage()))) {
                                        break;
                                    }
                                    token = token2;
                                } else {
                                    break;
                                }
                            }

                            // If we found a ClassDef which doesn't have a declared __init__, get the __init__ from
                            // one of the parents (if it's available).
                            //
                            // i.e.:
                            // class A:
                            //   def __init__(self, a):
                            //     pass
                            // class B(A):
                            //     pass
                            //
                            // Completions of B should have B with the 'a' argument.
                            if (token instanceof SourceToken) {
                                SourceToken sourceToken = (SourceToken) token;
                                if (sourceToken.getAst() instanceof ClassDef && token.getArgs().length() == 0) {
                                    ClassDef classDef = (ClassDef) sourceToken.getAst();
                                    if (classDef.bases != null && classDef.bases.length > 0) {
                                        String parentPackage = sourceToken.getParentPackage();
                                        if (parentPackage != null) {
                                            IModule module;
                                            if (parentPackage.equals(current.getName())) {
                                                module = current;
                                            } else {
                                                module = astManager.getModule(parentPackage, nature, true, state);
                                            }
                                            if (module != null) {
                                                if (module instanceof SourceModule) {
                                                    SourceModule sourceModule = (SourceModule) module;

                                                    OUT: for (int j = 0; j < classDef.bases.length; j++) {
                                                        try {
                                                            LookingFor lookingFor = state.getLookingFor();
                                                            TokensList completions;
                                                            try {
                                                                ClassDefTokensExtractor classTokensExtractor = new ClassDefTokensExtractor(
                                                                        classDef, sourceModule, state);
                                                                completions = classTokensExtractor
                                                                        .getCompletionsForBase(astManager,
                                                                                classDef.bases[j]);
                                                            } finally {
                                                                // Completions at this point shouldn't change the state of what we were looking for.
                                                                state.setLookingFor(lookingFor, true);
                                                            }
                                                            if (completions != null && completions.size() > 0) {
                                                                for (IterTokenEntry entry1 : completions) {
                                                                    IToken comp = entry1.getToken();
                                                                    if ("__init__".equals(comp.getRepresentation())) {
                                                                        if (comp.getArgs().length() > 0) {
                                                                            initialToken.setArgs(comp.getArgs());
                                                                        }
                                                                        break OUT;
                                                                    }
                                                                }
                                                            }
                                                        } catch (CompletionRecursionException e) {
                                                            // Ignore (just don't get the args).
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            alreadyChecked.put(strRep, new IterTokenEntry(initialToken, entry.lookingFor));
                        }
                    }

                } finally {
                    state.popFindResolveImportMemoryCtx();
                }
            }

            if (analyzedTokens == null) {
                tokensList.addAll(new TokensListMixedLookingFor(alreadyChecked.values()));
            } else {
                tokensList = new TokensOrProposalsList(analyzedTokens.toArray(new Object[0]));
                tokensList.addAll(new TokensListMixedLookingFor(alreadyChecked.values()));
            }
            changeItokenToCompletionPropostal(request, ret, tokensList, importsTip, state);
        } catch (CompletionRecursionException e) {
            if (onCompletionRecursionException != null) {
                onCompletionRecursionException.call(e);
            }
            if (DebugSettings.DEBUG_CODE_COMPLETION) {
                org.python.pydev.shared_core.log.ToLogFile.toLogFile(e);
            }
            //PydevPlugin.log(e);
            //ret.add(new CompletionProposal("",request.documentOffset,0,0,null,e.getMessage(), null,null));
        }

        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            org.python.pydev.shared_core.log.ToLogFile.remLogLevel();
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                    "Finished completion. Returned:" + ret.size() + " completions.\r\n");
        }

        return new TokensOrProposalsList(ret);
    }

    private void createSuperCodeCompletions(CompletionRequest request, TokensOrProposalsList tokensList,
            PySelection ps) throws BadLocationException, CompletionRecursionException, MisconfigurationException {
        String lineContentsToCursor = ps.getLineContentsToCursor();
        LineStartingScope scopeStart = ps.getPreviousLineThatStartsScope(PySelection.CLASS_TOKEN, false,
                PySelection.getFirstCharPosition(lineContentsToCursor));
        String className = null;
        if (scopeStart != null) {
            className = PySelection.getClassNameInLine(scopeStart.lineStartingScope);
            if (className != null && className.length() > 0) {
                Tuple<List<String>, Integer> insideParensBaseClasses = ps.getInsideParentesisToks(true,
                        scopeStart.iLineStartingScope);
                if (insideParensBaseClasses != null) {
                    for (String baseClass : insideParensBaseClasses.o1) {
                        ICompletionState state = new CompletionState(-1, -1, null, request.nature,
                                baseClass);
                        String actTok = request.activationToken.replace("super()", baseClass);
                        if (actTok.endsWith(".")) {
                            actTok = actTok.substring(0, actTok.length() - 1);
                        }
                        state.setActivationToken(actTok);
                        state.setIsInCalltip(false);

                        IPythonNature pythonNature = request.nature;
                        checkPythonNature(pythonNature);

                        ICodeCompletionASTManager astManager = pythonNature.getAstManager();
                        if (astManager == null) {
                            //we're probably still loading it.
                            return;
                        }
                        //Ok, looking for a token in globals.
                        IModule module = request.getModule();
                        if (module == null) {
                            continue;
                        }
                        try (NoExceptionCloseable c = state
                                .pushLookingFor(ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE)) {
                            LookingFor lookingFor = state.getLookingFor();
                            TokensList comps = astManager.getCompletionsForModule(module, state, true, true);
                            tokensList.addAll(comps);
                            comps.setLookingFor(lookingFor);
                        }
                    }
                }
            }
        }
    }

    private void createOverrideCodeCompletions(CompletionRequest request,
            ArrayList<ICompletionProposalHandle> ret,
            PySelection ps) throws BadLocationException {
        IImageCache imageCache = SharedCorePlugin.getImageCache();
        IImageHandle imageOverride = imageCache != null ? imageCache.get(UIConstants.METHOD_ICON) : null;
        String lineContentsToCursor = ps.getLineContentsToCursor();
        LineStartingScope scopeStart = ps.getPreviousLineThatStartsScope(PySelection.CLASS_TOKEN, false,
                PySelection.getFirstCharPosition(lineContentsToCursor));

        String className = null;
        if (scopeStart != null) {
            className = PySelection.getClassNameInLine(scopeStart.lineStartingScope);
            if (className != null && className.length() > 0) {
                Tuple<List<String>, Integer> insideParensBaseClasses = ps.getInsideParentesisToks(true,
                        scopeStart.iLineStartingScope);
                if (insideParensBaseClasses != null) {

                    //representation -> token and base class
                    OrderedMap<String, ImmutableTuple<IToken, String>> map = new OrderedMap<String, ImmutableTuple<IToken, String>>();

                    for (String baseClass : insideParensBaseClasses.o1) {
                        try {
                            ICompletionState state = new CompletionState(-1, -1, null, request.nature,
                                    baseClass);
                            state.setActivationToken(baseClass);
                            state.setIsInCalltip(false);

                            IPythonNature pythonNature = request.nature;
                            checkPythonNature(pythonNature);

                            ICodeCompletionASTManager astManager = pythonNature.getAstManager();
                            if (astManager == null) {
                                //we're probably still loading it.
                                return;
                            }
                            //Ok, looking for a token in globals.
                            IModule module = request.getModule();
                            if (module == null) {
                                continue;
                            }
                            TokensList comps = astManager.getCompletionsForModule(module, state, true, true);
                            for (IterTokenEntry entry : comps) {
                                IToken iToken = entry.getToken();
                                String representation = iToken.getRepresentation();
                                ImmutableTuple<IToken, String> curr = map.get(representation);
                                if (curr != null && curr.o1 instanceof SourceToken) {
                                    continue; //source tokens are never reset!
                                }

                                int type = iToken.getType();
                                if (iToken instanceof SourceToken
                                        && ((SourceToken) iToken).getAst() instanceof FunctionDef) {
                                    map.put(representation, new ImmutableTuple<IToken, String>(iToken,
                                            baseClass));

                                } else if (type == IToken.TYPE_FUNCTION || type == IToken.TYPE_UNKNOWN
                                        || type == IToken.TYPE_BUILTIN) {
                                    map.put(representation, new ImmutableTuple<IToken, String>(iToken,
                                            baseClass));

                                }
                            }
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }

                    for (ImmutableTuple<IToken, String> tokenAndBaseClass : map.values()) {
                        FunctionDef functionDef = null;

                        //No checkings needed for type (we already did that above).
                        if (tokenAndBaseClass.o1 instanceof SourceToken) {
                            SourceToken sourceToken = (SourceToken) tokenAndBaseClass.o1;
                            SimpleNode ast = sourceToken.getAst();
                            if (ast instanceof FunctionDef) {
                                functionDef = (FunctionDef) ast;
                            } else {
                                functionDef = sourceToken.getAliased().createCopy();
                                NameTok t = (NameTok) functionDef.name;
                                t.id = sourceToken.getRepresentation();
                            }
                        } else {
                            //unfortunately, for builtins we usually cannot trust the parameters.
                            String representation = tokenAndBaseClass.o1.getRepresentation();
                            PyAstFactory factory = new PyAstFactory(new AdapterPrefs(ps.getEndLineDelim(),
                                    request.nature));
                            functionDef = factory.createFunctionDef(representation);
                            functionDef.args = factory.createArguments(true);
                            functionDef.args.vararg = new NameTok("args", NameTok.VarArg);
                            functionDef.args.kwarg = new NameTok("kwargs", NameTok.KwArg);
                            if (!representation.equals("__init__")) {
                                functionDef.body = new stmtType[] { new Return(null) }; //signal that the return should be added
                            }
                        }

                        if (functionDef != null) {
                            ret.add(CompletionProposalFactory.get().createOverrideMethodCompletionProposal(
                                    request, ps,
                                    ps.getAbsoluteCursorOffset(), 0, 0, imageOverride,
                                    functionDef, tokenAndBaseClass.o2, className));
                        }
                    }

                }
            }
        }
    }

    private static void fillTokensWithJediCompletions(CompletionRequest request, PySelection ps, IPythonNature nature,
            ICodeCompletionASTManager astManager, TokensOrProposalsList tokensList) throws IOException, CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {

        try {
            char c = ps.getCharBeforeCurrentOffset();
            if (c == '(') {
                System.out.println("Get call def.");
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }

        CompletionsShell shell = (CompletionsShell) AbstractShell.getServerShell(nature, AbstractShell.getShellId());
        String charset = "utf-8";
        //                    if (viewer instanceof PySourceViewer) {
        //                        PySourceViewer pySourceViewer = (PySourceViewer) viewer;
        //                        IEditorInput input = (IEditorInput) pySourceViewer.getAdapter(IEditorInput.class);
        //                        final IFile file = (IFile) ((FileEditorInput) input).getAdapter(IFile.class);
        //                        charset = file.getCharset();
        //                    }
        List<String> completePythonPath = astManager.getModulesManager().getCompletePythonPath(
                nature.getProjectInterpreter(),
                nature.getRelatedInterpreterManager());
        List<IToken> completions;
        try {
            completions = shell.getJediCompletions(request.editorFile, ps,
                    charset, completePythonPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        tokensList.addAll(new TokensList(completions));
    }

    private void handleKeywordParam(CompletionRequest request, int line, Map<String, IterTokenEntry> alreadyChecked)
            throws BadLocationException, CompletionRecursionException {
        if (request.isInMethodKeywordParam) {

            PySelection ps = new PySelection(request.doc, request.offsetForKeywordParam);
            RefactoringRequest findRequest = new RefactoringRequest(request.editorFile, ps, new NullProgressMonitor(),
                    request.nature, null);
            ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
            CompletionState completionState = new CompletionState();
            PyRefactoringFindDefinition.findActualDefinition(findRequest, completionState, selected);

            //Changed: showing duplicated parameters (only removing self and cls).
            //Tuple<List<String>, Integer> insideParentesisToks = ps.getInsideParentesisToks(false, completionRequestForKeywordParam.documentOffset);
            HashSet<String> ignore = new HashSet<String>();
            ignore.add("self");
            ignore.add("cls");
            //if(insideParentesisToks!=null && insideParentesisToks.o1 != null){
            //    for (String object : insideParentesisToks.o1) {
            //        ignore.add(object);
            //    }
            //}

            for (IDefinition iDefinition : selected) {
                if (iDefinition instanceof Definition) {
                    Definition definition = (Definition) iDefinition;
                    if (definition.ast != null) {
                        SimpleNode node = definition.ast;
                        if (node instanceof ClassDef) {
                            node = NodeUtils.getClassDefInit((ClassDef) node);
                        }

                        if (node instanceof FunctionDef) {
                            FunctionDef f = (FunctionDef) node;

                            String nodeDocString = NodeUtils.getNodeDocString(definition.ast);

                            if (f.args.args != null) {
                                for (int i = 0; i < f.args.args.length; i++) {
                                    addArg(request, ignore, definition, alreadyChecked, f.args.args, f.args.annotation,
                                            f.args.defaults, i, nodeDocString);
                                }
                            }
                            if (f.args.kwonlyargs != null) {
                                for (int i = 0; i < f.args.kwonlyargs.length; i++) {
                                    addArg(request, ignore, definition, alreadyChecked, f.args.kwonlyargs,
                                            f.args.kwonlyargannotation, f.args.kw_defaults, i, nodeDocString);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addArg(final CompletionRequest request, final Set<String> ignore, final Definition definition,
            final Map<String, IterTokenEntry> alreadyChecked, final exprType[] argName, final exprType[] annotation,
            final exprType[] defaults, final int i, final String nodeDocString) {
        final String rep = NodeUtils.getRepresentationString(argName[i]);
        if (ignore.contains(rep)) {
            return;
        }
        String kwParam = rep + "=";
        SimpleNode paramNode = new NameTok(kwParam, NameTok.KwArg);
        SourceToken sourceToken = new SourceToken(paramNode, kwParam, "", "", "", IToken.TYPE_LOCAL,
                definition.module != null ? definition.module.getNature() : null, definition.module);
        alreadyChecked.put(kwParam, new IterTokenEntry(sourceToken));

        sourceToken.setDocStrCallback((token) -> {
            FastStringBuffer buf = new FastStringBuffer();
            buf.clear();
            buf.append("param: ");
            buf.append(rep);

            if (annotation != null && annotation[i] != null) {
                String val = NodeUtils.printAst(DefaultIndentPrefs.get(null), request.nature, annotation[i], "\n");
                if (val != null) {
                    val = val.strip();
                    if (val.length() > 0) {
                        buf.append(val);
                    }
                }

            }
            if (defaults != null && defaults[i] != null) {
                String val = NodeUtils.printAst(DefaultIndentPrefs.get(null), request.nature, defaults[i], "\n");
                if (val != null) {
                    val = val.strip();
                    if (val.length() > 0) {
                        buf.append(val);
                    }
                }

            }

            if (nodeDocString != null && nodeDocString.length() > 0) {
                buf.append("\n\n");
                buf.append(nodeDocString);
            }
            return buf.toString();
        });
    }

    /**
     * Does a code-completion that will retrieve the globals in the module
     * @throws MisconfigurationException
     */
    private void doGlobalsCompletion(CompletionRequest request, ICodeCompletionASTManager astManager,
            TokensOrProposalsList tokensList, ICompletionState state) throws CompletionRecursionException,
            MisconfigurationException {
        state.setActivationToken(request.activationToken);
        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "astManager.getCompletionsForToken");
            org.python.pydev.shared_core.log.ToLogFile.addLogLevel();
        }

        IModule module = request.getModule();
        if (module == null) {
            org.python.pydev.shared_core.log.ToLogFile.remLogLevel();
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                    "END astManager.getCompletionsForToken: null module");
            return;
        }
        TokensList comps = astManager.getCompletionsForModule(module, state, true, true);
        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            org.python.pydev.shared_core.log.ToLogFile.remLogLevel();
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "END astManager.getCompletionsForToken");
        }
        tokensList.addAll(comps);
        tokensList.addAll(getGlobalsFromParticipants(request, state));
    }

    /**
     * Does a code-completion that will retrieve the all matches for some token in the module
     * @throws MisconfigurationException
     * @throws PythonNatureWithoutProjectException
     * @throws CoreException
     * @throws IOException
     */
    public static void doTokenCompletion(ITokenCompletionRequest request, ICodeCompletionASTManager astManager,
            TokensList tokensList, String trimmed, ICompletionState state) throws CompletionRecursionException,
            MisconfigurationException, IOException, CoreException, PythonNatureWithoutProjectException {

        if (request.getActivationToken().endsWith(".")) {
            request.setActivationToken(
                    request.getActivationToken().substring(0, request.getActivationToken().length() - 1));
        }

        final String initialActivationToken = request.getActivationToken();
        int parI = request.getActivationToken().indexOf('(');
        if (parI != -1) {
            request.setActivationToken(ParsingUtils.removeCalls(request.getActivationToken()));
        }

        char[] toks = new char[] { '.', ' ' };

        boolean lookInGlobals = true;

        if (trimmed.equals("self") || FullRepIterable.getFirstPart(trimmed, toks).equals("self")) {
            lookInGlobals = !getSelfOrClsCompletions(request, tokensList, state, false, true, "self");

        } else if (trimmed.equals("cls") || FullRepIterable.getFirstPart(trimmed, toks).equals("cls")) {
            lookInGlobals = !getSelfOrClsCompletions(request, tokensList, state, false, true, "cls");

        }

        if (lookInGlobals) {
            state.setActivationToken(initialActivationToken);

            //Ok, looking for a token in globals.
            IModule module = request.getModule();
            if (module != null) {
                TokensList comps = astManager.getCompletionsForModule(module, state, true, true);
                tokensList.addAll(comps);
            }
        }
    }

    /**
     * Does a code-completion that will check for imports
     * @throws MisconfigurationException
     */
    private boolean doImportCompletion(CompletionRequest request, ICodeCompletionASTManager astManager,
            TokensOrProposalsList tokensList, ImportInfo importsTipper) throws CompletionRecursionException,
            MisconfigurationException {
        boolean importsTip;
        //get the project and make the code completion!!
        //so, we want to do a code completion for imports...
        //let's see what we have...

        importsTip = true;
        importsTipper.importsTipperStr = importsTipper.importsTipperStr.trim();
        TokensList imports = astManager.getCompletionsForImport(importsTipper, request, false);
        tokensList.addAll(imports);
        return importsTip;
    }

    /**
     * Checks if the python nature is valid
     */
    private void checkPythonNature(IPythonNature pythonNature) {
        if (pythonNature == null) {
            throw new RuntimeException("Unable to get python nature.");
        }
    }

    /**
     * @return completions added from contributors
     * @throws MisconfigurationException
     */
    @SuppressWarnings({ "rawtypes" })
    private TokensOrProposalsList getGlobalsFromParticipants(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        TokensOrProposalsList ret = new TokensOrProposalsList();

        List participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
            ret.addAll(participant.getGlobalCompletions(request, state));
        }
        return ret;
    }

    /**
     * @param request this is the request for the completion
     * @param theList OUT - returned completions are added here. (IToken instances)
     * @param getOnlySupers whether we should only get things from super classes (in this case, we won't get things from the current class)
     * @param checkIfInCorrectScope if true, we'll first check if we're in a scope that actually has a method with 'self' or 'cls'
     *
     * @return true if we actually tried to get the completions for self or cls.
     * @throws MisconfigurationException
     */
    public static boolean getSelfOrClsCompletions(ITokenCompletionRequest request, TokensList theList,
            ICompletionState state,
            boolean getOnlySupers, boolean checkIfInCorrectScope, String lookForRep) throws MisconfigurationException {

        IModule module = request.getModule();
        SimpleNode s = null;
        if (module instanceof SourceModule) {
            SourceModule sourceModule = (SourceModule) module;
            s = sourceModule.getAst();
        }
        if (s != null) {
            FindScopeVisitor visitor = new FindScopeVisitor(state.getLine() + 1, 1, state.getNature(), module);
            try {
                s.accept(visitor);
                if (checkIfInCorrectScope) {
                    boolean scopeCorrect = false;

                    FastStack<ISimpleNode> scopeStack = visitor.scope.getScopeStack();
                    for (Iterator<ISimpleNode> it = scopeStack.topDownIterator(); scopeCorrect == false
                            && it.hasNext();) {
                        ISimpleNode node = it.next();
                        if (node instanceof FunctionDef) {
                            FunctionDef funcDef = (FunctionDef) node;
                            if (funcDef.args != null && funcDef.args.args != null && funcDef.args.args.length > 0) {
                                //ok, we have some arg, let's check for self or cls
                                String rep = NodeUtils.getRepresentationString(funcDef.args.args[0]);
                                if (rep != null && (rep.equals("self") || rep.equals("cls"))) {
                                    scopeCorrect = true;
                                }
                            }
                        }
                    }
                    if (!scopeCorrect) {
                        return false;
                    }
                }
                TokensList tokensList = new TokensList();
                LookingFor lookingFor;
                if (lookForRep.equals("self")) {
                    state.setLookingFor(ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE);
                } else {
                    state.setLookingFor(ICompletionState.LookingFor.LOOKING_FOR_CLASSMETHOD_VARIABLE);
                }
                lookingFor = state.getLookingFor();
                getSelfOrClsCompletions(visitor.scope, request, tokensList, state, getOnlySupers);
                if (tokensList.notEmpty()) {
                    tokensList.setLookingFor(lookingFor);
                    theList.addAll(tokensList);
                }
            } catch (Exception e1) {
                Log.log(e1);
            }
            return true;
        }
        return false;
    }

    /**
     * Get self completions when you already have a scope
     * @throws MisconfigurationException
     */
    public static void getSelfOrClsCompletions(ILocalScope scope, ITokenCompletionRequest request, TokensList theList,
            ICompletionState state, boolean getOnlySupers) throws BadLocationException, MisconfigurationException {
        for (Iterator<ISimpleNode> it = scope.iterator(); it.hasNext();) {

            List<ITypeInfo> possibleClassesForActivationToken = scope
                    .getPossibleClassesForActivationToken(request.getActivationToken());

            if (possibleClassesForActivationToken != null && possibleClassesForActivationToken.size() > 0) {
                ICodeCompletionASTManager astManager = request.getNature().getAstManager();
                IModule module = request.getModule();
                if (module != null) {
                    try {
                        TokensList completions = astManager.getCompletionsFromTokenInLocalScope(module,
                                state.getCopyWithActTok(request.getActivationToken()), false,
                                false, scope);
                        theList.addAll(completions);
                    } catch (CompletionRecursionException e) {
                    }
                }
            }

            ISimpleNode node = it.next();
            if (node instanceof ClassDef) {
                ClassDef d = (ClassDef) node;

                if (getOnlySupers) {
                    for (int i = 0; i < d.bases.length; i++) {
                        if (d.bases[i] instanceof Name) {
                            Name n = (Name) d.bases[i];
                            state.setActivationToken(n.id);
                            TokensList completions;
                            try {
                                ICodeCompletionASTManager astManager = request.getNature().getAstManager();
                                IModule module = request.getModule();
                                if (module != null) {
                                    completions = astManager.getCompletionsForModule(module, state, true, true);
                                    theList.addAll(completions);
                                }
                            } catch (CompletionRecursionException e) {
                                //ok...
                            }
                        }
                    }
                } else {
                    //ok, get the completions for the class, only thing we have to take care now is that we may
                    //not have only 'self' for completion, but something like self.foo.
                    //so, let's analyze our activation token to see what should we do.

                    String trimmed = request.getActivationToken().replace('.', ' ').trim();
                    String[] actTokStrs = trimmed.split(" ");
                    if (actTokStrs.length == 0 || (!actTokStrs[0].equals("self") && !actTokStrs[0].equals("cls"))) {
                        throw new AssertionError(
                                "We need to have at least one token (self or cls) for doing completions in the class.");
                    }

                    if (actTokStrs.length == 1) {
                        //ok, it's just really self, let's get on to get the completions
                        state.setActivationToken(NodeUtils.getNameFromNameTok((NameTok) d.name));
                        try {
                            ICodeCompletionASTManager astManager = request.getNature().getAstManager();
                            IModule module = request.getModule();
                            TokensList completions = astManager.getCompletionsForModule(module, state, true, true);
                            theList.addAll(completions);
                        } catch (CompletionRecursionException e) {
                            //ok
                        }

                    } else {
                        //it's not only self, so, first we have to get the definition of the token
                        //the first one is self, so, just discard it, and go on, token by token to know what is the last
                        //one we are completing (e.g.: self.foo.bar)
                        int line = request.getLine();
                        int col = request.getCol();

                        //ok, try our best shot at getting the module name of the current buffer used in the request.
                        IModule module = request.getModule();
                        AbstractASTManager astMan = ((AbstractASTManager) request.getNature().getAstManager());
                        ICompletionState st = state.getCopyWithActTok(request.getActivationToken(), line, col);
                        st.setQualifier(request.getQualifier());
                        TokensList assignCompletions = new AssignAnalysis().getCompletionsFollowingDefinition(astMan,
                                module, st, scope).completions;
                        theList.addAll(assignCompletions);
                    }
                }
            }
        }
    }

}