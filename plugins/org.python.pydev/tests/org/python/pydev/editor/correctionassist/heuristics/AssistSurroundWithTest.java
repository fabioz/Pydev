/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.ast.surround_with.AssistSurroundWith;
import org.python.pydev.core.TestCaseUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.codecompletion.proposals.DefaultCompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class AssistSurroundWithTest extends TestCase {

    public static void main(String[] args) {
        try {
            AssistSurroundWithTest builtins = new AssistSurroundWithTest();
            builtins.setUp();
            builtins.testSurround();
            builtins.tearDown();

            junit.textui.TestRunner.run(AssistSurroundWithTest.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompletionProposalFactory.set(new DefaultCompletionProposalFactory());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CompletionProposalFactory.set(null);
    }

    public void testSurround2() throws Exception {
        AssistSurroundWith assistSurroundWith = new AssistSurroundWith();
        IDocument doc = new Document("" +
                "def m1():\n" +
                "\n" +
                "#c\n" +
                "    a = 10\n" +
                "\n" +
                "\n");
        PySelection ps = new PySelection(doc, 1, 0, 13);
        int offset = ps.getAbsoluteCursorOffset();
        List<ICompletionProposalHandle> props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        apply(props.get(0), doc);
        TestCaseUtils.assertContentsEqual("" +
                "def m1():\n" +
                "    try:\n" +
                "    \n" +
                "    #c\n" +
                "        a = 10\n"
                +
                "    except:\n" +
                "        raise\n" +
                "\n" +
                "\n" +
                "", doc.get());
    }

    private void apply(ICompletionProposalHandle iCompletionProposalHandle, IDocument doc) {
        TemplateProposal p = (TemplateProposal) iCompletionProposalHandle;
        p.apply(createViewerWithDoc(doc), ' ', 0, 0);
    }

    public void testSurround3() throws Exception {
        AssistSurroundWith assistSurroundWith = new AssistSurroundWith();
        IDocument doc = new Document("" +
                "def m1():\n" +
                "\n" +
                "#c\n" +
                "#    a = 10\n" +
                "\n" +
                "\n");
        PySelection ps = new PySelection(doc, 1, 0, 14);
        int offset = ps.getAbsoluteCursorOffset();
        List<ICompletionProposalHandle> props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        apply(props.get(0), doc);
        TestCaseUtils.assertContentsEqual("" +
                "def m1():\n" +
                "try:\n" +
                "    \n" +
                "    #c\n" +
                "    #    a = 10\n"
                +
                "except:\n" +
                "    raise\n" +
                "\n" +
                "\n" +
                "", doc.get());
    }

    public void testSurround() throws Exception {
        AssistSurroundWith assistSurroundWith = new AssistSurroundWith();
        int offset = 0;
        IDocument doc = new Document("a = 10");
        PySelection ps = new PySelection(doc, 0, 0, 3);
        List<ICompletionProposalHandle> props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        apply(props.get(0), doc);
        TestCaseUtils.assertContentsEqual("" +
                "try:\n" +
                "    a = 10\n" +
                "except:\n" +
                "    raise" +
                "",
                doc.get());

        doc = new Document("" +
                "def m1():\n" +
                "\n" +
                "\n" +
                "    a = 10\n" +
                "\n" +
                "\n");
        ps = new PySelection(doc, 1, 0, 11);
        props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        String additionalProposalInfo = props.get(0).getAdditionalProposalInfo();
        TestCaseUtils.assertContentsEqual(
                "    try:\n"
                        + "    \n"
                        + "    \n"
                        + "        a = 10\n"
                        + "    except:\n"
                        + "        raise",
                additionalProposalInfo);

        apply(props.get(0), doc);

        TestCaseUtils.assertContentsEqual("" +
                "def m1():\n" +
                "    try:\n" +
                "    \n" +
                "    \n" +
                "        a = 10\n"
                +
                "    except:\n" +
                "        raise\n" +
                "\n" +
                "\n" +
                "", doc.get());

        doc = new Document("" +
                "\n" +
                "\n" +
                "\n");
        ps = new PySelection(doc, 1, 0, 1);
        props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        assertEquals(0, props.size());
    }

    private ITextViewer createViewerWithDoc(IDocument doc) {
        // TODO Auto-generated method stub
        return new ITextViewer() {

            @Override
            public void setVisibleRegion(int offset, int length) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setUndoManager(IUndoManager undoManager) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setTopIndex(int index) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setTextHover(ITextHover textViewerHover, String contentType) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy, String contentType) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setTextColor(Color color, int offset, int length, boolean controlRedraw) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setTextColor(Color color) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setSelectedRange(int offset, int length) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setEventConsumer(IEventConsumer consumer) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setEditable(boolean editable) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setDocument(IDocument document, int modelRangeOffset, int modelRangeLength) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setDocument(IDocument document) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setAutoIndentStrategy(IAutoIndentStrategy strategy, String contentType) {
                // TODO Auto-generated method stub

            }

            @Override
            public void revealRange(int offset, int length) {
                // TODO Auto-generated method stub

            }

            @Override
            public void resetVisibleRegion() {
                // TODO Auto-generated method stub

            }

            @Override
            public void resetPlugins() {
                // TODO Auto-generated method stub

            }

            @Override
            public void removeViewportListener(IViewportListener listener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void removeTextListener(ITextListener listener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void removeTextInputListener(ITextInputListener listener) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean overlapsWithVisibleRegion(int offset, int length) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isEditable() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void invalidateTextPresentation() {
                // TODO Auto-generated method stub

            }

            @Override
            public IRegion getVisibleRegion() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getTopInset() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getTopIndexStartOffset() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getTopIndex() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public StyledText getTextWidget() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ITextOperationTarget getTextOperationTarget() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ISelectionProvider getSelectionProvider() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Point getSelectedRange() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public IFindReplaceTarget getFindReplaceTarget() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public IDocument getDocument() {
                // TODO Auto-generated method stub
                return doc;
            }

            @Override
            public int getBottomIndexEndOffset() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getBottomIndex() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void changeTextPresentation(TextPresentation presentation, boolean controlRedraw) {
                // TODO Auto-generated method stub

            }

            @Override
            public void addViewportListener(IViewportListener listener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void addTextListener(ITextListener listener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void addTextInputListener(ITextInputListener listener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void activatePlugins() {
                // TODO Auto-generated method stub

            }
        };
    }
}
