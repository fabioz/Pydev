/**
 * Copyright (c) 2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions.organize_imports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.PyImportsHandling;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

public class ImportArranger {

    public boolean addNewLinesToImports = false;

    private final class FromImportEntries {
        private final List<ImportHandleInfo> containedImports = new ArrayList<>();

        private final List<Tuple<String, String>> importsAndComments = new ArrayList<Tuple<String, String>>();
        private final List<Tuple<String, String>> importsAndNoComments = new ArrayList<Tuple<String, String>>();

        private final FastStringBuffer lastFromXXXImportWritten = new FastStringBuffer();
        private final FastStringBuffer line = new FastStringBuffer();
        private String from;

        public void add(ImportHandleInfo info) {
            containedImports.add(info);
        }

        private void checkForCommentsAfterImport() {
            //first, reorganize them in the order to be written (the ones with comments after the ones without)

            for (ImportHandleInfo v : FromImportEntries.this.containedImports) {
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

            if (sortNamesGrouped) {
                Comparator<? super Tuple<String, String>> c = new Comparator<Tuple<String, String>>() {

                    @Override
                    public int compare(Tuple<String, String> o1, Tuple<String, String> o2) {
                        return o1.o1.compareTo(o2.o1);
                    }
                };
                Collections.sort(importsAndNoComments, c);
            }

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
                        String addAfter = indentStr;
                        //we have to make the wrapping
                        if (breakWithParenthesis) {
                            if (!addedParenForLine) {
                                line.insert(lastFromXXXImportWritten.length(), '(');
                                if (StringUtils.rightTrim(line.toString()).length() > maxCols) {
                                    addAfter = indentStr
                                            + line.subSequence(lastFromXXXImportWritten.length() + 1, line.length())
                                                    .toString();
                                    line.setLength(lastFromXXXImportWritten.length() + 1);
                                }
                                addedParenForLine = true;
                            }
                            line.append(endLineDelim);
                        } else {
                            line.append('\\');
                            line.append(endLineDelim);
                        }
                        all.append(line);
                        line.clear();
                        line.append(addAfter);
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
     * @return true if the imports should be split with parenthesis (instead of escaping)
     */
    private static boolean getBreakImportsWithParenthesis(IPyFormatStdProvider edit) {
        String breakIportMode = ImportsPreferencesPage.getBreakIportMode(edit);
        boolean breakWithParenthesis = true;
        if (!breakIportMode.equals(ImportsPreferencesPage.BREAK_IMPORTS_MODE_PARENTHESIS)) {
            breakWithParenthesis = false;
        }
        return breakWithParenthesis;
    }

    protected final IDocument doc;
    protected final String endLineDelim;
    private final String indentStr;
    private int lineForNewImports = -1;
    private final boolean multilineImports;
    private final boolean sortNamesGrouped;
    private int maxCols;
    private final boolean breakWithParenthesis;
    private final boolean removeUnusedImports;
    private final boolean automatic;
    protected final IPyFormatStdProvider edit;

    public ImportArranger(IDocument doc, boolean removeUnusedImports, String endLineDelim, String indentStr,
            boolean automatic, IPyFormatStdProvider edit) {
        this.doc = doc;
        this.endLineDelim = endLineDelim;
        this.indentStr = indentStr;
        this.removeUnusedImports = removeUnusedImports;
        this.automatic = automatic;
        this.edit = edit;
        multilineImports = ImportsPreferencesPage.getMultilineImports(edit);
        sortNamesGrouped = ImportsPreferencesPage.getSortNamesGrouped(edit);
        breakWithParenthesis = getBreakImportsWithParenthesis(edit);
        maxCols = getMaxCols(multilineImports);
    }

    public void perform() {
        perform(ImportsPreferencesPage.getGroupImports(edit), edit);
    }

    protected void perform(boolean groupFromImports, IPyFormatStdProvider edit) {
        boolean executeOnlyIfChanged = automatic;
        perform(groupFromImports, executeOnlyIfChanged, edit);
    }

    /**
     * @param executeOnlyIfChanged: if 'true' initially, we'll check if something changes first. If something changes
     * it'll call itself again with 'false' to force the changes.
     */
    private void perform(boolean groupFromImports, boolean executeOnlyIfChanged, IPyFormatStdProvider edit) {
        List<Tuple3<Integer, String, ImportHandle>> list = collectImports();
        if (list.isEmpty()) {
            return;
        }
        int lineOfFirstOldImport = list.get(0).o1;

        List<Tuple<Integer, String>> linesToDelete = deleteImports(list);
        if (!executeOnlyIfChanged) {
            for (Tuple<Integer, String> tup : linesToDelete) {
                PySelection.deleteLine(doc, tup.o1);
            }
        }

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

        String finalStr = all.toString();

        if (executeOnlyIfChanged) {
            //If going automatic, let's check the contents before actually doing the organize 
            //(and skip if the order is ok).
            ArrayList<String> list2 = new ArrayList<String>();
            for (Tuple<Integer, String> tup : linesToDelete) {
                list2.add(tup.o2);
            }
            Collections.reverse(list2);
            String join = StringUtils.join("", list2).trim();
            String other = StringUtils.replaceNewLines(finalStr, "").trim();
            if (join.equals(other)) {
                //                System.out.println("Equals");
            } else {
                //                System.out.println("Not equal!");
                //                System.out.println("\n\n---");
                //                System.out.println(join);
                //                System.out.println("---");
                //                System.out.println(other);
                //                System.out.println("---\n");
                perform(groupFromImports, false, edit);
            }
            return;
        }

        try {
            PyFormatStd std = new PyFormatStd();
            boolean throwSyntaxError = false;
            ISelectionProvider selectionProvider = null;
            int[] regionsToFormat = null;
            IDocument psDoc = new Document(finalStr);
            PySelection ps = new PySelection(psDoc);
            std.applyFormatAction(edit, ps, regionsToFormat, throwSyntaxError, selectionProvider);
            finalStr = psDoc.get();
            if (addNewLinesToImports) {
                // Leave 2 empty new lines separating imports from code 
                String expectedEnd = endLineDelim + endLineDelim + endLineDelim;
                while (!finalStr.endsWith(expectedEnd)) {
                    finalStr += endLineDelim;
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }

        PySelection.addLine(doc, endLineDelim, finalStr, lineForNewImports);

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

    protected void writeImports(List<Tuple3<Integer, String, ImportHandle>> list, FastStringBuffer all) {
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

    protected void beforeImports(FastStringBuffer all) {
    }

    protected void afterImports(FastStringBuffer all) {
    }

    protected void beforeImport(Tuple3<Integer, String, ImportHandle> element, FastStringBuffer all) {
        // do nothing
    }

    protected int insertImportsHere(int lineOfFirstOldImport) {
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

    protected void sortImports(List<Tuple3<Integer, String, ImportHandle>> list) {
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

    private List<Tuple<Integer, String>> deleteImports(List<Tuple3<Integer, String, ImportHandle>> list) {
        //sort in inverse order (for removal of the string of the document).
        List<Tuple<Integer, String>> linesToDelete = new ArrayList<>();
        Collections.sort(list, new Comparator<Tuple3<Integer, String, ImportHandle>>() {

            public int compare(Tuple3<Integer, String, ImportHandle> o1, Tuple3<Integer, String, ImportHandle> o2) {
                return o2.o1.compareTo(o1.o1);
            }
        });
        //ok, now we have to delete all lines with imports.
        for (Iterator<Tuple3<Integer, String, ImportHandle>> iter = list.iterator(); iter.hasNext();) {
            Tuple3<Integer, String, ImportHandle> element = iter.next();
            String s = element.o2;
            int max = StringUtils.countLineBreaks(s);
            for (int i = 0; i <= max; i++) {
                int lineToDel = (element.o1).intValue();

                int j = lineToDel + i;
                linesToDelete.add(new Tuple(j, PySelection.getLine(doc, j)));
            }
        }
        Comparator<? super Tuple<Integer, String>> c = new Comparator<Tuple<Integer, String>>() {

            @Override
            public int compare(Tuple<Integer, String> o1, Tuple<Integer, String> o2) {
                return Integer.compare(o2.o1, o1.o1); //reversed compare (o2 first o1 last).
            }
        };
        Collections.sort(linesToDelete, c);
        return linesToDelete;
    }

    final List<Tuple3<Integer, String, ImportHandle>> collectImports() {

        List<Tuple3<Integer, String, ImportHandle>> list = new ArrayList<Tuple3<Integer, String, ImportHandle>>();
        //Gather imports in a structure we can work on.
        PyImportsHandling pyImportsHandling = new PyImportsHandling(doc, true, this.removeUnusedImports);
        for (ImportHandle imp : pyImportsHandling) {
            if (imp.importFound.contains("@NoMove")) {
                continue;
            }
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