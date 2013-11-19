/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Copyright (c) 2013 by Syapse, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 18, 2005
 *
 * @author Fabio Zadrozny, Jeremy J. Carroll
 */
package org.python.pydev.editor.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.PyImportsHandling;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

/**
 * @author Fabio Zadrozny, Jeremy J. Carroll
 */
public class PyOrganizeImports extends PyAction implements IFormatter {

    private static abstract class ImportClassifier {
        static final int FUTURE = 0;
        static final int SYSTEM = 1;
        static final int THIRD_PARTY = 2;
        static final int OUR_CODE = 3;
        static final int RELATIVE = 4;

        abstract int classify(ImportHandle imp);
    }

    private static final class DummyImportClassifier extends ImportClassifier {

        @Override
        int classify(ImportHandle imp) {
            String module = getModuleName(imp);
            if (module.equals("__future__")) {
                return FUTURE;
            }
            if (module.startsWith(".")) {
                return RELATIVE;
            }
            return OUR_CODE;
        }
    }

    private static class PathImportClassifier extends ImportClassifier {

        private List<String> externalSourcePaths;

        private ISystemModulesManager manager;

        private IPythonNature nature;

        private IProjectModulesManager projectModulesManager;

        private Map<Object, Integer> mapToClassification = new HashMap<Object, Integer>();

        PathImportClassifier(IProject project) throws MisconfigurationException, PythonNatureWithoutProjectException {
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                try {
                    String externalProjectSourcePath = nature.getPythonPathNature().getProjectExternalSourcePath(true);
                    externalSourcePaths = StringUtils.splitAndRemoveEmptyTrimmed(externalProjectSourcePath, '|');
                    manager = nature.getProjectInterpreter().getModulesManager();
                    projectModulesManager = (IProjectModulesManager) nature.getAstManager().getModulesManager();
                    this.nature = nature;
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
        }

        @Override
        int classify(ImportHandle imp) {
            //Cache it as it may be asked multiple times for the same element during a sort.
            String module = getModuleName(imp);
            Integer currClassification = mapToClassification.get(module);
            if (currClassification != null) {
                return currClassification;
            }
            int classification = classifyInternal(module);
            mapToClassification.put(module, classification);
            return classification;
        }

        private int classifyInternal(String module) {
            if (module.equals("__future__")) {
                return FUTURE;
            }
            if (module.startsWith(".")) {
                return RELATIVE;
            }
            if (nature == null) {
                return OUR_CODE;
            }

            IModule mod;

            mod = manager.getModule(module, nature, false);
            if (mod == null) {

                mod = projectModulesManager.getModuleInDirectManager(module, nature, false);
                if (mod != null) {
                    File file = mod.getFile();
                    if (file != null) {
                        String fileAbsolutePath = FileUtils.getFileAbsolutePath(file);
                        int len = externalSourcePaths.size();
                        for (int i = 0; i < len; i++) {
                            String path = externalSourcePaths.get(i);
                            if (fileAbsolutePath.startsWith(path)) {
                                return THIRD_PARTY;
                            }
                        }
                    }
                }

                return OUR_CODE;
            }

            File file = mod.getFile();
            //Not sure I like this approach, but couldn't come up with anything better.
            if (file != null && file.getAbsolutePath().contains("site-packages")) {
                return THIRD_PARTY;
            }
            return SYSTEM;
        }

    }

    private static class Pep8ImportArranger extends ImportArranger {

        final ImportClassifier classifier;

        public Pep8ImportArranger(IDocument doc, boolean removeUnusedImports, String endLineDelim, IProject prj,
                String indentStr) {
            super(doc, removeUnusedImports, endLineDelim, indentStr);
            classifier = getClassifier(prj);
        }

        private ImportClassifier getClassifier(IProject p) {
            if (p != null) {
                try {
                    return new PathImportClassifier(p);
                } catch (MisconfigurationException e) {
                } catch (PythonNatureWithoutProjectException e) {
                }
            }
            return new DummyImportClassifier();
        }

        @Override
        void perform() {
            if (ImportsPreferencesPage.getGroupImports()) {
                perform(true);
            }
            perform(false);
        }

