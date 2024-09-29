package org.python.pydev.ast.codecompletion.revisited;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

public class TextViewerStub
        implements ITextViewer, ITextViewerExtension, ITextViewerExtension2, IPostSelectionProvider {
    private final IDocument doc;

    public TextViewerStub(IDocument doc) {
        this.doc = doc;
    }

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

    @Override
    public void invalidateTextPresentation(int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTextHover(ITextHover textViewerHover, String contentType, int stateMask) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeTextHovers(String contentType) {
        // TODO Auto-generated method stub

    }

    @Override
    public ITextHover getCurrentTextHover() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Point getHoverEventLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void prependAutoEditStrategy(IAutoEditStrategy strategy, String contentType) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAutoEditStrategy(IAutoEditStrategy strategy, String contentType) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addPainter(IPainter painter) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePainter(IPainter painter) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public ISelection getSelection() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSelection(ISelection selection) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void prependVerifyKeyListener(VerifyKeyListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendVerifyKeyListener(VerifyKeyListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeVerifyKeyListener(VerifyKeyListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public Control getControl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMark(int offset) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getMark() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRedraw(boolean redraw) {
        // TODO Auto-generated method stub

    }

    @Override
    public IRewriteTarget getRewriteTarget() {
        // TODO Auto-generated method stub
        return null;
    }
}
