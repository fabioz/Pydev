package com.python.pydev.refactoring.wizards.rename;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
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
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class MatchImportsVisitor extends VisitorBase {

    private static final class ImportFromModPartRenameAstEntry extends ImportRenameAstEntry {
        /**
         * I.e.: the name which was matched (it may be different from the import module part because if it's found in
         * a relative import, it may be actually matched in absolute form).
         */
        private String matchedAs;
        private String initialModuleName;

        private ImportFromModPartRenameAstEntry(ASTEntry parent, ImportFrom node, String matchedAs,
                String initialModuleName) {
            super(parent, node);
            this.matchedAs = matchedAs;
            this.initialModuleName = initialModuleName;
        }

        @Override
        public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                RefactoringStatus status, IPath file, IPythonNature nature) {
            //Simple one: just the first part has to be changed.
            ImportFrom f = (ImportFrom) this.node;

            //The actual initial name
            String modId = ((NameTok) f.module).id;
            if (!(modId + ".").startsWith(initialName)) {
                initialName = modId;
            }
            int offset = PySelection
                    .getAbsoluteCursorOffset(doc, f.module.beginLine - 1, f.module.beginColumn - 1);

            TextEditCreation.checkExpectedInput(doc, this.node.beginLine, offset, initialName, status, file);

            //-f.level because we'll make the import absolute now!
            TextEdit replaceEdit = new ReplaceEdit(offset - f.level, initialName.length() + f.level, inputName);
            return Arrays.asList(replaceEdit);
        }
    }

    private static final class ImportFromRenameAstEntry extends ImportRenameAstEntry {
        public Set<Integer> indexes;

        private ImportFromRenameAstEntry(ASTEntry parent, SimpleNode node) {
            super(parent, node);
            Assert.isTrue(node instanceof ImportFrom || node instanceof Import);
        }

        @Override
        public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                RefactoringStatus status, IPath file, IPythonNature nature) {
            String line = PySelection.getLine(doc, this.node.beginLine - 1);
            ArrayList<TextEdit> ret = new ArrayList<>();

            //Ok, this is a bit more tricky: we have a from import where we may have to change 2 parts: the from and import...
            //For this use case, we'll create a copy, change it, rewrite the ast and change the whole thing.

            stmtType importFrom = (stmtType) this.node;
            stmtType copied = (stmtType) importFrom.createCopy(false);

            //Make things from back to forward to keep indexes valid.
            ArrayList<Integer> sorted = new ArrayList<Integer>(indexes);
            Collections.sort(sorted);
            Collections.reverse(sorted);

            List<stmtType> body = new ArrayList<stmtType>();

            ArrayList<aliasType> names = new ArrayList<aliasType>();
            List<Import> forcedImports = new ArrayList<>();

            for (int aliasIndex : indexes) {
                aliasType[] copiedNodeNames = getNames(copied);
                aliasType alias = copiedNodeNames[aliasIndex];

                //Just removing the names from the copied (as it may have to be added if some other parts are
                //not affected).
                setNames(copied, ArrayUtils.remove(copiedNodeNames, aliasIndex, aliasType.class));

                String full = getFull(importFrom, (NameTok) alias.name);
                String firstPart;

                //If it was an import a.b, keep it as an import (if it's dotted)
                boolean forceImport = importFrom instanceof Import && full.contains(".");
                if (forceImport) {
                    firstPart = inputName;

                } else {
                    //Otherwise, just put the last part
                    firstPart = FullRepIterable.getLastPart(inputName);
                }

                if (full.startsWith(initialName + ".")) {
                    NameTok t = (NameTok) alias.name;
                    t.id = firstPart + "." + full.substring(initialName.length() + 1);

                } else {
                    NameTok t = (NameTok) alias.name;
                    t.id = firstPart;
                }

                if (forceImport) {
                    forcedImports.add(new Import(new aliasType[] { alias }));
                } else {
                    names.add(alias);
                }
            }
            if (forcedImports.size() > 0) {
                body.addAll(forcedImports);

            }
            if (names.size() > 0) {
                if (inputName.indexOf(".") == -1) {
                    body.add(new Import(names.toArray(new aliasType[names.size()])));
                } else {
                    String[] headAndTail = FullRepIterable.headAndTail(inputName);
                    NameTokType nameTok = new NameTok(headAndTail[0], NameTok.ImportModule);
                    body.add(new ImportFrom(nameTok, names.toArray(new aliasType[names.size()]), 0));
                }
            }
            if (getNames(copied).length > 0) {
                body.add(0, copied);
            }

            Module module = new Module(body.toArray(new stmtType[body.size()]));

            //We'll change all
            String delimiter = PySelection.getDelimiter(doc);
            PrettyPrinterPrefsV2 prefsV2 = PrettyPrinterV2.createDefaultPrefs(nature, DefaultIndentPrefs.get(nature),
                    delimiter);

            PrettyPrinterV2 prettyPrinterV2 = new PrettyPrinterV2(prefsV2);
            String str = null;
            try {
                try {
                    MakeAstValidForPrettyPrintingVisitor.makeValid(module);
                } catch (Exception e) {
                    Log.log(e);
                }
                str = prettyPrinterV2.print(module);

            } catch (IOException e) {
                status.addFatalError("Unexpected exception: " + e.getMessage());
                Log.log(e);
            }
            if (str != null) {
                str = StringUtils.rightTrim(str);
                int offset;
                try {
                    offset = doc.getLineOffset(this.node.beginLine - 1);
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
                int firstCharPosition = TextSelectionUtils.getFirstCharPosition(line);
                if (firstCharPosition > 0) {
                    str = line.substring(0, firstCharPosition) + str;
                }
                TextEdit replaceEdit = new ReplaceEdit(offset, line.length(), str);
                ret.add(replaceEdit);
            }

            // System.out.println(line);
            // System.out.println(file);
            // System.out.println("ImportFromRenameAstEntry.createRenameEdit: " + initialName + " to " + inputName);
            // System.out.println("");
            return ret;
        }

        private String getFull(stmtType imp, NameTok name) {
            if (imp instanceof ImportFrom) {
                ImportFrom importFrom = (ImportFrom) imp;
                return ((NameTok) importFrom.module).id + "." + name.id;
            }
            return name.id;
        }

        private void setNames(SimpleNode copied, aliasType[] arr) {
            if (copied instanceof ImportFrom) {
                ((ImportFrom) copied).names = arr;
                return;
            }
            if (copied instanceof Import) {
                ((Import) copied).names = arr;
                return;
            }
            throw new AssertionError("Expected Import or ImportFrom. Found: " + copied.getClass());
        }

        private aliasType[] getNames(SimpleNode copied) {
            if (copied instanceof ImportFrom) {
                return ((ImportFrom) copied).names;
            }
            if (copied instanceof Import) {
                return ((Import) copied).names;
            }
            throw new AssertionError("Expected Import or ImportFrom. Found: " + copied.getClass());
        }
    }

    private static final class AttributeASTEntry extends ASTEntry implements IRefactorCustomEntry {
        private final String fixedInitialString;
        private final boolean fullAttrMatch;

        private AttributeASTEntry(String initial, SimpleNode node, boolean fullAttrMatch) {
            super(null, node);
            this.fixedInitialString = initial;
            this.fullAttrMatch = fullAttrMatch;
        }

        @Override
        public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                RefactoringStatus status, IPath file, IPythonNature nature) {
            initialName = fixedInitialString;
            if (!fullAttrMatch) {
                inputName = FullRepIterable.getLastPart(inputName);
            }

            int offset = AbstractRenameRefactorProcess.getOffset(doc, this);
            TextEditCreation.checkExpectedInput(doc, node.beginLine, offset, initialName, status, file);
            TextEdit replaceEdit = new ReplaceEdit(offset, initialName.length(), inputName);
            List<TextEdit> edits = Arrays.asList(replaceEdit);
            return edits;
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
    private FastStack<SimpleNode> stack = new FastStack<>(10);

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
    public Object visitModule(Module node) throws Exception {
        stack.push(node);
        super.visitModule(node);
        stack.pop();
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        stack.push(node);
        super.visitFunctionDef(node);
        stack.pop();
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        stack.push(node);
        super.visitClassDef(node);
        stack.pop();
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        Object ret = super.visitAttribute(node);
        NameTok attr = (NameTok) node.attr;
        if (attr.ctx == NameTok.Attrib) {
            if (attr.id.equals(getModuleNameLastPart())) {
                String checkName = NodeUtils.getFullRepresentationString(node);
                AttributeASTEntry entry;

                if (checkName.equals(this.initialModuleName)) {
                    //The full attribute matches (i.e.: a.b)
                    List<SimpleNode> parts = NodeUtils.getAttributeParts(node);

                    entry = new AttributeASTEntry(checkName, parts.get(0), true);
                } else {
                    //Only the last part matches (.b)
                    entry = new AttributeASTEntry(attr.id, attr, false);
                }

                if (checkIndirectReferenceFromDefinition(checkName, true, entry,
                        attr.beginColumn,
                        attr.beginLine)) {
                    return true;
                }
            }
        }
        return ret;
    }

    private boolean acceptOnlyAbsoluteImports = false;

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        int level = node.level;
        String modRep = NodeUtils.getRepresentationString(node.module);
        if ("__future__".equals(modRep)) {
            if (node.names != null && node.names.length == 1) {
                aliasType aliasType = node.names[0];
                if ("absolute_import".equals(((NameTok) aliasType.name).id)) {
                    acceptOnlyAbsoluteImports = true;
                }
            }
        }

        HashSet<Tuple<String, Boolean>> s = new HashSet<>(); //the module and whether it's relative
        if (level > 0) {
            //Ok, direct match didn't work, so, let's check relative imports
            modRep = makeRelative(level, modRep);
            s.add(new Tuple<String, Boolean>(modRep, false));
        } else {
            //Treat imports as relative on Python 2.x variants without the from __future__ import absolute_import statement.
            if (nature.getGrammarVersion() < IPythonNature.GRAMMAR_PYTHON_VERSION_3_0 && !acceptOnlyAbsoluteImports) {
                s.add(new Tuple<String, Boolean>(modRep, false));
                s.add(new Tuple<String, Boolean>(makeRelative(1, modRep), true));
            }
        }

        boolean matched = false;

        for (Tuple<String, Boolean> modRep2 : s) {
            if (!matched) {
                //try to check full name first
                matched = handleNames(node, node.names, modRep2.o1, true);
            }
        }

        if (!matched) {
            //check partial in module later

            for (Tuple<String, Boolean> tup : s) {
                if (!matched) {
                    String modRep2 = tup.o1;
                    boolean isRelative = tup.o2;
                    if (modRep2.equals(this.initialModuleName)
                            || (!isRelative && (modRep2 + ".").startsWith(initialModuleName + "."))) {
                        //Ok, if the first part matched, no need to check other things (i.e.: rename only the from "xxx.yyy" part)
                        importFromsMatchingOnModulePart.add(node);
                        occurrences.add(new ImportFromModPartRenameAstEntry(null, node, modRep2, initialModuleName));
                        //Found a match
                        matched = true;
                        break;
                    }
                }
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

    public boolean handleNames(SimpleNode node, aliasType[] names, String modRep, boolean onlyFullMatch) {
        boolean handled = false;
        if (names != null && names.length > 0) {
            //not wild import!

            Set<Integer> aliasesHandled = new TreeSet<>();
            ImportFromRenameAstEntry renameAstEntry = new ImportFromRenameAstEntry(null, node);

            for (int i = 0; i < names.length; i++) {
                aliasType aliasType = names[i];
                NameTok name = (NameTok) aliasType.name;
                String full;
                final String nameInImport = name.id;
                if (modRep != null && modRep.length() > 0) {
                    full = StringUtils.join(".", modRep, nameInImport);
                } else {
                    full = nameInImport;
                }
                boolean addAsSearchString = aliasType.asname == null;
                boolean equals = full.equals(this.initialModuleName);
                boolean startsWith = (full + ".").startsWith(initialModuleName);

                if (equals || (startsWith && !onlyFullMatch)) {
                    //Ok, this match is a bit more tricky: we matched it, but we need to rename a part before and after the from xxx.yyy import zzz part
                    //also, we must take care not to destroy any alias in the process or other imports which may be joined with this one (the easiest part
                    //is probably removing the whole import and re-writing everything again).
                    if (node instanceof ImportFrom) {
                        importFromsMatchingOnAliasPart.add((ImportFrom) node);
                        aliasesHandled.add(i);
                        if (addAsSearchString) {
                            searchStringsAs.add(nameInImport);
                        }

                    } else if (node instanceof Import) {
                        importsMatchingOnAliasPart.add((Import) node);
                        aliasesHandled.add(i);
                        if (addAsSearchString) {
                            searchStringsAs.add(nameInImport);
                        }
                    }

                    if (aliasType.asname == null) {
                        boolean partialInImportStatement = node instanceof Import && startsWith;
                        String checkName = partialInImportStatement ? initialModuleName : nameInImport;

                        findOccurrences(partialInImportStatement, checkName);
                    }
                    handled = true;
                } else {
                    if (nameInImport.equals(getModuleNameLastPart())) {
                        if (checkIndirectReferenceFromDefinition(nameInImport, addAsSearchString, renameAstEntry,
                                node.beginColumn,
                                node.beginLine)) {
                            findOccurrences(false, nameInImport);
                            aliasesHandled.add(i);
                            handled = true;
                        }
                    }
                }
            }
            if (aliasesHandled.size() > 0) {
                renameAstEntry.indexes = aliasesHandled;
                occurrences.add(renameAstEntry);
            }
        }
        return handled;
    }

    protected void findOccurrences(boolean partialInImportStatement, String checkName) {
        List<ASTEntry> localOccurrences = ScopeAnalysis.getLocalOccurrences(checkName,
                stack.peek());
        for (ASTEntry astEntry : localOccurrences) {
            if ((astEntry.node instanceof NameTok)
                    && (((NameTok) astEntry.node).ctx == NameTok.ImportName || ((NameTok) astEntry.node).ctx == NameTok.ImportModule)) {
                //i.e.: skip if it's an import as we already handle those!
                continue;
            } else {
                occurrences.add(new PyRenameImportProcess.FixedInputStringASTEntry(checkName,
                        null, astEntry.node, partialInImportStatement));
            }
        }
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
        boolean matched = handleNames(node, names, "", false);
        //Treat imports as relative on Python 2.x variants without the from __future__ import absolute_import statement.
        if (!matched && nature.getGrammarVersion() < IPythonNature.GRAMMAR_PYTHON_VERSION_3_0) {
            String relative = makeRelative(1, "");
            handleNames(node, names, relative, true);
        }
        return null;
    }

    public List<ASTEntry> getEntryOccurrences() {
        return new ArrayList<ASTEntry>(occurrences);
    }
}