        @Override
        void sortImports(List<Tuple3<Integer, String, ImportHandle>> list) {
            Collections.sort(list, new Comparator<Tuple3<Integer, String, ImportHandle>>() {

                public int compare(Tuple3<Integer, String, ImportHandle> o1, Tuple3<Integer, String, ImportHandle> o2) {

                    int class1 = classifier.classify(o1.o3);
                    int class2 = classifier.classify(o2.o3);

                    if (class1 != class2) {
                        return class1 - class2;
                    }

                    int rslt = getModuleName(o1.o3).compareTo(getModuleName(o2.o3));
                    if (rslt != 0) {
                        return rslt;
                    }
                    return o1.o2.compareTo(o2.o2);
                }

            });
        }

        private int classification = -1;
        private boolean foundDocComment = false;

        @Override
        void writeImports(List<Tuple3<Integer, String, ImportHandle>> list, FastStringBuffer all) {
            super.writeImports(list, all);
            for (int i = endLineDelim.length(); i > 0; i--) {
                all.deleteFirst();
            }
        }

        @Override
        void beforeImport(Tuple3<Integer, String, ImportHandle> element, FastStringBuffer all) {
            int c = classifier.classify(element.o3);
            if (c != classification) {
                all.append(endLineDelim);
                classification = c;
            }
        }

        @Override
        void beforeImports(FastStringBuffer all) {
            if (foundDocComment) {
                all.append(this.endLineDelim);
            }
        }

        @Override
        void afterImports(FastStringBuffer all) {
            all.append(this.endLineDelim);
            all.append(this.endLineDelim);
        }

        @Override
        int insertImportsHere(int lineOfFirstOldImport) {
            return skipOverDocComment(lineOfFirstOldImport) - 1;
        }

        /**
         * 
         * This enum encapsulates the logic of the {@link ImportArranger#skipOverDocComment} method.
         * The order is significant, the matches method is called in order on
         * each value, until the value for the line in consideration is found.
         * @author jeremycarroll
         *
         */
        private enum SkipLineType {
            EndDocComment {
                @Override
                boolean matches(String line, SkipLineType startDocComment) {
                    return startDocComment.isEndDocComment(line.trim());
                }

                @Override
                boolean isEndDocComment(String nextLine) {
                    return true;
                }
            },
            MidDocComment {
                @Override
                boolean matches(String line, SkipLineType startDocComment) {
                    return !startDocComment.isDummy();
                }
            },
            SingleQuoteDocComment("'''"),
            DoubleQuoteDocComment("\"\"\""),
            BlankLine {
                @Override
                boolean matches(String line, SkipLineType startDocComment) {
                    return line.trim().isEmpty();
                }
            },
            Comment {
                @Override
                boolean matches(String line, SkipLineType startDocComment) {
                    return line.trim().startsWith("#");
                }
            },
            Code {
                @Override
                boolean matches(String line, SkipLineType startDocComment) {
                    // presupposes that others do not match!
                    return true;
                }
            },
            DummyHaventFoundStartDocComment {
                @Override
                boolean matches(String line, SkipLineType startDocComment) {
                    return false;
                }

                @Override
                boolean isDummy() {
                    return true;
                }
            },
            DummyHaveFoundEndDocComment {
                @Override
                boolean matches(String line, SkipLineType startDocComment) {
                    return false;
                }

                @Override
                boolean isDummy() {
                    return true;
                }

                @Override
                public boolean passedDocComment() {
                    return true;
                }
            };
            final String prefix;
            final boolean isStartDocComment;

            SkipLineType(String prefix, boolean isDocComment) {
                this.prefix = prefix;
                isStartDocComment = isDocComment;
            }

            SkipLineType() {
                this(null, false);
            }

            SkipLineType(String prefix) {
                this(prefix, true);
            }

            boolean matches(String line, SkipLineType startDocComment) {
                return line.startsWith(prefix);
            }

            boolean matchesStartAndEnd(String line) {
                if (prefix == null) {
                    return false;
                }
                line = line.trim();
                return line.length() >= 2 * prefix.length()
                        && line.startsWith(prefix)
                        && line.endsWith(prefix);
            }

            boolean isEndDocComment(String nextLine) {
                return isStartDocComment && nextLine.trim().endsWith(prefix);
            }

            boolean isDummy() {
                return false;
            }

            public boolean passedDocComment() {
                return false;
            }

        }

