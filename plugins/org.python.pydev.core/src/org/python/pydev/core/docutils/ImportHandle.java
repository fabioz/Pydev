/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Class that represents an import found in a document.
 *
 * @author Fabio
 */
public class ImportHandle {

    /**
     * Class representing some import information
     *
     * @author Fabio
     */
    public static class ImportHandleInfo {

        //spaces* 'from' space+ module space+ import (mod as y)
        private static final Pattern FromImportPattern = Pattern
                .compile("(from\\s+)(\\.|\\w)+((\\\\|\\s)+import(\\\\|\\s)+)");
        private static final Pattern BadFromPattern = Pattern
                .compile("from\\s+(\\.|\\w)+(\\\\|\\s)+import");
        private static final Pattern ImportPattern = Pattern.compile("(import\\s+)");

        /**
         * Holds the 'KKK' if the import is from KKK import YYY
         * If it's not a From Import, it should be null.
         */
        private String fromStr;

        /**
         * This is the alias that's been imported. E.g.: in from KKK import YYY, ZZZ, this is a list
         * with YYY and ZZZ
         */
        private List<String> importedStr;

        /**
         * Comments (one for each imported string) E.g.: in from KKK import (YYY, #comment\n ZZZ), this is a list
         * with #comment and an empty string.
         */
        private List<String> importedStrComments;

        /**
         * Starting line for this import.
         */
        private int startLine;

        /**
         * Ending line for this import.
         */
        private int endLine;

        /**
         * Holds whether the import started in the middle of the line (after a ';')
         */
        private boolean startedInMiddleOfLine;

        /**
         * Constructor that does not set the line for the import.
         */
        public ImportHandleInfo(String importFound) throws ImportNotRecognizedException {
            this(importFound, -1, -1, false, false);
        }

        /**
         * Constructor.
         * 
         * Creates the information to be returned later
         * 
         * @param importFound
         * @throws ImportNotRecognizedException 
         */
        public ImportHandleInfo(String importFound, int lineStart, int lineEnd, boolean startedInMiddleOfLine,
                boolean allowBadInput)
                throws ImportNotRecognizedException {
            this.startLine = lineStart;
            this.endLine = lineEnd;
            this.startedInMiddleOfLine = startedInMiddleOfLine;

            importFound = importFound.trim();
            if (importFound.length() == 0) {
                throw new ImportNotRecognizedException("Could not recognize empty string as import");
            }
            char firstChar = importFound.charAt(0);

            if (firstChar == 'f') {
                //from import
                Matcher matcher = FromImportPattern.matcher(importFound);
                if (matcher.find()) {
                    this.fromStr = importFound.substring(matcher.end(1), matcher.end(2)).trim();

                    //we have to do that because the last group will only have the last match in the string
                    String importedStr = importFound.substring(matcher.end(3), importFound.length()).trim();

                    buildImportedList(importedStr);

                } else {
                    if (allowBadInput &&
                            ("from".equals(importFound)
                            || BadFromPattern.matcher(importFound).matches())) {
                        dummyImportList();
                        return;
                    }
                    throw new ImportNotRecognizedException("Could not recognize import: " + importFound);
                }

            } else if (firstChar == 'i') {
                //regular import
                Matcher matcher = ImportPattern.matcher(importFound);
                if (matcher.find()) {
                    //we have to do that because the last group will only have the last match in the string
                    String importedStr = importFound.substring(matcher.end(1), importFound.length()).trim();

                    buildImportedList(importedStr);

                } else {
                    if (allowBadInput && "import".equals(importFound)) {
                        dummyImportList();
                        return;
                    }
                    throw new ImportNotRecognizedException("Could not recognize import: " + importFound);
                }

            } else {
                throw new ImportNotRecognizedException("Could not recognize import: " + importFound);
            }
        }

        private void dummyImportList() {
            this.importedStr = this.importedStrComments = Arrays.asList(new String[0]);
        }

        /**
         * Fills the importedStrComments and importedStr given the importedStr passed 
         * 
         * @param importedStr string with the tokens imported in an import
         */
        private void buildImportedList(String importedStr) {
            ArrayList<String> lst = new ArrayList<String>();
            ArrayList<String> importComments = new ArrayList<String>();

            FastStringBuffer alias = new FastStringBuffer();
            FastStringBuffer comments = new FastStringBuffer();
            for (int i = 0; i < importedStr.length(); i++) {
                char c = importedStr.charAt(i);
                if (c == '#') {
                    comments = comments.clear();
                    i = ParsingUtils.create(importedStr).eatComments(comments, i);
                    addImportAlias(lst, importComments, alias, comments.toString());
                    alias = alias.clear();

                } else if (c == ',' || c == '\r' || c == '\n') {
                    addImportAlias(lst, importComments, alias, "");
                    alias = alias.clear();

                } else if (c == '(' || c == ')' || c == '\\') {
                    //do nothing

                    // commented out: we'll get the xxx as yyy all in the alias. Clients may treat it separately if needed.
                    //                }else if(c == ' ' || c == '\t'){
                    //                    
                    //                    String curr = alias.toString();
                    //                    if(curr.endsWith(" as") | curr.endsWith("\tas")){
                    //                        alias = new StringBuffer();
                    //                    }
                    //                    alias.append(c);

                } else {
                    alias.append(c);
                }
            }

            if (alias.length() > 0) {
                addImportAlias(lst, importComments, alias, "");

            }

            this.importedStrComments = importComments;
            this.importedStr = lst;
        }

