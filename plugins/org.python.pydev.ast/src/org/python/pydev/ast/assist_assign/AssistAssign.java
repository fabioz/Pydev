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
package org.python.pydev.ast.assist_assign;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.core.IAssistProps;
import org.python.pydev.core.ICodingStd;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PyDevCodeStylePreferences;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.core.templates.PyAddTemplateResolvers;
import org.python.pydev.core.templates.PyDocumentTemplateContext;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;

/**
 * @author Fabio Zadrozny
 */
public class AssistAssign implements IAssistProps {

    private ICodingStd std;

    public AssistAssign() {
        this(new ICodingStd() {

            @Override
            public boolean localsAndAttrsCamelcase() {
                return PyDevCodeStylePreferences.useLocalsAndAttrsCamelCase();
            }

        });
    }

    public AssistAssign(ICodingStd std) {
        this.std = std;
    }

    private IImageHandle getImage(IImageCache imageCache, String c) {
        if (imageCache != null) {
            return imageCache.get(c);
        }
        return null;
    }

    /**
     * @see org.python.pydev.core.IAssistProps#getProps
     */
    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException {
        return this.getProps(ps, imageCache, edit, offset, TextSelectionUtils.getLineWithoutComments(ps),
                PySelection.getFirstCharPosition(ps.getDoc(), ps.getAbsoluteCursorOffset()), nature);
    }