        private SkipLineType findLineType(String line, SkipLineType state) {
            for (SkipLineType slt : SkipLineType.values()) {
                if (slt.matches(line, state)) {
                    return slt;
                }
            }
            throw new IllegalStateException("No match");
        }

        private int skipOverDocComment(int firstOldImportLine) {
            try {
                SkipLineType parseState = SkipLineType.DummyHaventFoundStartDocComment;
                for (int l = firstOldImportLine; true; l++) {
                    IRegion lineInfo = doc.getLineInformation(l);
                    String line = doc.get(lineInfo.getOffset(), lineInfo.getLength());
                    SkipLineType slt = findLineType(line, parseState);
                    switch (slt) {
                        case MidDocComment:
                        case Comment:
                            break;
                        case Code:
                            if (!parseState.passedDocComment()) {
                                return firstOldImportLine;
                            } else {
                                foundDocComment = true;
                                return l;
                            }
                        case BlankLine:
                            // delete all blank lines in imports section of document
                            l--;
                            doc.replace(lineInfo.getOffset(),
                                    lineInfo.getLength() + endLineDelim.length(),
                                    "");
                            break;
                        case DoubleQuoteDocComment:
                        case SingleQuoteDocComment:
                            if (slt.matchesStartAndEnd(line)) {
                                parseState = SkipLineType.DummyHaveFoundEndDocComment;
                            } else {
                                parseState = slt;
                            }
                            break;
                        case EndDocComment:
                            parseState = SkipLineType.DummyHaveFoundEndDocComment;
                            break;
                        default:
                            throw new IllegalStateException(slt.name() + " not expected");

                    }
                }
            } catch (BadLocationException e) {
            }
            return firstOldImportLine;
        }

    }

    private static class ImportArranger {

        @SuppressWarnings("serial")
        private class FromImportEntries extends ArrayList<ImportHandleInfo> {
            ArrayList<Tuple<String, String>> importsAndComments = new ArrayList<Tuple<String, String>>();
            ArrayList<Tuple<String, String>> importsAndNoComments = new ArrayList<Tuple<String, String>>();
            FastStringBuffer lastFromXXXImportWritten = new FastStringBuffer();
            FastStringBuffer line = new FastStringBuffer();
            private String from;

            private void checkForCommentsAfterImport() {
                //first, reorganize them in the order to be written (the ones with comments after the ones without)

                for (ImportHandleInfo v : FromImportEntries.this) {
                    List<String> importedStr = v.getImportedStr();
                    List<String> commentsForImports = v.getCommentsForImports();
                    for (int i = 0; i < importedStr.size(); i++) {
                        String importedString = importedStr.get(i).trim();
                        String comment = commentsForImports.get(i).trim();
                        boolean isWildImport = importedString.equals("*");
                        if (isWildImport) {
                            importsAndComments.clear();
                            importsAndNoComments.clear();
                        }
                        if (comment.length() > 0) {
                            importsAndComments.add(new Tuple<String, String>(importedString, comment));
                        } else {
                            importsAndNoComments.add(new Tuple<String, String>(importedString, comment));
                        }
                        if (isWildImport) {
                            return;
                        }
                    }
                }
            }

            public void setFrom(String from) {
                this.from = from;
            }

            public void arrangeAndAdd(FastStringBuffer all) {

                // TODO: this could be clarified further but ...
                //ok, it's all filled, let's start rewriting it!
                boolean firstInLine = true;
                line.clear();
                boolean addedParenForLine = false;

                //ok, write all the ones with comments after the ones without any comments (each one with comment
                //will be written as a new import)
                importsAndNoComments.addAll(importsAndComments);
                for (int i = 0; i < importsAndNoComments.size(); i++) {

                    Tuple<String, String> tuple = importsAndNoComments.get(i);

                    if (firstInLine) {
                        lastFromXXXImportWritten.clear();
                        lastFromXXXImportWritten.append("from ");
                        lastFromXXXImportWritten.append(from);
                        lastFromXXXImportWritten.append(" import ");
                        line.append(lastFromXXXImportWritten);
                    } else {
                        line.append(", ");
                    }

                    if (multilineImports) {
                        if (line.length() + tuple.o1.length() + tuple.o2.length() > maxCols) {
                            //we have to make the wrapping
                            if (breakWithParenthesis) {
                                if (!addedParenForLine) {
                                    line.insert(lastFromXXXImportWritten.length(), '(');
                                    addedParenForLine = true;
                                }
                                line.append(endLineDelim);
                                line.append(indentStr);
                            } else {
                                line.append('\\');
                                line.append(endLineDelim);
                                line.append(indentStr);
                            }
                            all.append(line);
                            line.clear();
                        }
                    }

                    line.append(tuple.o1);

                    if (addedParenForLine && i == importsAndNoComments.size()) {
                        addedParenForLine = false;
                        line.append(')');
                    }

                    firstInLine = false;

                    if (tuple.o2.length() > 0) {
                        if (addedParenForLine) {
                            addedParenForLine = false;
                            line.append(')');
                        }
                        line.append(' ');
                        line.append(tuple.o2);
                        line.append(endLineDelim);
                        all.append(line);
                        line.clear();
                        firstInLine = true;
                    }
                }

                if (!firstInLine) {
                    if (addedParenForLine) {
                        addedParenForLine = false;
                        line.append(')');
                    }
                    line.append(endLineDelim);
                    all.append(line);
                    line.clear();
                }
            }

        }

