/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.modules.ASTEntryWithSourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;
import com.python.pydev.analysis.scopeanalysis.ScopeAnalyzerVisitor;
import com.python.pydev.analysis.visitors.Found;

/**
 * The rename import process is used when we find that we have to rename a module.
 */
public class PyRenameImportProcess extends AbstractRenameWorkspaceRefactorProcess {

    public static final int TYPE_RENAME_MODULE = 1;
    public static final int TYPE_RENAME_UNRESOLVED_IMPORT = 2;

    protected int type = -1;

    /**
     * The module for which we're looking for references
     */
    protected SourceModule moduleToFind;

    /**
     * @param definition this is the definition we're interested in.
     */
    public PyRenameImportProcess(Definition definition) {
        super(definition);
        if (definition.ast == null) {
            this.type = TYPE_RENAME_MODULE;
        } else {
            this.type = TYPE_RENAME_UNRESOLVED_IMPORT;
        }
    }

    @Override
    public void findReferencesToRename(RefactoringRequest request, RefactoringStatus status) {
        if (request.isModuleRenameRefactoringRequest() && request.getSimpleResourceRename()
                && request.getIFileResource() != null) {
            return;

        }
        super.findReferencesToRename(request, status);
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        if (request.isModuleRenameRefactoringRequest()) {
            onModuleRenameRefactoringRequest(request);
        }

        List<ASTEntry> oc = getOccurrencesWithScopeAnalyzer(request, (SourceModule) request.getModule());
        SimpleNode root = request.getAST();
        if (oc.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            oc.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, root));
            oc.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, root));
        }

        addOccurrences(request, oc);
    }

    private void onModuleRenameRefactoringRequest(RefactoringRequest request) {
        moduleToFind = (SourceModule) request.getModule();
        List<ASTEntry> lst = new ArrayList<ASTEntry>();
        lst.add(new ASTEntryWithSourceModule(moduleToFind));
        addOccurrences(lst, moduleToFind.getFile(), moduleToFind.getName());
    }

    @Override
    protected void doCheckInitialOnWorkspace(RefactoringStatus status, RefactoringRequest request) {

        boolean wasResolved = false;

        if (request.isModuleRenameRefactoringRequest()) {
            onModuleRenameRefactoringRequest(request);
            wasResolved = true;

        } else if (docOccurrences.size() != 0) {
            //now, on the workspace, we need to find the module definition as well as the imports for it...
            //the local scope should have already determined which is the module to be renamed (unless it
            //is an unresolved import, in which case we'll only make a local refactor)

            ASTEntry entry = docOccurrences.iterator().next();
            Found found = (Found) entry
                    .getAdditionalInfo(ScopeAnalyzerVisitor.FOUND_ADDITIONAL_INFO_IN_AST_ENTRY, null);
            if (found == null) {
                throw new RuntimeException("Expecting decorated entry.");
            }
            if (found.importInfo == null) {
                throw new RuntimeException("Expecting import info from the found entry.");
            }
            if (found.importInfo.wasResolved) {
                Definition d = found.importInfo
                        .getModuleDefinitionFromImportInfo(request.nature, new CompletionCache());
                if (d == null || d.module == null) {
                    status.addFatalError(org.python.pydev.shared_core.string.StringUtils
                            .format("Unable to find the definition for the module."));
                    return;
                }
                if (!(d.module instanceof SourceModule)) {
                    status.addFatalError(StringUtils.format(
                            "Only source modules may be renamed (the module %s was found as a %s module)",
                            d.module.getName(), d.module.getClass()));
                    return;
                }

                this.moduleToFind = (SourceModule) d.module;
                wasResolved = true;

                //it cannot be a compiled extension
                if (!(found.importInfo.mod instanceof SourceModule)) {
                    status.addFatalError(StringUtils.format(
                            "Error. The module %s may not be renamed\n"
                                    + "(Because it was found as a compiled extension).", found.importInfo.mod.getName()));
                    return;
                }

                //nor be a system module
                ISystemModulesManager systemModulesManager = request.nature.getAstManager().getModulesManager()
                        .getSystemModulesManager();
                IModule systemModule = systemModulesManager.getModule(found.importInfo.mod.getName(), request.nature,
                        true);
                if (systemModule != null) {
                    status.addFatalError(StringUtils.format(
                            "Error. The module '%s' may not be renamed\n"
                                    + "Only project modules may be renamed\n"
                                    + "(and it was found as being a system module).",
                            found.importInfo.mod.getName()));
                    return;
                }

                List<ASTEntry> lst = new ArrayList<ASTEntry>();
                lst.add(new ASTEntryWithSourceModule(moduleToFind));
                addOccurrences(lst, moduleToFind.getFile(), moduleToFind.getName());
            }
        }

        if (wasResolved) {
            //now, if we've been able to resolve it, let's keep on with the 'default' way of getting workspace occurrences
            //(if we haven't been able to resolve it, there's no way to find matching imports in the workspace)
            super.doCheckInitialOnWorkspace(status, request);
        }
    }

    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        List<ASTEntry> entryOccurrences = new ArrayList<ASTEntry>();

        try {
            checkProperRequest();

            MatchImportsVisitor visitor = new MatchImportsVisitor(request.nature, request.initialName, module,
                    request.getMonitor());

            SimpleNode root = module.getAst();
            root.accept(visitor);
            entryOccurrences = visitor.getEntryOccurrences();
            if (entryOccurrences.size() > 0) {
                Set<String> searchStringsAs = visitor.searchStringsAs;
                //only add comments and strings if there's at least some other occurrence
                for (String string : searchStringsAs) {
                    entryOccurrences.addAll(convertToUseInitialName(string,
                            ScopeAnalysis.getCommentOccurrences(string, root)));

                    entryOccurrences.addAll(convertToUseInitialName(string,
                            ScopeAnalysis.getStringOccurrences(string, root)));

                }
            }
            //Look for the full match on all strings or comments in this case.
            entryOccurrences.addAll(convertToUseInitialName(request.initialName,
                    ScopeAnalysis.getCommentOccurrences(request.initialName, root)));

            entryOccurrences.addAll(convertToUseInitialName(request.initialName,
                    ScopeAnalysis.getStringOccurrences(request.initialName, root)));

        } catch (Exception e) {
            Log.log(e);
        }
        return entryOccurrences;
    }

    private Collection<ASTEntry> convertToUseInitialName(String string, List<ASTEntry> commentOccurrences) {
        ArrayList<ASTEntry> lst = new ArrayList<>(commentOccurrences.size());
        for (ASTEntry astEntry : commentOccurrences) {
            lst.add(new FixedInputStringASTEntry(string, null, astEntry.node, false));
        }
        return lst;
    }

    public static final class FixedInputStringASTEntry extends ASTEntry implements IRefactorCustomEntry {
        private final String fixedInitialString;
        private final boolean forceFull;

        public FixedInputStringASTEntry(String s, ASTEntry parent, SimpleNode node, boolean forceFull) {
            super(parent, node);
            this.fixedInitialString = s;
            this.forceFull = forceFull;
        }

        @Override
        public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                RefactoringStatus status, IPath file, IPythonNature nature) {
            initialName = fixedInitialString;
            if (!initialName.contains(".") && !this.forceFull) {
                inputName = FullRepIterable.getLastPart(inputName);
            }

            int offset = AbstractRenameRefactorProcess.getOffset(doc, this);
            TextEditCreation.checkExpectedInput(doc, node.beginLine, offset, initialName, status, file);
            TextEdit replaceEdit = new ReplaceEdit(offset, initialName.length(), inputName);
            List<TextEdit> edits = Arrays.asList(replaceEdit);
            return edits;
        }
    }

    protected void checkProperRequest() throws AssertionError {
        if (!(request.isModuleRenameRefactoringRequest())) {
            throw new AssertionError("To rename an import, a ModuleRenameRefactoringRequest is needed.");
        }
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

}