    /**
     * @param lineWithoutComments the line that should be checked (without any comments)
     */
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, IPyEdit edit,
            int offset, String lineWithoutComments, int firstCharAbsolutePosition, IPythonNature nature)
            throws BadLocationException {

        List<ICompletionProposalHandle> l = new ArrayList<>();
        if (lineWithoutComments.trim().length() == 0) {
            return l;
        }

        //go on and make the suggestion.
        //
        //if we have a method call, eg.:
        //  e.methodCall()| would result in the following suggestions:
        //
        //                   methodCall = e.methodCall()
        //                     self.methodCall = e.methodCall()
        //
        // NewClass()| would result in
        //
        //                   newClass = NewClass()
        //                     self.newClass = NewClass()
        //
        //now, if we don't have a method call, eg.:
        // 1+1| would result in
        //
        //                     |result| = 1+1
        //                     self.|result| = 1+1

        String callName = getTokToAssign(ps);

        if (callName.length() > 0) {
            //all that just to change first char to lower case.
            if (callName.toLowerCase().startsWith("get") && callName.length() > 3) {
                callName = callName.substring(3);
            }

            callName = changeToCodingStd(callName);

            for (int i = 0; i < callName.length(); i++) {
                char c = callName.charAt(i);
                if (c != '_') {
                    callName = TextSelectionUtils.lowerChar(callName, i);
                    break;
                }
            }
        } else {
            callName = "result";
        }

        String loc = callName;
        if (loc.startsWith("_")) {
            loc = loc.substring(1);
        }

        IIndentPrefs indentPrefs = edit != null ? edit.getIndentPrefs() : null;
        if (indentPrefs == null) {
            indentPrefs = DefaultIndentPrefs.get(nature);
        }

        // Unfortunately in the IScriptConsole applying a template proposal doesn't work very well (on the first char
        // typed it will exit the linked mode and the user will type in the exit location).
        //        int len = offset - firstCharAbsolutePosition;
        //        IRegion region = new Region(firstCharAbsolutePosition, len);
        //        String upToCursor = ps.getDoc().get(firstCharAbsolutePosition, len);
        //        TemplateContext context = createContext(region, ps.getDoc(), indentPrefs);
        //
        //        Template t = new Template("Assign to local (" + loc + ")", "", "", "${" + loc + "}${cursor} = " + upToCursor,
        //                false);
        //        l.add(CompletionProposalFactory.get().createPyTemplateProposal(t, context, region,
        //                imageCache == null ? null : imageCache.get(UIConstants.COMPLETION_TEMPLATE),
        //                IPyCompletionProposal.PRIORITY_DEFAULT));
        //
        //        t = new Template("Assign to field (self." + callName + ")", "", "",
        //                "self.${" + callName + "}${cursor} = " + upToCursor,
        //                false);
        //        l.add(CompletionProposalFactory.get().createPyTemplateProposal(t, context, region,
        //                imageCache == null ? null : imageCache.get(UIConstants.COMPLETION_TEMPLATE),
        //                IPyCompletionProposal.PRIORITY_DEFAULT));

        IImageHandle localImg = getImage(imageCache, UIConstants.ASSIST_ASSIGN_TO_LOCAL);
        IImageHandle clsImage = getImage(imageCache, UIConstants.ASSIST_ASSIGN_TO_CLASS);

        l.add(CompletionProposalFactory.get().createAssistAssignCompletionProposal("${" + loc + "} = ",
                firstCharAbsolutePosition, 0, 0, localImg, "Assign to local (" + loc + ")", null, null,
                IPyCompletionProposal.PRIORITY_DEFAULT, edit));

        l.add(CompletionProposalFactory.get().createAssistAssignCompletionProposal(
                "self.${" + callName + "} = ",
                firstCharAbsolutePosition, 0, 5, clsImage, "Assign to field (self." + callName + ")", null, null,
                IPyCompletionProposal.PRIORITY_DEFAULT, edit));
        return l;
    }

    public static TemplateContext createContext(IRegion region, IDocument document, IIndentPrefs indentPrefs) {
        TemplateContextType contextType = new TemplateContextType();
        PyAddTemplateResolvers.addDefaultResolvers(contextType);
        return new PyDocumentTemplateContext(contextType, document, region.getOffset(), region.getLength(), "",
                indentPrefs);
    }

    private String changeToCodingStd(String callName) {
        if (this.std.localsAndAttrsCamelcase()) {
            return StringUtils.asStyleCamelCaseFirstLower(callName);

        } else {
            return StringUtils.asStyleLowercaseUnderscores(callName);
        }
    }

    /**
     * @see org.python.pydev.core.IAssistProps#isValid
     */
    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return isValid(ps.getTextSelection().getLength(), sel, offset);
    }

    /**
     * @param selectionLength the length of the currently selected text
     * @param lineContents the contents of the line
     * @param offset the offset of the cursor
     * @return true if an assign is available and false otherwise
     */
    public boolean isValid(int selectionLength, String lineContents, int offset) {
        if (!(selectionLength == 0)) {
            return false;
        }

        if (!(lineContents.indexOf("class ") == -1 && lineContents.indexOf("def ") == -1 && lineContents
                .indexOf("import ") == -1)) {

            return false;
        }

        String eqReplaced = lineContents.replaceAll("==", "");
        if (eqReplaced.indexOf("=") != -1) { //we have some equal
            //ok, make analysis taking into account the first parentesis
            if (eqReplaced.indexOf('(') == -1) {
                return false;
            }
            int i = eqReplaced.indexOf('(');
            if (eqReplaced.substring(0, i).indexOf('=') != -1) {
                return false;
            }
        }
        return true;
    }

    private static String getStringToAnalyze(PySelection ps) {
        ParsingUtils parsingUtils = ParsingUtils.create(ps.getDoc());
        FastStringBuffer buf = new FastStringBuffer();
        String string = null;
        try {
            parsingUtils.getFullFlattenedLine(ps.getStartLineOffset(), buf);
            if (buf.length() > 0) {
                string = buf.toString();
            }
        } catch (SyntaxErrorException e) {
            //won't happen (we didn't ask for it)
            Log.log(e);
        }
        if (string == null) {
            string = TextSelectionUtils.getLineWithoutComments(ps);
        }
        return string.trim();
    }

    /**
     * @return string with the token or empty token if not found.
     */
    private static String getBeforeParentesisTok(String string) {
        int i;

        String callName = "";
        //get parenthesis position and go backwards
        if ((i = string.lastIndexOf("(")) != -1) {
            callName = "";

            for (int j = i - 1; j >= 0 && TextSelectionUtils.stillInTok(string, j); j--) {
                callName = string.charAt(j) + callName;
            }

        }
        return callName;
    }

    /**
     * @return the token which should be used to make the assign.
     */
    private String getTokToAssign(PySelection ps) {
        String string = getStringToAnalyze(ps); //it's already trimmed!
        String tokToAssign = getTokToAssign(string);
        if (tokToAssign == null || tokToAssign.length() == 0) {
            return "result";
        }
        return tokToAssign;
    }

    private String changeToLowerUppercaseConstant(String callName) {
        if (StringUtils.isAllUpper(callName)) {
            return callName.toLowerCase();
        }
        return callName;
    }

    public String getTokToAssign(String string) {
        string = string.trim();

        String callName = "";

        String beforeParentesisTok = getBeforeParentesisTok(string);
        if (beforeParentesisTok.length() > 0) {
            callName = beforeParentesisTok;
        } else {
            //otherwise, try to find . (ignore code after #)
            int i;
            if ((i = string.lastIndexOf(".")) != -1) {
                callName = "";

                for (int j = i + 1; j < string.length() && TextSelectionUtils.stillInTok(string, j); j++) {
                    callName += string.charAt(j);
                }
            }
            if (callName.length() == 0) {
                if (PyStringUtils.isPythonIdentifier(string)) {
                    callName = string;
                }
            }
        }
        callName = changeToLowerUppercaseConstant(callName);

        if (callName.length() > 0) {
            //all that just to change first char to lower case.
            if (callName.toLowerCase().startsWith("get") && callName.length() > 3) {
                callName = callName.substring(3);

            } else if (callName.toLowerCase().startsWith("_get") && callName.length() > 4) {
                callName = callName.substring(4);
            }

            callName = changeToCodingStd(callName);

            for (int i = 0; i < callName.length(); i++) {
                char c = callName.charAt(i);
                if (c != '_') {
                    callName = TextSelectionUtils.lowerChar(callName, i);
                    break;
                }
            }
        } else {
            callName = null;
        }
        return callName;
    }

}