        final IDocument doc;
        final String endLineDelim;
        private final String indentStr;
        private int lineForNewImports = -1;
        private final boolean multilineImports = ImportsPreferencesPage.getMultilineImports();
        private int maxCols = getMaxCols(multilineImports);
        private final boolean breakWithParenthesis = getBreakImportsWithParenthesis();
        private final boolean removeUnusedImports;

        public ImportArranger(IDocument doc, boolean removeUnusedImports, String endLineDelim, String indentStr) {
            this.doc = doc;
            this.endLineDelim = endLineDelim;
            this.indentStr = indentStr;
            this.removeUnusedImports = removeUnusedImports;
        }

        void perform() {
            perform(ImportsPreferencesPage.getGroupImports());
        }

        void perform(boolean groupFromImports) {
            List<Tuple3<Integer, String, ImportHandle>> list = collectImports();
            if (list.isEmpty()) {
                return;
            }
            int lineOfFirstOldImport = list.get(0).o1;

            deleteImports(list);

            lineForNewImports = insertImportsHere(lineOfFirstOldImport);

            if (this.removeUnusedImports) {
                pruneEmptyImports(list);
            }

            sortImports(list);

            //now, re-add the imports
            FastStringBuffer all = new FastStringBuffer();

            if (!groupFromImports) {
                writeImports(list, all);
            } else { //we have to group the imports!

                groupAndWriteImports(list, all);
            }

            PySelection.addLine(doc, endLineDelim, all.toString(), lineForNewImports);

        }

        private void pruneEmptyImports(List<Tuple3<Integer, String, ImportHandle>> list) {
            Iterator<Tuple3<Integer, String, ImportHandle>> it = list.iterator();
            while (it.hasNext()) {
                ImportHandle ih = it.next().o3;
                List<ImportHandleInfo> info = ih.getImportInfo();
                Iterator<ImportHandleInfo> itInfo = info.iterator();
                while (itInfo.hasNext()) {
                    if (itInfo.next().getImportedStr().isEmpty()) {
                        itInfo.remove();
                    }
                }
                if (info.size() == 0) {
                    it.remove();
                }
            }
        }

        void writeImports(List<Tuple3<Integer, String, ImportHandle>> list, FastStringBuffer all) {
            beforeImports(all);
            //no grouping
            for (Iterator<Tuple3<Integer, String, ImportHandle>> iter = list.iterator(); iter.hasNext();) {
                Tuple3<Integer, String, ImportHandle> element = iter.next();
                beforeImport(element, all);
                all.append(element.o2);
                all.append(endLineDelim);
            }
            afterImports(all);
        }

        void beforeImports(FastStringBuffer all) {
        }

        void afterImports(FastStringBuffer all) {
        }

        void beforeImport(Tuple3<Integer, String, ImportHandle> element, FastStringBuffer all) {
            // do nothing
        }

        int insertImportsHere(int lineOfFirstOldImport) {
            return lineOfFirstOldImport - 1;
        }

