/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.shared_core.string.DocIterator;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class ImportsSelection {

    private static final Pattern FromPattern = Pattern
            .compile("(\\bfrom\\s*)");
    private static final Pattern ImportPattern = Pattern
            .compile("(\\bimport\\b\\s*)");

    /**
     * @return the import info or null if none is available
     */
    public static ImportInfo getImportsTipperStr(String leftTrimmedLine, final boolean returnEvenEmpty) {
        if (!leftTrimmedLine.startsWith("from") && !leftTrimmedLine.startsWith("import")) {
            return new ImportInfo("", false, false); // it is not an import
        }

        final FastStringBuffer docContents = new FastStringBuffer(leftTrimmedLine, 0);

        Matcher matcher = FromPattern.matcher(leftTrimmedLine);
        int fromIndexEnd = -1;
        if (matcher.find()) {
            fromIndexEnd = matcher.end();
        }
        matcher = ImportPattern.matcher(leftTrimmedLine);
        int importIndexEnd = -1;
        if (matcher.find()) {
            importIndexEnd = matcher.end();
        }
        matcher = null;
        if (importIndexEnd == -1 && fromIndexEnd == -1) {
            return new ImportInfo("", false, false); // it is not an import
        }

        fixDocContents(fromIndexEnd, importIndexEnd, docContents);

        IDocument doc = new Document(docContents.toString());
        PyImportsHandling pyImportsHandling = new PyImportsHandling(doc, false, true);
        Iterator<ImportHandle> it = pyImportsHandling.iterator();
        if (!it.hasNext()) {
            return new ImportInfo("", importIndexEnd != -1, fromIndexEnd != -1);
        }
        ImportHandle next = it.next();
        List<ImportHandleInfo> importInfo = next.getImportInfo();
        if (importInfo.size() == 0) {
            return new ImportInfo(" ", importIndexEnd != -1, fromIndexEnd != -1);
        }
        ImportHandleInfo importHandleInfo = importInfo.get(importInfo.size() - 1);
        String fromImportStr = importHandleInfo.getFromImportStr();

        int leadingDots = 0;
        if (fromImportStr != null) {
            FastStringBuffer bufLeadingChars = new FastStringBuffer(fromImportStr, 0);

            for (int i = 0; i < bufLeadingChars.length(); i++) {
                if (bufLeadingChars.charAt(i) == '.') {
                    leadingDots += 1;
                } else {
                    break;
                }
            }
            if (leadingDots > 0) {
                bufLeadingChars.deleteFirstChars(leadingDots);
                fromImportStr = bufLeadingChars.toString();
            }

        }
        FastStringBuffer buf;

        List<String> importedStr = importHandleInfo.getImportedStr();
        Iterator<String> importedStrIterator = importedStr.iterator();

        final boolean hasFrom = importHandleInfo.getFoundFrom();
        final boolean hasImport = importHandleInfo.getFoundImport();
        final boolean addedFromPart = fromImportStr != null;
        final boolean addedImportPart = importedStrIterator.hasNext();

        if (fromImportStr == null) {
            buf = new FastStringBuffer(40);
        } else {
            buf = new FastStringBuffer(fromImportStr, 20);
            if (importedStrIterator.hasNext()) {
                buf.append('.');
            }
        }
        while (importedStrIterator.hasNext()) {
            buf.append(importedStrIterator.next());
        }

        buf.trim();

        if (hasFrom && hasImport && addedFromPart && !addedImportPart) {
            return buildImportInfo(buf, hasFrom, hasImport, leadingDots);
        }

        if ((hasFrom && !hasImport) || (!hasFrom && hasImport)) {
            int dots = StringUtils.countChars('.', buf);
            if (dots == buf.length()) {
                return buildImportInfo(buf, hasFrom, hasImport, leadingDots);
            }
        }

        if (buf.endsWith('.')) {
            buf.deleteLast();
            return buildImportInfo(buf, hasFrom, hasImport, leadingDots);

        } else {

            int lastIndexOf = buf.lastIndexOf('.');
            if (lastIndexOf >= 0) {
                buf.keepCharsUpTo(lastIndexOf);
                return buildImportInfo(buf, hasFrom, hasImport, leadingDots);
            } else {
                if (leadingDots > 0) {
                    return new ImportInfo(new FastStringBuffer("", leadingDots).appendN('.', leadingDots).toString(),
                            hasImport, hasFrom);
                }
                return new ImportInfo(" ", hasImport, hasFrom);
            }
        }
    }

    private static ImportInfo buildImportInfo(FastStringBuffer buf, final boolean hasFrom, final boolean hasImport,
            int leadingDots) {
        if (leadingDots > 0) {
            buf.insertN(0, '.', leadingDots);
        } else {
            if (buf.length() == 0) {
                buf.append(' '); // Leave a space just to note something was found.
            }
        }
        return new ImportInfo(buf.toString(), hasImport, hasFrom);
    }

    private static void fixDocContents(int fromIndexEnd, int importIndexEnd, FastStringBuffer docContents) {
        int commaIndex = docContents.lastIndexOf(',');
        if (commaIndex == -1) {
            return;
        }

        if (importIndexEnd != -1) {
            // we have something like import xxx, yyy, ...
            docContents.delete(importIndexEnd, commaIndex + 1);

        } else {
            docContents.delete(fromIndexEnd, commaIndex + 1);

        }
    }

    public static ImportInfo getImportsTipperStr(IDocument doc, int documentOffset) {
        DocIterator iterator = new DocIterator(false, new PySelection(doc, documentOffset));
        StringBuffer buffer = new StringBuffer();

        //it may still be a multiline-import... to check that, we have to go backward in the document and see if the
        //lines keep ending with a \ from the import line or if the import line has a (

        boolean expectContinue = false;
        boolean allEndingWithSlash = true;
        boolean found = false;

        while (iterator.hasNext()) {
            String line = ParsingUtils.removeComments(iterator.next());
            String leftTrimmedLine = line.stripLeading();
            String trimmedLine = line.strip();

            if (PySelection.isImportLine(trimmedLine)) {
                if (expectContinue) {
                    boolean correct = false;

                    if (trimmedLine.indexOf('(') != -1) {
                        correct = true;
                    }
                    if (leftTrimmedLine.endsWith("\\")) {
                        if (allEndingWithSlash) {
                            correct = true;
                        }
                    }

                    if (!correct) {
                        break;
                    }
                }
                //that's it, we found it!
                found = true;
                buffer.insert(0, leftTrimmedLine);
                break;

            } else {
                if (expectContinue) {
                    if (trimmedLine.indexOf(')') != -1) {
                        break;
                    }
                    if (allEndingWithSlash && trimmedLine.length() > 0) {
                        char c = trimmedLine.charAt(trimmedLine.length() - 1);
                        if (c != '\\' && c != ',' && c != '(') {
                            break; //let's keep the analysis to lines that end with , ( \ (because otherwise it can be time-consuming)
                        }
                        if (c != '\\') {
                            allEndingWithSlash = false;
                        }
                    } else if (trimmedLine.length() == 0) {
                        //if some empty line was found, stop the analysis.
                        break;
                    }
                }
                expectContinue = true;
                buffer.insert(0, line);
            }
        }

        if (!found) {
            return new ImportInfo("", false, false); // it is not an import
        }

        return getImportsTipperStr(buffer.toString(), true);
    }

}
