package org.python.pydev.overview_ruler;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

public final class StyledTextWithoutVerticalBar extends StyledText {
    private boolean showScrollbar;

    public StyledTextWithoutVerticalBar(Composite parent, int style, boolean showScrollbar) {
        super(parent, style);
        this.showScrollbar = showScrollbar;
        if (!this.showScrollbar) {
            super.getVerticalBar().setVisible(false);
        }
    }

    public StyledTextWithoutVerticalBar(Composite parent, int styles) {
        this(parent, styles, MinimapOverviewRulerPreferencesPage.getShowScrollbar());
    }

    /**
     * Ok, this is a hack to workaround a bug in org.eclipse.jface.text.source.SourceViewer.RulerLayout.
     * The method getVerticalScrollArrowHeights returns wrong values if the vertical bar is hidden
     * (but properly uses 0,0 for the padding if we return null).
     */
    @Override
    public ScrollBar getVerticalBar() {
        if (showScrollbar) {
            return super.getVerticalBar();
        }
        return null;
    }
}