        private void groupAndWriteImports(List<Tuple3<Integer, String, ImportHandle>> list, FastStringBuffer all) {
            //import from to the imports that should be grouped given its 'from'
            TreeMap<String, FromImportEntries> importsWithFrom = new TreeMap<String, FromImportEntries>(
                    new Comparator<String>() {

                        public int compare(String o1, String o2) {
                            Tuple<String, String> splitted1 = StringUtils.splitOnFirst(o1, '.');
                            Tuple<String, String> splitted2 = StringUtils.splitOnFirst(o2, '.');

                            boolean isFuture1 = splitted1.o1.equals("__future__");
                            boolean isFuture2 = splitted2.o1.equals("__future__");

                            if (isFuture1 != isFuture2) {
                                if (isFuture1) {
                                    return -1;
                                }
                                return 1;
                            }

                            return o1.compareTo(o2);
                        }
                    });
            List<ImportHandleInfo> importsWithoutFrom = new ArrayList<ImportHandleInfo>();

            fillImportStructures(list, importsWithFrom, importsWithoutFrom);

            Set<Entry<String, FromImportEntries>> entrySet = importsWithFrom.entrySet();

            for (Entry<String, FromImportEntries> entry : entrySet) {

                FromImportEntries value = entry.getValue();

                value.setFrom(entry.getKey());
                value.checkForCommentsAfterImport();
                value.arrangeAndAdd(all);
            }

            writeImportsWithoutFrom(all, importsWithoutFrom);
        }

        /**
         * Fills the import structure passed, so that the imports from will be grouped by the 'from' part and the regular
         * imports will be in a separate list.
         */
        private void fillImportStructures(List<Tuple3<Integer, String, ImportHandle>> list,
                TreeMap<String, FromImportEntries> importsWithFrom, List<ImportHandleInfo> importsWithoutFrom) {
            //fill the info
            for (Tuple3<Integer, String, ImportHandle> element : list) {

                List<ImportHandleInfo> importInfo = element.o3.getImportInfo();
                for (ImportHandleInfo importHandleInfo : importInfo) {
                    String fromImportStr = importHandleInfo.getFromImportStr();
                    if (fromImportStr == null) {
                        importsWithoutFrom.add(importHandleInfo);
                    } else {
                        FromImportEntries lst = importsWithFrom.get(fromImportStr);
                        if (lst == null) {
                            lst = new FromImportEntries();
                            importsWithFrom.put(fromImportStr, lst);
                        }
                        lst.add(importHandleInfo);
                    }
                }
            }
        }

        void sortImports(List<Tuple3<Integer, String, ImportHandle>> list) {
            Collections.sort(list, new Comparator<Tuple3<Integer, String, ImportHandle>>() {

                public int compare(Tuple3<Integer, String, ImportHandle> o1, Tuple3<Integer, String, ImportHandle> o2) {
                    //When it's __future__, it has to appear before the others.
                    List<ImportHandleInfo> info1 = o1.o3.getImportInfo();
                    List<ImportHandleInfo> info2 = o2.o3.getImportInfo();
                    boolean isFuture1 = getIsFuture(info1);
                    boolean isFuture2 = getIsFuture(info2);
                    if (isFuture1 && !isFuture2) {
                        return -1;
                    }
                    if (!isFuture1 && isFuture2) {
                        return 1;
                    }
                    return o1.o2.compareTo(o2.o2);
                }

                private boolean getIsFuture(List<ImportHandleInfo> info1) {
                    String from1 = null;
                    if (info1.size() > 0) {
                        from1 = info1.get(0).getFromImportStr();
                    }
                    boolean isFuture = from1 != null && from1.equals("__future__");
                    return isFuture;
                }
            });
        }

        private void deleteImports(List<Tuple3<Integer, String, ImportHandle>> list) {
            //sort in inverse order (for removal of the string of the document).
            Collections.sort(list, new Comparator<Tuple3<Integer, String, ImportHandle>>() {

                public int compare(Tuple3<Integer, String, ImportHandle> o1, Tuple3<Integer, String, ImportHandle> o2) {
                    return o2.o1.compareTo(o1.o1);
                }
            });
            //ok, now we have to delete all lines with imports.
            for (Iterator<Tuple3<Integer, String, ImportHandle>> iter = list.iterator(); iter.hasNext();) {
                Tuple3<Integer, String, ImportHandle> element = iter.next();
                String s = element.o2;
                int i = StringUtils.countLineBreaks(s);
                while (i >= 0) {
                    PySelection.deleteLine(doc, (element.o1).intValue());
                    i--;
                }
            }
        }

