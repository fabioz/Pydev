package org.python.pydev.refactoring.ui.controls.preview;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.ColorCache;

public class PyPreviewProjection extends ProjectionViewer {

	private ColorCache colorCache;

	private PyPreviewConfiguration editConfiguration;

	public PyPreviewProjection(Composite parent, IVerticalRuler ruler, IOverviewRuler overviewRuler, boolean showsAnnotationOverview,
			int styles) {
		super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);
	}

	@Override
	protected void createControl(Composite parent, int styles) {
		super.createControl(parent, styles);
		colorCache = new ColorCache(PydevPlugin.getChainedPrefStore());
		editConfiguration = new PyPreviewConfiguration(colorCache);
		configure(editConfiguration);
		getTextWidget().setEditable(false);
	}

	private boolean isInToggleCompletionStyle;

	public void setInToggleCompletionStyle(boolean b) {
		this.isInToggleCompletionStyle = b;
	}

	public boolean getIsInToggleCompletionStyle() {
		return this.isInToggleCompletionStyle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.source.projection.ProjectionViewer#doOperation(int)
	 */
	public void doOperation(int operation) {
		super.doOperation(operation);
		if (getTextWidget() == null)
			return;

	}

	public void revealUserSelection(ITextSelection selection) {
		setBackgroundColor(selection, SWT.COLOR_DARK_GRAY);
	}

	public void revealExtendedSelection(ITextSelection selection) {
		setBackgroundColor(selection, SWT.COLOR_GRAY);
		getTextWidget().setSelection(selection.getOffset());
	}

	private void setBackgroundColor(ITextSelection selection, int color) {
		setBackgroundColor(selection, Display.getCurrent().getSystemColor(color));
	}

	public void setBackgroundColor(ITextSelection selection, Color color) {
		StyleRange styleRangeNode = new StyleRange(selection.getOffset(), selection.getLength(), null, color);
		getTextWidget().setStyleRange(styleRangeNode);
	}
}
