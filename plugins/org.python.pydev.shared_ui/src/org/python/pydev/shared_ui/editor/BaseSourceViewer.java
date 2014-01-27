package org.python.pydev.shared_ui.editor;

import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Composite;

public class BaseSourceViewer extends SourceViewer implements ITextViewerExtensionAutoEditions {

    private boolean autoEditionsEnabled = true;

    public BaseSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        super(parent, ruler, styles);
    }

    public BaseSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
            boolean showAnnotationsOverview, int styles) {
        super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
    }

    @Override
    public boolean getAutoEditionsEnabled() {
        return autoEditionsEnabled;
    }

    @Override
    public void setAutoEditionsEnabled(boolean b) {
        this.autoEditionsEnabled = b;
    }

}