        final List<Tuple3<Integer, String, ImportHandle>> collectImports() {

            List<Tuple3<Integer, String, ImportHandle>> list = new ArrayList<Tuple3<Integer, String, ImportHandle>>();
            //Gather imports in a structure we can work on.
            PyImportsHandling pyImportsHandling = new PyImportsHandling(doc, true, this.removeUnusedImports);
            for (ImportHandle imp : pyImportsHandling) {

                list.add(new Tuple3<Integer, String, ImportHandle>(imp.startFoundLine, imp.importFound, imp));
            }
            return list;
        }

        /**
         * Write the imports that don't have a 'from' in the beggining (regular imports)
         */
        private void writeImportsWithoutFrom(FastStringBuffer all,
                List<ImportHandleInfo> importsWithoutFrom) {
            //now, write the regular imports (no wrapping or tabbing here)
            for (ImportHandleInfo info : importsWithoutFrom) {

                List<String> importedStr = info.getImportedStr();
                List<String> commentsForImports = info.getCommentsForImports();
                for (int i = 0; i < importedStr.size(); i++) {
                    all.append("import ");
                    String importedString = importedStr.get(i);
                    String comment = commentsForImports.get(i);
                    all.append(importedString);
                    if (comment.length() > 0) {
                        all.append(' ');
                        all.append(comment);
                    }
                    all.append(endLineDelim);
                }
            }
        }
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            PyEdit pyEdit = getPyEdit();

