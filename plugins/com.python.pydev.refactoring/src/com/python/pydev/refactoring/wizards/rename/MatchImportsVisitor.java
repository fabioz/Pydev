package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.StringUtils;

public class MatchImportsVisitor extends VisitorBase {

    private IPythonNature nature;
    private String initialModuleName;
    private SourceModule currentModule;

    public final List<ImportFrom> importFromsMatchingOnModulePart = new ArrayList<>();
    public final List<ImportFrom> importFromsMatchingOnAliasPart = new ArrayList<>();
    public final List<Import> importsMatchingOnAliasPart = new ArrayList<>();
    public final List<ASTEntry> occurrences = new ArrayList<>();

    public MatchImportsVisitor(IPythonNature nature, String initialName, SourceModule module) {
        this.nature = nature;
        this.initialModuleName = initialName;
        this.currentModule = module;
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
    public Object visitImportFrom(ImportFrom node) throws Exception {
        int level = node.level;
        String modRep = NodeUtils.getRepresentationString(node.module);

        //TODO: We don't treat imports as relative on Python 2.x variants without the from __future__ import absolute_import statement.

        if (level > 0) {
            //Ok, direct match didn't work, so, let's check relative imports
            String parentPackage = this.currentModule.getName();
            List<String> moduleParts = StringUtils.split(parentPackage, '.');

            if (moduleParts.size() > level) {
                String relative = FullRepIterable.joinParts(moduleParts, moduleParts.size() - level);
                modRep = StringUtils.join(".", relative, modRep);
            }
        }

        if (modRep.equals(this.initialModuleName) || (modRep + ".").startsWith(initialModuleName)) {
            //Ok, if the first part matched, no need to check other things (i.e.: rename only the from "xxx.yyy" part)
            importFromsMatchingOnModulePart.add(node);
            occurrences.add(new ImportRenameAstEntry(null, node) {
                @Override
                public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                        RefactoringStatus status) {
                    //Simple one: just the first part has to be changed.
                    ImportFrom f = (ImportFrom) this.node;
                    int offset = PySelection
                            .getAbsoluteCursorOffset(doc, f.module.beginLine - 1, f.module.beginColumn - 1);

                    TextEdit replaceEdit = new ReplaceEdit(offset, initialName.length(), inputName);
                    return Arrays.asList(replaceEdit);
                }
            });
            return null;
        }

        handleNames(node, node.names, modRep);

        return null;
    }

    public void handleNames(SimpleNode node, aliasType[] names, String modRep) {
        if (names != null && names.length > 0) {
            //not wild import!
            for (int i = 0; i < names.length; i++) {
                aliasType aliasType = names[i];
                NameTokType name = aliasType.name;
                String full;
                if (modRep != null && modRep.length() > 0) {
                    full = StringUtils.join(".", modRep, ((NameTok) name).id);
                } else {
                    full = ((NameTok) name).id;
                }
                if (full.equals(this.initialModuleName) || (full + ".").startsWith(initialModuleName)) {
                    //Ok, this match is a bit more tricky: we matched it, but we need to rename a part before and after the from xxx.yyy import zzz part
                    //also, we must take care not to destroy any alias in the process or other imports which may be joined with this one (the easiest part
                    //is probably removing the whole import and re-writing everything again).
                    if (node instanceof ImportFrom) {
                        importFromsMatchingOnAliasPart.add((ImportFrom) node);
                        occurrences.add(new ImportRenameAstEntry(null, node) {

                            @Override
                            public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                                    RefactoringStatus status) {
                                throw new RuntimeException("not implemented");
                            }

                        });
                    } else if (node instanceof Import) {
                        importsMatchingOnAliasPart.add((Import) node);
                        occurrences.add(new ImportRenameAstEntry(null, node) {

                            @Override
                            public List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName,
                                    RefactoringStatus status) {
                                throw new RuntimeException("not implemented");
                            }

                        });
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        aliasType[] names = node.names;
        handleNames(node, names, "");
        return null;
    }

    public List<ASTEntry> getEntryOccurrences() {
        return occurrences;
    }
}
