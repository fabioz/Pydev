/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.docstrings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.DocstringInfo;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

public class AssistDocString implements IAssistProps {

    private final String docStringStyle;

    public AssistDocString() {
        this(null);
    }

    /**
     * @param docStringStyle the doc string prefix to be used (i.e.: '@' or ':'). If null, it's gotten from the preferences.
     */
    public AssistDocString(String docStringStyle) {
        this.docStringStyle = docStringStyle;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection,
     *      org.python.pydev.shared_ui.ImageCache)
     */
    @Override
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature,
            PyEdit edit, int offset) throws BadLocationException {
        ArrayList<ICompletionProposal> l = new ArrayList<ICompletionProposal>();

        Tuple<List<String>, Integer> tuple = ps.getInsideParentesisToks(false);
        if (tuple == null) {
            if (ps.isInClassLine()) {
                tuple = new Tuple<List<String>, Integer>(new ArrayList<String>(), offset);
            } else {
                return l;
            }
        }
        List<String> params = tuple.o1;
        int lineOfOffset = ps.getLineOfOffset(tuple.o2);

        // Calculate only the initial part of the docstring here (everything else should be lazily computed on apply).
        String initial = PySelection.getIndentationFromLine(ps.getCursorLineContents());
        String delimiter = PyAction.getDelimiter(ps.getDoc());
        String indentation = edit != null ? edit.getIndentPrefs().getIndentationString() : DefaultIndentPrefs.get(
                nature).getIndentationString();
        String delimiterAndIndent = delimiter + initial + indentation;

        FastStringBuffer buf = new FastStringBuffer();
        String docStringMarker = DocstringsPrefPage.getDocstringMarker();
        buf.append(delimiterAndIndent + docStringMarker);
        buf.append(delimiterAndIndent);

        int newOffset = buf.length();
        int offsetPosToAdd = ps.getEndLineOffset(lineOfOffset);

        Image image = null; //may be null (testing)
        if (imageCache != null) {
            image = imageCache.get(UIConstants.ASSIST_DOCSTRING);
        }
        final boolean inFunctionLine = ps.isInFunctionLine(true);
        DocstringInfo docstringFromFunction = null;
        if (inFunctionLine) {
            int currLine = ps.getLineOfOffset();
            docstringFromFunction = ps.getDocstringFromLine(currLine + 1);
        }
        final DocstringInfo finalDocstringFromFunction = docstringFromFunction;

        l.add(new PyCompletionProposal("", offsetPosToAdd, 0, newOffset, image,
                finalDocstringFromFunction != null ? "Update docstring" : "Make docstring", null, null,
                IPyCompletionProposal.PRIORITY_DEFAULT, null) {
            @Override
            public void apply(IDocument document) {
                if (inFunctionLine) {
                    String preferredDocstringStyle = AssistDocString.this.docStringStyle;
                    if (preferredDocstringStyle == null) {
                        preferredDocstringStyle = DocstringsPrefPage.getPreferredDocstringStyle();
                    }

                    // Let's check if this function already has a docstring (if it does, update the current docstring
                    // instead of creating a new one).
                    String updatedDocstring = null;
                    if (finalDocstringFromFunction != null) {
                        updatedDocstring = updatedDocstring(finalDocstringFromFunction.string, params, delimiter,
                                initial + indentation,
                                preferredDocstringStyle);
                    }
                    if (updatedDocstring != null) {
                        fReplacementOffset = finalDocstringFromFunction.startLiteralOffset;
                        fReplacementLength = finalDocstringFromFunction.endLiteralOffset
                                - finalDocstringFromFunction.startLiteralOffset;
                        int initialLen = buf.length();
                        buf.clear();
                        fCursorPosition -= initialLen - buf.length();
                        buf.append(updatedDocstring);
                    } else {
                        // Create the docstrings
                        for (String paramName : params) {
                            if (!PySelection.isIdentifier(paramName)) {
                                continue;
                            }
                            buf.append(delimiterAndIndent).append(preferredDocstringStyle).append("param ")
                                    .append(paramName)
                                    .append(":");
                            if (DocstringsPrefPage.getTypeTagShouldBeGenerated(paramName)) {
                                buf.append(delimiterAndIndent).append(preferredDocstringStyle).append("type ")
                                        .append(paramName)
                                        .append(":");
                            }
                        }
                    }

                } else {
                    // It's a class declaration - do nothing.
                }
                if (finalDocstringFromFunction == null) {
                    buf.append(delimiterAndIndent).append(docStringMarker);
                }

                String comp = buf.toString();
                this.fReplacementString = comp;

                //remove the next line if it is a pass...
                if (finalDocstringFromFunction == null) {
                    PySelection ps = new PySelection(document, fReplacementOffset);
                    int iNextLine = ps.getCursorLine() + 1;
                    String nextLine = ps.getLine(iNextLine);
                    if (nextLine.trim().equals("pass")) {
                        ps.deleteLine(iNextLine);
                    }
                }
                super.apply(document);
            }
        });
        return l;
    }

    private static class ParamInfo {

        public int paramLine = -1;
        public int typeLine = -1;

        public ParamInfo() {
        }

    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection,
     *      java.lang.String)
     */
    @Override
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return ps.isInFunctionLine(true) || ps.isInClassLine();
    }

    /**
     * @param baseDocstring this is the existing docstring (starting with ''', ', """, ").
     * @param params
     * @param delimiterAndIndent
     * @param docstringStyle
     * @return null if there was some issue updating the existing docstring.
     */
    public static String updatedDocstring(String baseDocstring, List<String> params, String delimiter, String indent,
            String docstringStyle) {

        String docStringStartEnd;
        if (baseDocstring.startsWith("\"\"\"") && baseDocstring.endsWith("\"\"\"")) {
            docStringStartEnd = "\"\"\"";
        } else if (baseDocstring.startsWith("'''") && baseDocstring.endsWith("'''")) {
            docStringStartEnd = "'''";
        } else if (baseDocstring.startsWith("'") && baseDocstring.endsWith("'")) {
            docStringStartEnd = "'";
        } else if (baseDocstring.startsWith("\"") && baseDocstring.endsWith("\"")) {
            docStringStartEnd = "\"";
        } else {
            return null;
        }
        // Get it without the initial char quotes.
        baseDocstring = baseDocstring.substring(docStringStartEnd.length(),
                baseDocstring.length() - docStringStartEnd.length());

        // Parse the existing string to find existing param and type declarations.

        // param xxx:
        Pattern paramPattern = Pattern
                .compile("\\s*(" + Pattern.quote(docstringStyle) + "param(\\s)+)(\\w+)(\\s)*" + Pattern.quote(":"));

        // param dict(foo->`bar`) xxx:
        Pattern paramPatternWithTypeOnSphinx = Pattern
                .compile("\\s*(" + Pattern.quote(docstringStyle) + "param(\\s)+.*\\s+)(\\w+)(\\s)*"
                        + Pattern.quote(":"));
        // type xxx:
        Pattern typePattern = Pattern
                .compile("\\s*(" + Pattern.quote(docstringStyle) + "type(\\s)+)(\\w+)(\\s)*" + Pattern.quote(":"));

        // @test
        // :test
        Pattern otherPattern = Pattern
                .compile("\\s*" + Pattern.quote(docstringStyle) + "(\\w)+(\\b)");

        Map<String, ParamInfo> paramInfos = new HashMap<>();

        List<String> splitted = StringUtils.splitInLines(baseDocstring, false);
        String firstLine = splitted.get(0).trim();
        if (firstLine.length() > 0) {
            // First line must have only the delimiter.
            splitted.add(0, "");
            splitted.set(1, indent + splitted.get(1));
        }

        Set<String> paramsWithTypeInline = new HashSet<>();

        // Any :xxx tag that is not param nor type.
        Set<Integer> otherMatches = new HashSet<>();
        int size = splitted.size();
        for (int i = 0; i < size; i++) {
            String s = splitted.get(i);
            Matcher paramMatcher = paramPattern.matcher(s);

            if (paramMatcher.lookingAt()) {
                // Ok, we found some existing parameter docstring.
                String paramName = paramMatcher.group(3);
                getParamInfo(paramInfos, paramName).paramLine = i;
            } else {
                Matcher matcher = paramPatternWithTypeOnSphinx.matcher(s);
                if (matcher.lookingAt()) {
                    // Ok, we found some existing parameter docstring.
                    String paramName = matcher.group(3);
                    getParamInfo(paramInfos, paramName).paramLine = i;
                    paramsWithTypeInline.add(paramName);
                } else {
                    Matcher typeMatcher = typePattern.matcher(s);
                    if (typeMatcher.lookingAt()) {
                        // Ok, we found some existing parameter type docstring.
                        String paramName = typeMatcher.group(3);
                        getParamInfo(paramInfos, paramName).typeLine = i;
                    } else {
                        Matcher otherMatcher = otherPattern.matcher(s);
                        if (otherMatcher.lookingAt()) {
                            otherMatches.add(i);
                        }
                    }
                }
            }
        }

        // Now, actually go on and insert the new strings.
        FastStringBuffer buf = new FastStringBuffer();
        int paramsSize = params.size();
        for (int paramI = 0; paramI < paramsSize; paramI++) {
            String paramName = params.get(paramI);
            if (!PySelection.isIdentifier(paramName)) {
                continue;
            }

            ParamInfo existingInfo = paramInfos.get(paramName);
            boolean hasParam = existingInfo != null && existingInfo.paramLine != -1;
            boolean addTypeForParam = DocstringsPrefPage.getTypeTagShouldBeGenerated(paramName);
            if (addTypeForParam) {
                if (paramsWithTypeInline.contains(paramName)) {
                    addTypeForParam = false;
                }
            }
            boolean hasType = existingInfo != null && existingInfo.typeLine != -1;

            if (hasParam && hasType) {
                // Both there (keep on going, nothing to do).
            } else if (!hasParam && !hasType) {
                ParamInfo newParamInfo = null;
                if (existingInfo == null) {
                    newParamInfo = new ParamInfo();
                }

                int addIndex = getAddIndex(paramName, params, paramI, paramInfos, otherMatches, splitted);
                // neither is present, so, add both at a given location (if needed).
                splitted.add(addIndex, buf.append(indent).append(docstringStyle).append("param ")
                        .append(paramName).append(":").toString());
                buf.clear();
                incrementExistingLines(paramInfos, otherMatches, addIndex);
                if (newParamInfo != null) {
                    newParamInfo.paramLine = addIndex;
                }

                if (addTypeForParam) {
                    splitted.add(addIndex + 1, buf.append(indent).append(docstringStyle).append("type ")
                            .append(paramName).append(":").toString());
                    buf.clear();
                    incrementExistingLines(paramInfos, otherMatches, addIndex);
                    if (newParamInfo != null) {
                        newParamInfo.typeLine = addIndex + 1;
                    }
                }

                paramInfos.put(paramName, newParamInfo);
            } else {
                if (hasParam) {
                    // Add only the type after the existing param (if it has to be added)
                    if (addTypeForParam) {
                        int addIndex = existingInfo.paramLine + 1;

                        splitted.add(addIndex, buf.append(indent).append(docstringStyle).append("type ")
                                .append(paramName).append(":").toString());
                        buf.clear();
                        incrementExistingLines(paramInfos, otherMatches, addIndex);
                        existingInfo.typeLine = addIndex;
                    }
                } else {
                    // Add only the param before the existing type.
                    int addIndex = existingInfo.typeLine;
                    splitted.add(addIndex, buf.append(indent).append(docstringStyle).append("param ")
                            .append(paramName).append(":").toString());
                    buf.clear();
                    incrementExistingLines(paramInfos, otherMatches, addIndex);
                    existingInfo.paramLine = addIndex;
                }
            }
        }
        String lastLine = splitted.get(splitted.size() - 1).trim();
        if (lastLine.length() > 0) {
            // Last line must have only the indentation (compute after all new tags are added).
            splitted.add(indent);
        }
        buf.append(docStringStartEnd).append(StringUtils.join(delimiter, splitted)).append(docStringStartEnd);
        return buf.toString();
    }

    private static void incrementExistingLines(Map<String, ParamInfo> paramInfos, Set<Integer> otherMatches,
            int index) {
        Set<Entry<String, ParamInfo>> entrySet = paramInfos.entrySet();
        for (Entry<String, ParamInfo> entry : entrySet) {
            ParamInfo paramInfo = entry.getValue();
            if (paramInfo.paramLine >= index) {
                paramInfo.paramLine++;
            }
            if (paramInfo.typeLine >= index) {
                paramInfo.typeLine++;
            }
        }
        HashSet<Integer> newOtherMatches = new HashSet<>();
        for (Iterator<Integer> it = otherMatches.iterator(); it.hasNext();) {
            Integer i = it.next();
            if (i >= index) {
                newOtherMatches.add(i + 1);
            } else {
                newOtherMatches.add(i);
            }
        }
        otherMatches.clear();
        otherMatches.addAll(newOtherMatches);
    }

    private static int getAddIndex(String paramName, List<String> params, int paramI, Map<String, ParamInfo> paramInfos,
            Set<Integer> otherMatches, List<String> splitted) {
        if (paramI == 0) {
            int min = splitted.size(); // Last pos (after header)

            // See if there's some other tag (if there is, this one should be before it.
            if (paramInfos.size() > 0) {
                Set<Entry<String, ParamInfo>> entrySet = paramInfos.entrySet();
                for (Entry<String, ParamInfo> entry : entrySet) {
                    ParamInfo paramInfo = entry.getValue();
                    if (paramInfo.paramLine != -1) {
                        min = Math.min(paramInfo.paramLine, min);
                    }
                    if (paramInfo.typeLine != -1) {
                        min = Math.min(paramInfo.typeLine, min);
                    }
                }
            }
            for (Integer i : otherMatches) {
                min = Math.min(i, min);
            }
            return min;
        } else {
            // We should add it after some existing param.
            String prevParam = params.get(paramI - 1);
            ParamInfo paramInfo = paramInfos.get(prevParam);
            // At this point, paramInfo from the previous parameter *must* be valid (we should've
            // filled it when adding the previous parameter if it still wasn't there).
            if (paramInfo.typeLine != -1) {
                return paramInfo.typeLine + 1;
            } else {
                return paramInfo.paramLine + 1;
            }
        }
    }

    private static ParamInfo getParamInfo(Map<String, ParamInfo> paramInfos, String paramName) {
        ParamInfo paramInfo = paramInfos.get(paramName);
        if (paramInfo == null) {
            paramInfo = new ParamInfo();
            paramInfos.put(paramName, paramInfo);
        }
        return paramInfo;
    }
}
