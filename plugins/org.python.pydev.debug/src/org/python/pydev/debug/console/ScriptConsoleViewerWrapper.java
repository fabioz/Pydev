/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension8.EnrichMode;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextPrintOptions;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.TextConsoleViewer;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;

public class ScriptConsoleViewerWrapper implements ITextViewer, IScriptConsoleViewer {

    private TextConsoleViewer viewer;
    private IInterpreterInfo info;

    public ScriptConsoleViewerWrapper(TextConsoleViewer viewer, IInterpreterInfo info) {
        this.viewer = viewer;
        this.info = info;
    }

    @Override
    public IInterpreterInfo getInterpreterInfo() {
        return info;
    }

    @Override
    public String getCommandLine() {

        IDocument document = this.viewer.getDocument();
        ITextSelection selection = (ITextSelection) this.viewer.getSelection();
        PySelection ps = new PySelection(document, selection);
        return ps.getCursorLineContents();
    }

    @Override
    public int getCommandLineOffset() {
        IDocument document = this.viewer.getDocument();
        ITextSelection selection = (ITextSelection) this.viewer.getSelection();
        PySelection ps = new PySelection(document, selection);
        return ps.getStartLineOffset();
    }

    @Override
    public int getCaretOffset() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setCaretOffset(int offset, boolean async) {
        throw new RuntimeException("Not implemented");
    }

    // Delegates

    @Override
    public int hashCode() {
        return viewer.hashCode();
    }

    public void addHelpListener(HelpListener listener) {
        viewer.addHelpListener(listener);
    }

    @Override
    public boolean equals(Object obj) {
        return viewer.equals(obj);
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        viewer.addSelectionChangedListener(listener);
    }

    public Object getData(String key) {
        return viewer.getData(key);
    }

    public void setTabWidth(int tabWidth) {
        viewer.setTabWidth(tabWidth);
    }

    public void setFont(Font font) {
        viewer.setFont(font);
    }

    public void lineGetStyle(LineStyleEvent event) {
        viewer.lineGetStyle(event);
    }

    public void removeHelpListener(HelpListener listener) {
        viewer.removeHelpListener(listener);
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        viewer.removeSelectionChangedListener(listener);
    }

    public Item scrollDown(int x, int y) {
        return viewer.scrollDown(x, y);
    }

    @Override
    public String toString() {
        return viewer.toString();
    }

    public Item scrollUp(int x, int y) {
        return viewer.scrollUp(x, y);
    }

    public void setData(String key, Object value) {
        viewer.setData(key, value);
    }

    public void setSelection(ISelection selection) {
        viewer.setSelection(selection);
    }

    public void lineGetBackground(LineBackgroundEvent event) {
        viewer.lineGetBackground(event);
    }

    public void mouseEnter(MouseEvent e) {
        viewer.mouseEnter(e);
    }

    public void mouseExit(MouseEvent e) {
        viewer.mouseExit(e);
    }

    public void mouseHover(MouseEvent e) {
        viewer.mouseHover(e);
    }

    public void mouseMove(MouseEvent e) {
        viewer.mouseMove(e);
    }

    public Control getControl() {
        return viewer.getControl();
    }

    public void setAnnotationHover(IAnnotationHover annotationHover) {
        viewer.setAnnotationHover(annotationHover);
    }

    public void setOverviewRulerAnnotationHover(IAnnotationHover annotationHover) {
        viewer.setOverviewRulerAnnotationHover(annotationHover);
    }

    public void configure(SourceViewerConfiguration configuration) {
        viewer.configure(configuration);
    }

    public IHyperlink getHyperlink() {
        return viewer.getHyperlink();
    }

    public IHyperlink getHyperlink(int offset) {
        return viewer.getHyperlink(offset);
    }

    public void mouseDoubleClick(MouseEvent e) {
        viewer.mouseDoubleClick(e);
    }

    public void mouseDown(MouseEvent e) {
        viewer.mouseDown(e);
    }

    public void mouseUp(MouseEvent e) {
        viewer.mouseUp(e);
    }

    public void setConsoleWidth(int width) {
        viewer.setConsoleWidth(width);
    }

