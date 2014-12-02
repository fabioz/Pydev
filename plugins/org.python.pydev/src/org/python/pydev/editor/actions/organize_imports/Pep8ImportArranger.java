/**
 * Copyright (c) 2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions.organize_imports;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

public class Pep8ImportArranger extends ImportArranger {

    static final class DummyImportClassifier extends ImportClassifier {

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

    static abstract class ImportClassifier {
        static final int FUTURE = 0;
        static final int SYSTEM = 1;
        static final int THIRD_PARTY = 2;
        static final int OUR_CODE = 3;
        static final int RELATIVE = 4;

        abstract int classify(ImportHandle imp);
    }

    private static abstract class ImportType {
        static final int IMPORT = 1;
        static final int FROM = 2;
    }

    /**
     * Return the imported module associated with a given
     * 'import ...' or 'from ... import ...' statement.
     */
    static String getModuleName(ImportHandle imp) {
        String module = imp.getImportInfo().get(0).getFromImportStr();
        if (module == null) {
            module = imp.getImportInfo().get(0).getImportedStr().get(0);
        }
        return module;
    }

    /**
     * Return true if the given import uses the 'from ... import ...'
     * syntax, and false if it uses 'import ...'
     */
    static int getImportType(ImportHandle imp) {
        String module = imp.getImportInfo().get(0).getFromImportStr();
        if (module != null) {
            return ImportType.FROM;
        }
        return ImportType.IMPORT;
    }

    static class PathImportClassifier extends ImportClassifier {

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

    final ImportClassifier classifier;

    public Pep8ImportArranger(IDocument doc, boolean removeUnusedImports, String endLineDelim, IProject prj,
            String indentStr, boolean automatic, IPyFormatStdProvider edit) {
        super(doc, removeUnusedImports, endLineDelim, indentStr, automatic, edit);
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
    public void perform() {
        //        if (ImportsPreferencesPage.getGroupImports()) {
        //            perform(true); -- TODO: This mode is flawed (must be reviewed).
        //        } else {
        perform(false, edit);
        //        }
    }

    @Override
    protected void sortImports(List<Tuple3<Integer, String, ImportHandle>> list) {
        Collections.sort(list, new Comparator<Tuple3<Integer, String, ImportHandle>>() {

            public int compare(Tuple3<Integer, String, ImportHandle> o1, Tuple3<Integer, String, ImportHandle> o2) {

                int class1 = classifier.classify(o1.o3);
                int class2 = classifier.classify(o2.o3);

                if (class1 != class2) {
                    return class1 - class2;
                }

                if (ImportsPreferencesPage.getSortFromImportsFirst(edit))
                {
                    int type1 = getImportType(o1.o3);
                    int type2 = getImportType(o2.o3);
                    if (type1 != type2)
                    {
                        return type2 - type1;
                    }
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
    protected void writeImports(List<Tuple3<Integer, String, ImportHandle>> list, FastStringBuffer all) {
        super.writeImports(list, all);
        if (all.startsWith(endLineDelim)) {
            for (int i = endLineDelim.length(); i > 0; i--) {
                all.deleteFirst();
            }
        }
    }

    @Override
    protected void beforeImport(Tuple3<Integer, String, ImportHandle> element, FastStringBuffer all) {
        int c = classifier.classify(element.o3);
        if (c != classification) {
            all.append(endLineDelim);
            classification = c;
        }
    }

    @Override
    protected void beforeImports(FastStringBuffer all) {
        if (foundDocComment) {
            all.append(this.endLineDelim);
        }
    }

    @Override
    protected void afterImports(FastStringBuffer all) {
        all.append(this.endLineDelim);
        all.append(this.endLineDelim);
    }

    @Override
    protected int insertImportsHere(int lineOfFirstOldImport) {
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
            boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
                return startDocComment.isEndDocComment(line.trim());
            }

            @Override
            boolean isEndDocComment(String nextLine) {
                return true;
            }
        },
        MidDocComment {
            @Override
            boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
                return !startDocComment.isDummy();
            }
        },
        SingleQuoteDocComment("'''"),
        DoubleQuoteDocComment("\"\"\""),
        BlankLine {
            @Override
            boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
                return line.trim().isEmpty();
            }
        },
        Comment {
            @Override
            boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
                return line.trim().startsWith("#");
            }
        },
        Code {
            @Override
            boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
                // presupposes that others do not match!
                return true;
            }
        },
        DummyHaventFoundStartDocComment {
            @Override
            boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
                return false;
            }

            @Override
            boolean isDummy() {
                return true;
            }
        },
        DummyHaveFoundEndDocComment {
            @Override
            boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
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

        boolean matches(String line, Pep8ImportArranger.SkipLineType startDocComment) {
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

    private Pep8ImportArranger.SkipLineType findLineType(String line, Pep8ImportArranger.SkipLineType state) {
        for (Pep8ImportArranger.SkipLineType slt : SkipLineType.values()) {
            if (slt.matches(line, state)) {
                return slt;
            }
        }
        throw new IllegalStateException("No match");
    }

    private int skipOverDocComment(int firstOldImportLine) {
        try {
            Pep8ImportArranger.SkipLineType parseState = SkipLineType.DummyHaventFoundStartDocComment;
            for (int l = firstOldImportLine; true; l++) {
                IRegion lineInfo = doc.getLineInformation(l);
                String line = doc.get(lineInfo.getOffset(), lineInfo.getLength());
                Pep8ImportArranger.SkipLineType slt = findLineType(line, parseState);
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
                        addNewLinesToImports = true;
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