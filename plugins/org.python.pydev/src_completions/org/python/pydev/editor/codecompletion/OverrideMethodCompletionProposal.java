/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;

/**
 * @author fabioz
 *
 */
public class OverrideMethodCompletionProposal extends AbstractPyCompletionProposalExtension2 {

    private final FunctionDef functionDef;
    private final String parentClassName;
    private String currentClassName;

    public OverrideMethodCompletionProposal(int replacementOffset, int replacementLength, int cursorPosition,
            Image image, FunctionDef functionDef, String parentClassName, String currentClassName) {
        super("", replacementOffset, replacementLength, cursorPosition, IPyCompletionProposal.PRIORITY_CREATE);
        this.fImage = image;
        this.functionDef = functionDef;
        this.fDisplayString = ((NameTok) functionDef.name).id + " (Override method in " + parentClassName + ")";
        this.parentClassName = parentClassName;
        this.currentClassName = currentClassName;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.editor.codecompletion.PyCompletionProposal#apply(org.eclipse.jface.text.IDocument)
     */
    @Override
    public void apply(IDocument document) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
     */
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        IDocument document = viewer.getDocument();
        int finalOffset = applyOnDocument(viewer, document, trigger, stateMask, offset);
        if (finalOffset >= 0) {
            try {
                PySelection ps = new PySelection(document, finalOffset);
                int firstCharPosition = PySelection.getFirstCharPosition(ps.getLine());
                int lineOffset = ps.getLineOffset();
                int location = lineOffset + firstCharPosition;
                int len = finalOffset - location;
                fCursorPosition = location;
                fReplacementLength = len;

            } catch (Exception e) {
                Log.log(e);
            }

        }
    }

    public int applyOnDocument(ITextViewer viewer, IDocument document, char trigger, int stateMask, int offset) {
        IGrammarVersionProvider versionProvider = null;
        PyEdit edit = null;
        if (viewer instanceof PySourceViewer) {
            PySourceViewer pySourceViewer = (PySourceViewer) viewer;
            versionProvider = edit = pySourceViewer.getEdit();
        } else {
            versionProvider = new IGrammarVersionProvider() {

                public int getGrammarVersion() throws MisconfigurationException {
                    return IGrammarVersionProvider.LATEST_GRAMMAR_VERSION;
                }
            };
        }
        String delimiter = PySelection.getDelimiter(document);

        PyAstFactory factory = new PyAstFactory(new AdapterPrefs(delimiter, versionProvider));
        stmtType overrideBody = factory.createOverrideBody(this.functionDef, parentClassName, currentClassName); //Note that the copy won't have a parent.

        FunctionDef functionDef = this.functionDef.createCopy(false);
        functionDef.body = new stmtType[] { overrideBody != null ? overrideBody : new Pass() };

        try {
            MakeAstValidForPrettyPrintingVisitor.makeValid(functionDef);
        } catch (Exception e) {
            Log.log(e);
        }
        String printed = printAst(edit, functionDef, delimiter);
        PySelection ps = new PySelection(document, offset);
        try {
            String lineContentsToCursor = ps.getLineContentsToCursor();
            int defIndex = lineContentsToCursor.indexOf("def");
            int defOffset = ps.getLineOffset() + defIndex;
            printed = StringUtils.indentTo(printed, lineContentsToCursor.substring(0, defIndex), false);
            printed = StringUtils.rightTrim(printed);

            this.fLen += offset - defOffset;

            document.replace(defOffset, this.fLen, printed);
            return defOffset + printed.length();
        } catch (BadLocationException x) {
            // ignore
        }
        return -1;
    }

    public static String printAst(PyEdit edit, SimpleNode astToPrint, String lineDelimiter) {
        String str = null;
        if (astToPrint != null) {
            IIndentPrefs indentPrefs;
            if (edit != null) {
                indentPrefs = edit.getIndentPrefs();
            } else {
                indentPrefs = DefaultIndentPrefs.get(null);
            }

            PrettyPrinterPrefsV2 prefsV2 = PrettyPrinterV2.createDefaultPrefs(edit, indentPrefs, lineDelimiter);

            PrettyPrinterV2 prettyPrinterV2 = new PrettyPrinterV2(prefsV2);
            try {

                str = prettyPrinterV2.print(astToPrint);
            } catch (IOException e) {
                Log.log(e);
            }
        }
        return str;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.editor.codecompletion.AbstractPyCompletionProposalExtension2#getTriggerCharacters()
     */
    @Override
    public char[] getTriggerCharacters() {
        return null;
    }

    @Override
    public Point getSelection(IDocument document) {
        return new Point(fCursorPosition, fReplacementLength);
    }

}
