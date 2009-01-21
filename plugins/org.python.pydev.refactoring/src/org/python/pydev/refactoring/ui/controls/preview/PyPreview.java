package org.python.pydev.refactoring.ui.controls.preview;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;

public class PyPreview {

    private Composite parent;

    private IDocument doc;

    private PyPreviewProjection viewer;

    public PyPreview(Composite parent, IDocument doc) {
        super();
        this.parent = parent;
        this.doc = doc;
        createSourceViewer(parent, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    }

    protected void createSourceViewer(Composite parent, int styles) {
        viewer = new PyPreviewProjection(this.parent, null, null, false, styles);
        viewer.setDocument(doc);
        viewer.setRangeIndicator(new DefaultRangeIndicator());

        FormData textLData = new FormData(0, 0);
        textLData.right = new FormAttachment(1000, 1000, 0);
        textLData.left = new FormAttachment(0, 1000, 0);
        textLData.top = new FormAttachment(0, 1000, 0);
        textLData.bottom = new FormAttachment(1000, 1000, 0);
        viewer.getTextWidget().setLayoutData(textLData);
    }

    public Control getControl() {
        return this.viewer.getControl();
    }

    public void revealUserSelection(ITextSelection selection) {
        viewer.revealUserSelection(selection);
    }

    public void revealExtendedSelection(ITextSelection selection) {
        viewer.revealExtendedSelection(selection);
    }

}