        /**
         * Adds an import and its related comment to the given lists (if there's actually something available to be
         * added)
         * 
         * @param lst list where the alias will be added
         * @param importComments list where the comment will be added
         * @param alias the name of the import to be added
         * @param importComment the comment related to the import
         */
        private void addImportAlias(ArrayList<String> lst, ArrayList<String> importComments, FastStringBuffer alias,
                String importComment) {

            String aliasStr = alias.toString().trim();
            importComment = importComment.trim();

            if (aliasStr.length() > 0) {
                lst.add(aliasStr);
                importComments.add(importComment);
            } else if (importComment.length() > 0 && importComments.size() > 0) {
                importComments.set(importComments.size() - 1, importComment);
            }
        }

        /**
         * @return the from module in the import
         */
        public String getFromImportStr() {
            return this.fromStr;
        }

        /**
         * @return the tokens imported from the module (or the alias if it's specified)
         */
        public List<String> getImportedStr() {
            return this.importedStr;
        }

        /**
         * @return a list with a string for each imported token correspondent to a comment related to that import.
         */
        public List<String> getCommentsForImports() {
            return this.importedStrComments;
        }

        /**
         * @return the start line for this import (0-based)
         */
        public int getStartLine() {
            return this.startLine;
        }

        /**
         * @return the end line for this import (0-based)
         */
        public int getEndLine() {
            return this.endLine;
        }

        /**
         * @return true if this import was started in the middle of the line. I.e.: after a ';'
         */
        public boolean getStartedInMiddleOfLine() {
            return this.startedInMiddleOfLine;
        }

        /**
         * @return a list of tuples with the iported string and the comment that's attached to it.
         */
        public List<Tuple<String, String>> getImportedStrAndComments() {
            ArrayList<Tuple<String, String>> lst = new ArrayList<Tuple<String, String>>();
            for (int i = 0; i < this.importedStr.size(); i++) {
                lst.add(new Tuple<String, String>(this.importedStr.get(i), this.importedStrComments.get(i)));
            }
            return lst;
        }

    }

    /**
     * Document where the import was found
     */
    public final IDocument doc;

    /**
     * The import string found. Note: it may contain comments and multi-lines.
     */
    public final String importFound;

    /**
     * The initial line where the import was found
     */
    public final int startFoundLine;

    /**
     * The final line where the import was found
     */
    public int endFoundLine;

    /**
     * Import information for the import found and handled in this class (only created on request)
     */
    private final List<ImportHandleInfo> importInfo;

    private final boolean allowBadInput;

    /**
     * Constructor.
     * 
     * Assigns parameters to fields.
     * @param allowBadInput 
     * @throws ImportNotRecognizedException 
     */
    public ImportHandle(IDocument doc, String importFound, int startFoundLine, int endFoundLine, boolean allowBadInput)
            throws ImportNotRecognizedException {
        this.doc = doc;
        this.importFound = importFound;
        this.startFoundLine = startFoundLine;
        this.endFoundLine = endFoundLine;

        this.importInfo = new ArrayList<ImportHandleInfo>();
        this.allowBadInput = allowBadInput;

        int line = startFoundLine;
        boolean startedInMiddle = false;

        FastStringBuffer imp = new FastStringBuffer();
        ImportHandleInfo found = null;
        for (int i = 0; i < importFound.length(); i++) {
            char c = importFound.charAt(i);

            if (c == '#') {
                i = ParsingUtils.create(importFound).eatComments(imp, i);

            } else if (c == ';') {
                String impStr = imp.toString();
                int endLine = line + StringUtils.countLineBreaks(impStr);
                found = new ImportHandleInfo(impStr, line, endLine, startedInMiddle, allowBadInput);
                this.importInfo.add(found);
                line = endLine;
                imp = imp.clear();
                startedInMiddle = true;
            } else {
                if (c == '\r' || c == '\n') {
                    startedInMiddle = false;
                }
                imp.append(c);
            }

        }
        String impStr = imp.toString();
        this.importInfo.add(new ImportHandleInfo(impStr, line, line + StringUtils.countLineBreaks(impStr),
                startedInMiddle, allowBadInput));

    }

    public ImportHandle(IDocument doc, String importFound, int startFoundLine, int endFoundLine)
            throws ImportNotRecognizedException {
        this(doc, importFound, startFoundLine, endFoundLine, false);
    }

    /**
     * @param realImportHandleInfo the import to match. Note that only a single import statement may be passed as a parameter.
     * 
     * @return true if the passed import matches the import in this handle (note: as this class can actually wrap more
     * than 1 import, it'll return true if any of the internal imports match the passed import)
     * @throws ImportNotRecognizedException if the passed import could not be recognized
     */
    public boolean contains(ImportHandleInfo otherImportInfo) throws ImportNotRecognizedException {
        List<ImportHandleInfo> importHandleInfo = this.getImportInfo();

        for (ImportHandleInfo info : importHandleInfo) {
            if (info.fromStr != otherImportInfo.fromStr) {
                if (otherImportInfo.fromStr == null || info.fromStr == null) {
                    continue; //keep on to the next possible match
                }
                if (!otherImportInfo.fromStr.equals(info.fromStr)) {
                    continue; //keep on to the next possible match
                }
            }

            if (otherImportInfo.importedStr.size() != 1) {
                continue;
            }

            if (info.importedStr.contains(otherImportInfo.importedStr.get(0))) {
                return true;
            }

        }

        return false;
    }

    /**
     * @return a list with the import information generated from the import this handle is wrapping.
     */
    public List<ImportHandleInfo> getImportInfo() {
        return this.importInfo;
    }

}
