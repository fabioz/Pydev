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
package org.python.pydev.editor.codecompletion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.editor.codecompletion.revisited.AssignAnalysis;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.ImmutableTuple;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_core.structure.OrderedMap;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.UIConstants;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion extends AbstractPyCodeCompletion {

    /**
     * Called when a recursion exception is detected.
     */
    public static ICallback<Object, CompletionRecursionException> onCompletionRecursionException;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException,
            BadLocationException, IOException, MisconfigurationException, PythonNatureWithoutProjectException {
        if (request.getPySelection().getCursorLineContents().trim().startsWith("#")) {
            //this may happen if the context is still not correctly computed in python
            return new PyStringCodeCompletion().getCodeCompletionProposals(viewer, request);
        }
        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            Log.toLogFile(this, "Starting getCodeCompletionProposals");
            Log.addLogLevel();
            Log.toLogFile(this, "Request:" + request);
        }

        ArrayList<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();

        //let's see if we should do a code-completion in the current scope...

        //this engine does not work 'correctly' in the default scope on:
        //- class definitions - after 'class' and before '('
        //- method definitions - after 'def' and before '('
        PySelection ps = request.getPySelection();
        int lineCtx = ps.isInDeclarationLine();
        if (lineCtx != PySelection.DECLARATION_NONE) {
            if (lineCtx == PySelection.DECLARATION_METHOD) {
                Image imageOverride = PydevPlugin.getImageCache().get(UIConstants.METHOD_ICON);
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
                                        return ret;
                                    }
                                    //Ok, looking for a token in globals.
                                    IModule module = request.getModule();
                                    if (module == null) {
                                        continue;
                                    }
                                    IToken[] comps = astManager.getCompletionsForModule(module, state, true, true);
                                    for (int i = 0; i < comps.length; i++) {
                                        IToken iToken = comps[i];
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
                                    ret.add(new OverrideMethodCompletionProposal(ps.getAbsoluteCursorOffset(), 0, 0,
                                            imageOverride, functionDef, tokenAndBaseClass.o2, //baseClass
                                            className));
                                }
                            }

                        }
                    }
                }
            }
            request.showTemplates = false;
            return ret;
        }

        try {
            IPythonNature nature = request.nature;
            checkPythonNature(nature);

            ICodeCompletionASTManager astManager = nature.getAstManager();
            if (astManager == null) {
                //we're probably still loading it.
                return ret;
            }

            //list of Object[], IToken or ICompletionProposal
            List<Object> tokensList = new ArrayList<Object>();
            String trimmed = request.activationToken.replace('.', ' ').trim();

            ImportInfo importsTipper = getImportsTipperStr(request);

            int line = request.doc.getLineOfOffset(request.documentOffset);
            IRegion region = request.doc.getLineInformation(line);

            ICompletionState state = new CompletionState(line, request.documentOffset - region.getOffset(), null,
                    request.nature, request.qualifier);
            state.setIsInCalltip(request.isInCalltip);

            Map<String, IToken> alreadyChecked = new HashMap<String, IToken>();

            boolean importsTip = false;

            if (importsTipper.importsTipperStr.length() != 0) {
                //code completion in imports
                request.isInCalltip = false; //if found after (, but in an import, it is not a calltip!
                request.isInMethodKeywordParam = false; //if found after (, but in an import, it is not a calltip!
                importsTip = doImportCompletion(request, astManager, tokensList, importsTipper);

            } else if (trimmed.length() > 0 && request.activationToken.indexOf('.') != -1) {
                //code completion for a token
                doTokenCompletion(request, astManager, tokensList, trimmed, state);
                handleKeywordParam(request, line, alreadyChecked);

            } else {
                //go to globals
                doGlobalsCompletion(request, astManager, tokensList, state);

                //At this point, after doing the globals completion, we may also need to check if we need to show
                //keyword parameters to the user.
                handleKeywordParam(request, line, alreadyChecked);
            }

            String lowerCaseQual = request.qualifier.toLowerCase();
            if (lowerCaseQual.length() >= PyCodeCompletionPreferencesPage.getArgumentsDeepAnalysisNChars()) {
                //this can take some time on the analysis, so, let's let the user choose on how many chars does he
                //want to do the analysis...
                state.pushFindResolveImportMemoryCtx();
                try {
                    if (tokensList.size() > 10000) {
                        Log.logWarn(StringUtils.format(
                                "Warning: computed %s completions (trimming to 10000).\nRequest: %s",
                                tokensList.size(), request));
                        //With too many items it's possible that we have too many removals,
                        //so, switch to a linked list (where removal is fast).
                        tokensList = new LinkedListWarningOnSlowOperations(tokensList.subList(0, 10000));
                    }
                    int i = 0;
                    for (Iterator<Object> it = tokensList.listIterator(); it.hasNext();) {
                        i++;
                        if (i > 10000) {
                            break;
                        }

                        Object o = it.next();
                        if (o instanceof IToken) {
                            it.remove(); // always remove the tokens from the list (they'll be re-added later once they are filtered)

                            IToken initialToken = (IToken) o;

                            IToken token = initialToken;
                            String strRep = token.getRepresentation();
                            IToken prev = alreadyChecked.get(strRep);

                            if (prev != null) {
                                if (prev.getArgs().length() != 0) {
                                    continue; // we already have a version with args... just keep going
                                }
                            }

                            if (!strRep.toLowerCase().startsWith(lowerCaseQual)) {
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

                            alreadyChecked.put(strRep, initialToken);
                        }
                    }

                } finally {
                    state.popFindResolveImportMemoryCtx();
                }
            }

            tokensList.addAll(alreadyChecked.values());
            changeItokenToCompletionPropostal(request, ret, tokensList, importsTip, state);
        } catch (CompletionRecursionException e) {
            if (onCompletionRecursionException != null) {
                onCompletionRecursionException.call(e);
            }
            if (DebugSettings.DEBUG_CODE_COMPLETION) {
                Log.toLogFile(e);
            }
            //PydevPlugin.log(e);
            //ret.add(new CompletionProposal("",request.documentOffset,0,0,null,e.getMessage(), null,null));
        }

        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            Log.remLogLevel();
            Log.toLogFile(this, "Finished completion. Returned:" + ret.size() + " completions.\r\n");
        }

        return ret;
    }

    private void fillTokensWithJediCompletions(CompletionRequest request, PySelection ps, IPythonNature nature,
            ICodeCompletionASTManager astManager, List<Object> tokensList) throws IOException, CoreException,
                    MisconfigurationException, PythonNatureWithoutProjectException {

        try {
            char c = ps.getCharBeforeCurrentOffset();
            if (c == '(') {
                System.out.println("Get call def.");
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }

        AbstractShell shell = AbstractShell.getServerShell(nature, AbstractShell.getShellId());
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
        List<CompiledToken> completions;
        try {
            completions = shell.getJediCompletions(request.editorFile, ps,
                    charset, completePythonPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        tokensList.addAll(completions);
    }

    private void handleKeywordParam(CompletionRequest request, int line, Map<String, IToken> alreadyChecked)
            throws BadLocationException, CompletionRecursionException {
        if (request.isInMethodKeywordParam) {

            PySelection ps = new PySelection(request.doc, request.offsetForKeywordParam);
            RefactoringRequest findRequest = new RefactoringRequest(request.editorFile, ps, new NullProgressMonitor(),
                    request.nature, null);
            ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
            PyRefactoringFindDefinition.findActualDefinition(findRequest, new CompletionCache(), selected);

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
                        String args = NodeUtils.getNodeArgs(definition.ast);
                        String fullArgs = NodeUtils.getFullArgs(definition.ast);
                        StringTokenizer tokenizer = new StringTokenizer(args, "(, )");
                        while (tokenizer.hasMoreTokens()) {
                            String nextToken = tokenizer.nextToken();
                            if (ignore.contains(nextToken)) {
                                continue;
                            }
                            String kwParam = nextToken + "=";
                            SimpleNode node = new NameTok(kwParam, NameTok.KwArg);
                            SourceToken sourceToken = new SourceToken(node, kwParam, "", "", "", IToken.TYPE_LOCAL);
                            sourceToken.setDocStr(fullArgs);
                            alreadyChecked.put(kwParam, sourceToken);
                        }
                    }
                }
            }
        }
    }

    /**
     * Does a code-completion that will retrieve the globals in the module
     * @throws MisconfigurationException
     */
    private void doGlobalsCompletion(CompletionRequest request, ICodeCompletionASTManager astManager,
            List<Object> tokensList, ICompletionState state) throws CompletionRecursionException,
                    MisconfigurationException {
        state.setActivationToken(request.activationToken);
        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            Log.toLogFile(this, "astManager.getCompletionsForToken");
            Log.addLogLevel();
        }

        IModule module = request.getModule();
        if (module == null) {
            Log.remLogLevel();
            Log.toLogFile(this, "END astManager.getCompletionsForToken: null module");
            return;
        }
        IToken[] comps = astManager.getCompletionsForModule(module, state, true, true);
        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            Log.remLogLevel();
            Log.toLogFile(this, "END astManager.getCompletionsForToken");
        }

        for (int i = 0; i < comps.length; i++) {
            tokensList.add(comps[i]);
        }
        tokensList.addAll(getGlobalsFromParticipants(request, state));
    }

    /**
     * Does a code-completion that will retrieve the all matches for some token in the module
     * @throws MisconfigurationException
     * @throws PythonNatureWithoutProjectException
     * @throws CoreException
     * @throws IOException
     */
    private void doTokenCompletion(CompletionRequest request, ICodeCompletionASTManager astManager,
            List<Object> tokensList, String trimmed, ICompletionState state) throws CompletionRecursionException,
                    MisconfigurationException, IOException, CoreException, PythonNatureWithoutProjectException {
        if (false) { //disabled for now.
            fillTokensWithJediCompletions(request, request.getPySelection(), request.nature, astManager, tokensList);
            return;
        }

        if (request.activationToken.endsWith(".")) {
            request.activationToken = request.activationToken.substring(0, request.activationToken.length() - 1);
        }

        final String initialActivationToken = request.activationToken;
        int parI = request.activationToken.indexOf('(');
        if (parI != -1) {
            request.activationToken = request.activationToken.substring(0, parI);
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
                IToken[] comps = astManager.getCompletionsForModule(module, state, true, true);
                for (int i = 0; i < comps.length; i++) {
                    tokensList.add(comps[i]);
                }
            }
        }
    }

    /**
     * Does a code-completion that will check for imports
     * @throws MisconfigurationException
     */
    private boolean doImportCompletion(CompletionRequest request, ICodeCompletionASTManager astManager,
            List<Object> tokensList, ImportInfo importsTipper) throws CompletionRecursionException,
                    MisconfigurationException {
        boolean importsTip;
        //get the project and make the code completion!!
        //so, we want to do a code completion for imports...
        //let's see what we have...

        importsTip = true;
        importsTipper.importsTipperStr = importsTipper.importsTipperStr.trim();
        IToken[] imports = astManager.getCompletionsForImport(importsTipper, request, false);
        for (int i = 0; i < imports.length; i++) {
            tokensList.add(imports[i]);
        }
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Collection<Object> getGlobalsFromParticipants(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        ArrayList ret = new ArrayList();

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
    @SuppressWarnings("unchecked")
    public static boolean getSelfOrClsCompletions(CompletionRequest request, List theList, ICompletionState state,
            boolean getOnlySupers, boolean checkIfInCorrectScope, String lookForRep) throws MisconfigurationException {

        IModule module = request.getModule();
        SimpleNode s = null;
        if (module instanceof SourceModule) {
            SourceModule sourceModule = (SourceModule) module;
            s = sourceModule.getAst();
        }
        if (s != null) {
            FindScopeVisitor visitor = new FindScopeVisitor(state.getLine(), 0);
            try {
                s.accept(visitor);
                if (checkIfInCorrectScope) {
                    boolean scopeCorrect = false;

                    FastStack<SimpleNode> scopeStack = visitor.scope.getScopeStack();
                    for (Iterator<SimpleNode> it = scopeStack.topDownIterator(); scopeCorrect == false
                            && it.hasNext();) {
                        SimpleNode node = it.next();
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
                if (lookForRep.equals("self")) {
                    state.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                } else {
                    state.setLookingFor(ICompletionState.LOOKING_FOR_CLASSMETHOD_VARIABLE);
                }
                getSelfOrClsCompletions(visitor.scope, request, theList, state, getOnlySupers);
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void getSelfOrClsCompletions(ILocalScope scope, CompletionRequest request, List theList,
            ICompletionState state, boolean getOnlySupers) throws BadLocationException, MisconfigurationException {
        for (Iterator<SimpleNode> it = scope.iterator(); it.hasNext();) {
            SimpleNode node = it.next();
            if (node instanceof ClassDef) {
                ClassDef d = (ClassDef) node;

                if (getOnlySupers) {
                    for (int i = 0; i < d.bases.length; i++) {
                        if (d.bases[i] instanceof Name) {
                            Name n = (Name) d.bases[i];
                            state.setActivationToken(n.id);
                            IToken[] completions;
                            try {
                                ICodeCompletionASTManager astManager = request.nature.getAstManager();
                                IModule module = request.getModule();
                                if (module != null) {
                                    completions = astManager.getCompletionsForModule(module, state, true, true);
                                    for (int j = 0; j < completions.length; j++) {
                                        theList.add(completions[j]);
                                    }
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

                    String trimmed = request.activationToken.replace('.', ' ').trim();
                    String[] actTokStrs = trimmed.split(" ");
                    if (actTokStrs.length == 0 || (!actTokStrs[0].equals("self") && !actTokStrs[0].equals("cls"))) {
                        throw new AssertionError(
                                "We need to have at least one token (self or cls) for doing completions in the class.");
                    }

                    if (actTokStrs.length == 1) {
                        //ok, it's just really self, let's get on to get the completions
                        state.setActivationToken(NodeUtils.getNameFromNameTok((NameTok) d.name));
                        try {
                            ICodeCompletionASTManager astManager = request.nature.getAstManager();
                            IModule module = request.getModule();
                            IToken[] completions = astManager.getCompletionsForModule(module, state, true, true);
                            for (int j = 0; j < completions.length; j++) {
                                theList.add(completions[j]);
                            }
                        } catch (CompletionRecursionException e) {
                            //ok
                        }

                    } else {
                        //it's not only self, so, first we have to get the definition of the token
                        //the first one is self, so, just discard it, and go on, token by token to know what is the last
                        //one we are completing (e.g.: self.foo.bar)
                        int line = request.doc.getLineOfOffset(request.documentOffset);
                        IRegion region = request.doc.getLineInformationOfOffset(request.documentOffset);
                        int col = request.documentOffset - region.getOffset();

                        //ok, try our best shot at getting the module name of the current buffer used in the request.
                        IModule module = request.getModule();

                        AbstractASTManager astMan = ((AbstractASTManager) request.nature.getAstManager());
                        theList.addAll(new AssignAnalysis().getAssignCompletions(astMan, module, new CompletionState(
                                line, col, request.activationToken, request.nature, request.qualifier),
                                scope).completions);
                    }
                }
            }
        }
    }

}