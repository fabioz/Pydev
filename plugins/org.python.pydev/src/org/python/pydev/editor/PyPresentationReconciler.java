package org.python.pydev.editor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class PyPresentationReconciler extends PresentationReconciler {

    private ITextViewer viewer;

    /**
     * Important: update only asynchronously...
     */
    public void invalidateTextPresentation() {
        if (viewer != null) {
            RunInUiThread.async(new Runnable() {

                public void run() {
                    ITextViewer v = viewer;
                    if (v != null && v instanceof SourceViewer) {
                        SourceViewer sourceViewer = (SourceViewer) v;
                        StyledText textWidget = sourceViewer.getTextWidget();
                        if (textWidget != null && !textWidget.isDisposed()) {
                            sourceViewer.invalidateTextPresentation();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void install(ITextViewer viewer) {
        super.install(viewer);
        this.viewer = viewer;
    }

    @Override
    public void uninstall() {
        super.uninstall();
        this.viewer = null;
    }
}
