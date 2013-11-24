package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.StringUtils;

public class MatchImportsVisitor extends VisitorBase {

    private static final class ImportFromModPartRenameAstEntry extends ImportRenameAstEntry {
        private ImportFromModPartRenameAstEntry(ASTEntry parent, SimpleNode node) {
            super(parent, node);
        }

        @Override
        public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                RefactoringStatus status, IFile file) {
            //Simple one: just the first part has to be changed.
            ImportFrom f = (ImportFrom) this.node;
            int offset = PySelection
                    .getAbsoluteCursorOffset(doc, f.module.beginLine - 1, f.module.beginColumn - 1);

            TextEditCreation.checkExpectedInput(doc, this.node.beginLine, offset, initialName, status, file);
            TextEdit replaceEdit = new ReplaceEdit(offset, initialName.length(), inputName);
            return Arrays.asList(replaceEdit);
        }
    }

    private static final class ImportFromRenameAstEntry extends ImportRenameAstEntry {
        private ImportFromRenameAstEntry(ASTEntry parent, SimpleNode node) {
            super(parent, node);
        }

        @Override
        public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                RefactoringStatus status, IFile file) {
            throw new RuntimeException("not implemented");
        }
    }

    private static final class DirectImportRenameAstEntry extends ImportRenameAstEntry {
        private DirectImportRenameAstEntry(ASTEntry parent, SimpleNode node) {
            super(parent, node);
        }

        @Override
        public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                RefactoringStatus status, IFile file) {
            throw new RuntimeException("not implemented");
        }
    }

    private IPythonNature nature;
    private String initialModuleName;
    private SourceModule currentModule;

    public final List<ImportFrom> importFromsMatchingOnModulePart = new ArrayList<>();
    public final List<ImportFrom> importFromsMatchingOnAliasPart = new ArrayList<>();
    public final List<Import> importsMatchingOnAliasPart = new ArrayList<>();
    public final List<ASTEntry> occurrences = new ArrayList<>();
    public final Set<String> searchStringsAs = new HashSet<>();
    private ICompletionState completionState;
    private IProgressMonitor monitor;
    private String lastPart;

    public MatchImportsVisitor(IPythonNature nature, String initialName, SourceModule module, IProgressMonitor monitor) {
        this.nature = nature;
        this.initialModuleName = getWithoutInit(initialName);
        this.currentModule = module;
        completionState = CompletionStateFactory
                .getEmptyCompletionState(nature, new CompletionCache());
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        this.monitor = monitor;
        this.lastPart = FullRepIterable.getLastPart(this.initialModuleName);
    }

    protected String getModuleNameLastPart() {
        return lastPart;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        Object ret = super.visitAttribute(node);
        NameTok attr = (NameTok) node.attr;
        if (attr.ctx == NameTok.Attrib) {
            if (attr.id.equals(getModuleNameLastPart())) {
                String checkName = NodeUtils.getFullRepresentationString(node);
                if (checkIndirectReferenceFromDefinition(checkName, true, new ASTEntry(null, attr),
                        attr.beginColumn,
                        attr.beginLine)) {
                    return true;
                }

                System.out.println("Check attribute: " + checkName);
            }
        }
        return ret;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        int level = node.level;
        String modRep = NodeUtils.getRepresentationString(node.module);

        HashSet<String> s = new HashSet<>();
        if (level > 0) {
            //Ok, direct match didn't work, so, let's check relative imports
            modRep = makeRelative(level, modRep);
            s.add(modRep);
        } else {
            //Treat imports as relative on Python 2.x variants without the from __future__ import absolute_import statement.
            if (nature.getGrammarVersion() < IPythonNature.GRAMMAR_PYTHON_VERSION_3_0) {
                s.add(modRep);
                s.add(makeRelative(1, modRep));
            }
        }

        boolean matched = false;
        for (String modRep2 : s) {
            if (!matched) {
                if (modRep2.equals(this.initialModuleName) || (modRep2 + ".").startsWith(initialModuleName)) {
                    //Ok, if the first part matched, no need to check other things (i.e.: rename only the from "xxx.yyy" part)
                    importFromsMatchingOnModulePart.add(node);
                    occurrences.add(new ImportFromModPartRenameAstEntry(null, node));
                    //Found a match
                    matched = true;
                }
            }
        }

        for (String modRep2 : s) {
            if (!matched) {
                matched = handleNames(node, node.names, modRep2);
            }
        }

        return null;
    }

    protected String makeRelative(int level, String modRep) {
        String parentPackage = this.currentModule.getName();
        List<String> moduleParts = StringUtils.split(parentPackage, '.');

        if (moduleParts.size() > level) {
            String relative = FullRepIterable.joinParts(moduleParts, moduleParts.size() - level);
            if (modRep.isEmpty()) {
                modRep = relative;
            } else {
                modRep = StringUtils.join(".", relative, modRep);
            }
        }
        return modRep;
    }

    public boolean handleNames(SimpleNode node, aliasType[] names, String modRep) {
        if (names != null && names.length > 0) {
            //not wild import!
            for (int i = 0; i < names.length; i++) {
                aliasType aliasType = names[i];
                NameTok name = (NameTok) aliasType.name;
                String full;
                String nameInImport = name.id;
                if (modRep != null && modRep.length() > 0) {
                    full = StringUtils.join(".", modRep, nameInImport);
                } else {
                    full = nameInImport;
                }
                boolean addAsSearchString = aliasType.asname == null;
                ImportFromRenameAstEntry renameAstEntry = new ImportFromRenameAstEntry(null, node);
                if (full.equals(this.initialModuleName) || (full + ".").startsWith(initialModuleName)) {
                    //Ok, this match is a bit more tricky: we matched it, but we need to rename a part before and after the from xxx.yyy import zzz part
                    //also, we must take care not to destroy any alias in the process or other imports which may be joined with this one (the easiest part
                    //is probably removing the whole import and re-writing everything again).
                    if (node instanceof ImportFrom) {
                        importFromsMatchingOnAliasPart.add((ImportFrom) node);
                        occurrences.add(renameAstEntry);
                        if (addAsSearchString) {
                            searchStringsAs.add(nameInImport);
                        }

                    } else if (node instanceof Import) {
                        importsMatchingOnAliasPart.add((Import) node);
                        occurrences.add(new DirectImportRenameAstEntry(null, node));
                        if (addAsSearchString) {
                            searchStringsAs.add(nameInImport);
                        }
                    }
                    return true;
                }
                if (nameInImport.equals(getModuleNameLastPart())) {
                    if (checkIndirectReferenceFromDefinition(nameInImport, addAsSearchString, renameAstEntry,
                            node.beginColumn,
                            node.beginLine)) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    protected boolean checkIndirectReferenceFromDefinition(String nameInImport, boolean addAsSearchString,
            ASTEntry renameAstEntry, int beginColumn, int beginLine) {
        ArrayList<IDefinition> definitions = new ArrayList<>();
        try {
            PyRefactoringFindDefinition.findActualDefinition(monitor, this.currentModule,
                    nameInImport, definitions, beginLine,
                    beginColumn, nature, this.completionState);
            for (IDefinition iDefinition : definitions) {
                String modName = getWithoutInit(iDefinition.getModule().getName());
                if (modName.equals(this.initialModuleName)) {
                    occurrences.add(renameAstEntry);
                    if (addAsSearchString) {
                        searchStringsAs.add(nameInImport);
                    }
                    return true;
                }
            }
        } catch (CompletionRecursionException e) {
            Log.log(e);
        } catch (Exception e) {
            Log.log(e);
        }
        return false;
    }

    private String getWithoutInit(String initialName) {
        if (initialName.endsWith(".__init__")) {
            initialName = initialName.substring(0, initialName.length() - 9);
        }
        return initialName;
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        aliasType[] names = node.names;
        boolean matched = handleNames(node, names, "");
        //Treat imports as relative on Python 2.x variants without the from __future__ import absolute_import statement.
        if (!matched && nature.getGrammarVersion() < IPythonNature.GRAMMAR_PYTHON_VERSION_3_0) {
            String relative = makeRelative(1, "");
            handleNames(node, names, relative);
        }
        return null;
    }

    public List<ASTEntry> getEntryOccurrences() {
        return occurrences;
    }
}