    public void setHoverEnrichMode(EnrichMode mode) {
        viewer.setHoverEnrichMode(mode);
    }

    @Override
    public void activatePlugins() {
        viewer.activatePlugins();
    }

    @Override
    public void setDocument(IDocument document) {
        viewer.setDocument(document);
    }

    @Override
    public void setDocument(IDocument document, int visibleRegionOffset, int visibleRegionLength) {
        viewer.setDocument(document, visibleRegionOffset, visibleRegionLength);
    }

    public void setDocument(IDocument document, IAnnotationModel annotationModel) {
        viewer.setDocument(document, annotationModel);
    }

    public void setDocument(IDocument document, IAnnotationModel annotationModel, int modelRangeOffset,
            int modelRangeLength) {
        viewer.setDocument(document, annotationModel, modelRangeOffset, modelRangeLength);
    }

    public IAnnotationModel getAnnotationModel() {
        return viewer.getAnnotationModel();
    }

    public IQuickAssistAssistant getQuickAssistAssistant() {
        return viewer.getQuickAssistAssistant();
    }

    public final ContentAssistantFacade getContentAssistantFacade() {
        return viewer.getContentAssistantFacade();
    }

    public IQuickAssistInvocationContext getQuickAssistInvocationContext() {
        return viewer.getQuickAssistInvocationContext();
    }

    public IAnnotationModel getVisualAnnotationModel() {
        return viewer.getVisualAnnotationModel();
    }

    public void unconfigure() {
        viewer.unconfigure();
    }

    public boolean canDoOperation(int operation) {
        return viewer.canDoOperation(operation);
    }

    public void doOperation(int operation) {
        viewer.doOperation(operation);
    }

    public void enableOperation(int operation, boolean enable) {
        viewer.enableOperation(operation, enable);
    }

    public void setRangeIndicator(Annotation rangeIndicator) {
        viewer.setRangeIndicator(rangeIndicator);
    }

    public void setRangeIndication(int start, int length, boolean moveCursor) {
        viewer.setRangeIndication(start, length, moveCursor);
    }

    public IRegion getRangeIndication() {
        return viewer.getRangeIndication();
    }

    public void removeRangeIndication() {
        viewer.removeRangeIndication();
    }

    public void showAnnotations(boolean show) {
        viewer.showAnnotations(show);
    }

    public void showAnnotationsOverview(boolean show) {
        viewer.showAnnotationsOverview(show);
    }

    public IAnnotationHover getCurrentAnnotationHover() {
        return viewer.getCurrentAnnotationHover();
    }

    @Override
    public void resetPlugins() {
        viewer.resetPlugins();
    }

    @Override
    public StyledText getTextWidget() {
        return viewer.getTextWidget();
    }

    @Override
    public void setAutoIndentStrategy(IAutoIndentStrategy strategy, String contentType) {
        viewer.setAutoIndentStrategy(strategy, contentType);
    }

    public void prependAutoEditStrategy(IAutoEditStrategy strategy, String contentType) {
        viewer.prependAutoEditStrategy(strategy, contentType);
    }

    public void removeAutoEditStrategy(IAutoEditStrategy strategy, String contentType) {
        viewer.removeAutoEditStrategy(strategy, contentType);
    }

    @Override
    public void setEventConsumer(IEventConsumer consumer) {
        viewer.setEventConsumer(consumer);
    }

