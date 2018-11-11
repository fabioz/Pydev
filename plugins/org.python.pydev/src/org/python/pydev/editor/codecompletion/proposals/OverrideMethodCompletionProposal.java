/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.proposals;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPySourceViewer;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * @author fabioz
 *
 */
public class OverrideMethodCompletionProposal extends AbstractPyCompletionProposalExtension2 {

    private final FunctionDef functionDef;
    private final String parentClassName;
    private String currentClassName;

    public OverrideMethodCompletionProposal(int replacementOffset, int replacementLength, int cursorPosition,
            IImageHandle image, ISimpleNode functionDef, String parentClassName, String currentClassName) {
        super("", replacementOffset, replacementLength, cursorPosition, IPyCompletionProposal.PRIORITY_CREATE, null);
        this.fImage = image;
        this.functionDef = (FunctionDef) functionDef;
        this.fDisplayString = ((NameTok) this.functionDef.name).id + " (Override method in " + parentClassName + ")";
        this.parentClassName = parentClassName;
        this.currentClassName = currentClassName;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.ast.codecompletion.PyCompletionProposal#apply(org.eclipse.jface.text.IDocument)
     */
    @Override
    public void apply(IDocument document) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
     */
    @Override
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
        IPyEdit edit = null;
        if (viewer instanceof IPySourceViewer) {
            IPySourceViewer pySourceViewer = (IPySourceViewer) viewer;
            versionProvider = edit = pySourceViewer.getEdit();
        } else {
            versionProvider = new IGrammarVersionProvider() {

                @Override
                public int getGrammarVersion() throws MisconfigurationException {
                    return IGrammarVersionProvider.LATEST_GRAMMAR_PY3_VERSION;
                }

                @Override
                public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions()
                        throws MisconfigurationException {
                    return null;
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

        IIndentPrefs indentPrefs;
        if (edit != null) {
            indentPrefs = edit.getIndentPrefs();
        } else {
            indentPrefs = DefaultIndentPrefs.get(null);
        }
        String printed = NodeUtils.printAst(indentPrefs, edit, functionDef, delimiter);
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

    /* (non-Javadoc)
     * @see org.python.pydev.ast.codecompletion.AbstractPyCompletionProposalExtension2#getTriggerCharacters()
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