            PySelection ps = new PySelection(pyEdit);
            final IDocument doc = ps.getDoc();
            if (ps.getStartLineIndex() == ps.getEndLineIndex()) {
                organizeImports(pyEdit, doc, null, ps);
            } else {
                String endLineDelim = ps.getEndLineDelim();
                DocumentRewriteSession session = startWrite(doc);
                performSimpleSort(doc, endLineDelim, ps.getStartLineIndex(), ps.getEndLineIndex());
                endWrite(doc, session);
            }
        } catch (Exception e) {
            Log.log(e);
            beep(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void organizeImports(PyEdit edit, final IDocument doc, IFile f, PySelection ps) {
        DocumentRewriteSession session = null;
        String endLineDelim = ps.getEndLineDelim();
        List<IOrganizeImports> participants = null;
        if (f == null) {
            // organizing single file ...
            //let's see if someone wants to make a better implementation in another plugin...
            participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_ORGANIZE_IMPORTS);

            for (IOrganizeImports organizeImports : participants) {
                if (!organizeImports.beforePerformArrangeImports(ps, edit, f)) {
                    return;
                }
            }
        }

        String indentStr = edit != null ?
                edit.getIndentPrefs().getIndentationString() :
                DefaultIndentPrefs.get().getIndentationString();
        session = startWrite(doc);
        try {

            boolean removeUnusedImports = ImportsPreferencesPage.getDeleteUnusedImports();
            boolean pep8 = ImportsPreferencesPage.getPep8Imports();

            if (removeUnusedImports) {
                new OrganizeImportsFixesUnused().beforePerformArrangeImports(ps, edit, f);
            }

            if (pep8) {
                if (f == null) {
                    f = edit.getIFile();
                }
                IProject p = f != null ? f.getProject() : null;
                pep8PerformArrangeImports(doc, removeUnusedImports, endLineDelim, p, indentStr);
            } else {
                performArrangeImports(doc, removeUnusedImports, endLineDelim, indentStr);
            }

            if (participants != null) {
                for (IOrganizeImports organizeImports : participants) {
                    organizeImports.afterPerformArrangeImports(ps, edit);
                }
            }
        } finally {
            if (session != null) {
                endWrite(doc, session);
            }
        }
    }

    private static String getModuleName(ImportHandle imp) {
        String module = imp.getImportInfo().get(0).getFromImportStr();
        if (module == null) {
            module = imp.getImportInfo().get(0).getImportedStr().get(0);
        }
        return module;
    }

    /**
     * Stop a rewrite session
     */
    private void endWrite(IDocument doc, DocumentRewriteSession session) {
        if (doc instanceof IDocumentExtension4) {
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            d.stopRewriteSession(session);
        }
    }

    /**
     * Starts a rewrite session (keep things in a single undo/redo)
     */
    private DocumentRewriteSession startWrite(IDocument doc) {
        if (doc instanceof IDocumentExtension4) {
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            return d.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
        }
        return null;
    }

    /**
     * Actually does the action in the document. Public for testing.
     * 
     * @param doc
     * @param removeUnusedImports 
     * @param endLineDelim
     */
    public static void performArrangeImports(IDocument doc, boolean removeUnusedImports, String endLineDelim,
            String indentStr) {
        new ImportArranger(doc, removeUnusedImports, endLineDelim, indentStr).perform();
    }

    /**
     * Pep8 compliant version. Actually does the action in the document.
     * 
     * @param doc
     * @param removeUnusedImports 
     * @param endLineDelim
     */
    public static void pep8PerformArrangeImports(IDocument doc, boolean removeUnusedImports, String endLineDelim,
            IProject prj, String indentStr) {
        new Pep8ImportArranger(doc, removeUnusedImports, endLineDelim, prj, indentStr).perform();
    }

    /**
     * @return true if the imports should be split with parenthesis (instead of escaping)
     */
    private static boolean getBreakImportsWithParenthesis() {
        String breakIportMode = ImportsPreferencesPage.getBreakIportMode();
        boolean breakWithParenthesis = true;
        if (!breakIportMode.equals(ImportsPreferencesPage.BREAK_IMPORTS_MODE_PARENTHESIS)) {
            breakWithParenthesis = false;
        }
        return breakWithParenthesis;
    }

    /**
     * @return the maximum number of columns that may be available in a line.
     */
    private static int getMaxCols(boolean multilineImports) {
        final int maxCols;
        if (multilineImports) {
            if (SharedCorePlugin.inTestMode()) {
                maxCols = 80;
            } else {
                IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();
                maxCols = chainedPrefStore
                        .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
            }
        } else {
            maxCols = Integer.MAX_VALUE;
        }
        return maxCols;
    }

    /**
     * Performs a simple sort without taking into account the actual contents of the selection (aside from lines
     * ending with '\' which are considered as a single line).
     * 
     * @param doc the document to be sorted
     * @param endLineDelim the delimiter to be used
     * @param startLine the first line where the sort should happen
     * @param endLine the last line where the sort should happen
     */
    public static void performSimpleSort(IDocument doc, String endLineDelim, int startLine, int endLine) {
        try {
            ArrayList<String> list = new ArrayList<String>();

            StringBuffer lastLine = null;
            for (int i = startLine; i <= endLine; i++) {

                String line = PySelection.getLine(doc, i);

                if (lastLine != null) {
                    int len = lastLine.length();
                    if (len > 0 && lastLine.charAt(len - 1) == '\\') {
                        lastLine.append(endLineDelim);
                        lastLine.append(line);
                    } else {
                        list.add(lastLine.toString());
                        lastLine = new StringBuffer(line);
                    }
                } else {
                    lastLine = new StringBuffer(line);
                }
            }

            if (lastLine != null) {
                list.add(lastLine.toString());
            }

            Collections.sort(list);
            StringBuffer all = new StringBuffer();
            for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
                String element = iter.next();
                all.append(element);
                if (iter.hasNext()) {
                    all.append(endLineDelim);
                }
            }

            int length = doc.getLineInformation(endLine).getLength();
            int endOffset = doc.getLineInformation(endLine).getOffset() + length;
            int startOffset = doc.getLineInformation(startLine).getOffset();

            doc.replace(startOffset, endOffset - startOffset, all.toString());

        } catch (BadLocationException e) {
            Log.log(e);
        }

    }

    /**
     * Used by legacy tests.
     * @param doc
     * @param endLineDelim
     * @param indentStr
     */
    public static void performArrangeImports(Document doc, String endLineDelim, String indentStr) {
        performArrangeImports(doc, false, endLineDelim, indentStr);
    }

    public void formatAll(IDocument doc, IPyEdit edit, IFile f, boolean isOpenedFile, boolean throwSyntaxError)
            throws SyntaxErrorException {
        organizeImports((PyEdit) edit, doc, f, new PySelection(doc));
    }

    public void formatSelection(IDocument doc, int[] regionsToFormat, IPyEdit edit, PySelection ps) {
        throw new UnsupportedOperationException();
    }
}