    @Override
    public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
        viewer.setIndentPrefixes(indentPrefixes, contentType);
    }

    @Override
    public int getTopInset() {
        return viewer.getTopInset();
    }

    @Override
    public boolean isEditable() {
        return viewer.isEditable();
    }

    @Override
    public void setEditable(boolean editable) {
        viewer.setEditable(editable);
    }

    @Override
    public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
        viewer.setDefaultPrefixes(defaultPrefixes, contentType);
    }

    @Override
    public void setUndoManager(IUndoManager undoManager) {
        viewer.setUndoManager(undoManager);
    }

    public IUndoManager getUndoManager() {
        return viewer.getUndoManager();
    }

    @Override
    public void setTextHover(ITextHover hover, String contentType) {
        viewer.setTextHover(hover, contentType);
    }

    public void setTextHover(ITextHover hover, String contentType, int stateMask) {
        viewer.setTextHover(hover, contentType, stateMask);
    }

    public void removeTextHovers(String contentType) {
        viewer.removeTextHovers(contentType);
    }

    public void setHoverControlCreator(IInformationControlCreator creator) {
        viewer.setHoverControlCreator(creator);
    }

    public boolean requestWidgetToken(IWidgetTokenKeeper requester) {
        return viewer.requestWidgetToken(requester);
    }

    public boolean requestWidgetToken(IWidgetTokenKeeper requester, int priority) {
        return viewer.requestWidgetToken(requester, priority);
    }

    public void releaseWidgetToken(IWidgetTokenKeeper tokenKeeper) {
        viewer.releaseWidgetToken(tokenKeeper);
    }

    @Override
    public Point getSelectedRange() {
        return viewer.getSelectedRange();
    }

    @Override
    public void setSelectedRange(int selectionOffset, int selectionLength) {
        viewer.setSelectedRange(selectionOffset, selectionLength);
    }

    public void setSelection(ISelection selection, boolean reveal) {
        viewer.setSelection(selection, reveal);
    }

    public ISelection getSelection() {
        return viewer.getSelection();
    }

    @Override
    public ISelectionProvider getSelectionProvider() {
        return viewer.getSelectionProvider();
    }

    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
        viewer.addPostSelectionChangedListener(listener);
    }

    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
        viewer.removePostSelectionChangedListener(listener);
    }

    @Override
    public void addTextListener(ITextListener listener) {
        viewer.addTextListener(listener);
    }

    @Override
    public void removeTextListener(ITextListener listener) {
        viewer.removeTextListener(listener);
    }

    @Override
    public void addTextInputListener(ITextInputListener listener) {
        viewer.addTextInputListener(listener);
    }

    @Override
    public void removeTextInputListener(ITextInputListener listener) {
        viewer.removeTextInputListener(listener);
    }

    public Object getInput() {
        return viewer.getInput();
    }

    @Override
    public IDocument getDocument() {
        return viewer.getDocument();
    }

    public void setInput(Object input) {
        viewer.setInput(input);
    }

    @Override
    public void addViewportListener(IViewportListener listener) {
        viewer.addViewportListener(listener);
    }

    @Override
    public void removeViewportListener(IViewportListener listener) {
        viewer.removeViewportListener(listener);
    }

    @Override
    public int getTopIndex() {
        return viewer.getTopIndex();
    }

    @Override
    public void setTopIndex(int index) {
        viewer.setTopIndex(index);
    }

    @Override
    public int getBottomIndex() {
        return viewer.getBottomIndex();
    }

    @Override
    public int getTopIndexStartOffset() {
        return viewer.getTopIndexStartOffset();
    }

    @Override
    public int getBottomIndexEndOffset() {
        return viewer.getBottomIndexEndOffset();
    }

    @Override
    public void revealRange(int start, int length) {
        viewer.revealRange(start, length);
    }

    public void refresh() {
        viewer.refresh();
    }

    @Override
    public final void invalidateTextPresentation() {
        viewer.invalidateTextPresentation();
    }

    public final void invalidateTextPresentation(int offset, int length) {
        viewer.invalidateTextPresentation(offset, length);
    }

    @Override
    public IRegion getVisibleRegion() {
        return viewer.getVisibleRegion();
    }

    @Override
    public boolean overlapsWithVisibleRegion(int start, int length) {
        return viewer.overlapsWithVisibleRegion(start, length);
    }

    @Override
    public void setVisibleRegion(int start, int length) {
        viewer.setVisibleRegion(start, length);
    }

    @Override
    public void resetVisibleRegion() {
        viewer.resetVisibleRegion();
    }

    @Override
    public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy, String contentType) {
        viewer.setTextDoubleClickStrategy(strategy, contentType);
    }

    public void print(StyledTextPrintOptions options) {
        viewer.print(options);
    }

    @Override
    public void setTextColor(Color color) {
        viewer.setTextColor(color);
    }

    @Override
    public void setTextColor(Color color, int start, int length, boolean controlRedraw) {
        viewer.setTextColor(color, start, length, controlRedraw);
    }

    @Override
    public void changeTextPresentation(TextPresentation presentation, boolean controlRedraw) {
        viewer.changeTextPresentation(presentation, controlRedraw);
    }

    @Override
    public IFindReplaceTarget getFindReplaceTarget() {
        return viewer.getFindReplaceTarget();
    }

    @Override
    public ITextOperationTarget getTextOperationTarget() {
        return viewer.getTextOperationTarget();
    }

    public void appendVerifyKeyListener(VerifyKeyListener listener) {
        viewer.appendVerifyKeyListener(listener);
    }

    public void prependVerifyKeyListener(VerifyKeyListener listener) {
        viewer.prependVerifyKeyListener(listener);
    }

    public void removeVerifyKeyListener(VerifyKeyListener listener) {
        viewer.removeVerifyKeyListener(listener);
    }

    public int getMark() {
        return viewer.getMark();
    }

    public void setMark(int offset) {
        viewer.setMark(offset);
    }

    public final void setRedraw(boolean redraw) {
        viewer.setRedraw(redraw);
    }

    public IRewriteTarget getRewriteTarget() {
        return viewer.getRewriteTarget();
    }

    public ITextHover getCurrentTextHover() {
        return viewer.getCurrentTextHover();
    }

    public Point getHoverEventLocation() {
        return viewer.getHoverEventLocation();
    }

    public void addPainter(IPainter painter) {
        viewer.addPainter(painter);
    }

    public void removePainter(IPainter painter) {
        viewer.removePainter(painter);
    }

    public int modelLine2WidgetLine(int modelLine) {
        return viewer.modelLine2WidgetLine(modelLine);
    }

    public int modelOffset2WidgetOffset(int modelOffset) {
        return viewer.modelOffset2WidgetOffset(modelOffset);
    }

    public IRegion modelRange2WidgetRange(IRegion modelRange) {
        return viewer.modelRange2WidgetRange(modelRange);
    }

    public int widgetlLine2ModelLine(int widgetLine) {
        return viewer.widgetlLine2ModelLine(widgetLine);
    }

    public int widgetLine2ModelLine(int widgetLine) {
        return viewer.widgetLine2ModelLine(widgetLine);
    }

    public int widgetOffset2ModelOffset(int widgetOffset) {
        return viewer.widgetOffset2ModelOffset(widgetOffset);
    }

    public IRegion widgetRange2ModelRange(IRegion widgetRange) {
        return viewer.widgetRange2ModelRange(widgetRange);
    }

    public IRegion getModelCoverage() {
        return viewer.getModelCoverage();
    }

    public int widgetLineOfWidgetOffset(int widgetOffset) {
        return viewer.widgetLineOfWidgetOffset(widgetOffset);
    }

    public boolean moveFocusToWidgetToken() {
        return viewer.moveFocusToWidgetToken();
    }

    public void setDocumentPartitioning(String partitioning) {
        viewer.setDocumentPartitioning(partitioning);
    }

    public void addTextPresentationListener(ITextPresentationListener listener) {
        viewer.addTextPresentationListener(listener);
    }

    public void removeTextPresentationListener(ITextPresentationListener listener) {
        viewer.removeTextPresentationListener(listener);
    }

    public void register(IEditingSupport helper) {
        viewer.register(helper);
    }

    public void unregister(IEditingSupport helper) {
        viewer.unregister(helper);
    }

    public IEditingSupport[] getRegisteredSupports() {
        return viewer.getRegisteredSupports();
    }

    public void setHyperlinkDetectors(IHyperlinkDetector[] hyperlinkDetectors, int eventStateMask) {
        viewer.setHyperlinkDetectors(hyperlinkDetectors, eventStateMask);
    }

    public void setHyperlinkPresenter(IHyperlinkPresenter hyperlinkPresenter) throws IllegalStateException {
        viewer.setHyperlinkPresenter(hyperlinkPresenter);
    }

    public void setTabsToSpacesConverter(IAutoEditStrategy converter) {
        viewer.setTabsToSpacesConverter(converter);
    }

}
