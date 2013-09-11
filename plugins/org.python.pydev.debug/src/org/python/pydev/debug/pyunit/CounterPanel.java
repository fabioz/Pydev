/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * A panel with counters for the number of Runs, Errors and Failures.
 * 
 * Copied from org.eclipse.jdt.internal.junit.ui.CounterPanel (but without totals)
 */
public class CounterPanel extends Composite {
    public final Text fNumberOfErrors;
    public final Text fNumberOfFailures;
    public final Text fNumberOfRuns;

    private final Image fErrorIcon;
    private final Image fFailureIcon;

    public CounterPanel(Composite parent) {
        super(parent, SWT.WRAP);
        //Note: don't dispose of icons in the cache
        fErrorIcon = PydevDebugPlugin.getImageCache().get("icons/ovr16/error_ovr.gif"); //$NON-NLS-1$
        fFailureIcon = PydevDebugPlugin.getImageCache().get("icons/ovr16/failed_ovr.gif"); //$NON-NLS-1$

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 6;
        gridLayout.makeColumnsEqualWidth = false;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        fNumberOfRuns = createLabel("Runs: ", null, "0/0", "Test Run/Tests Collected"); //$NON-NLS-1$
        fNumberOfErrors = createLabel("", fErrorIcon, "0", "Errors"); //$NON-NLS-1$
        fNumberOfFailures = createLabel("", fFailureIcon, "0", "Failures"); //$NON-NLS-1$
    }

    private Text createLabel(String name, Image image, String init, String tooltip) {
        Label label;

        if (image != null) {
            label = new Label(this, SWT.NONE);
            if (image != null) {
                image.setBackground(label.getBackground());
                label.setImage(image);
            }
            label.setToolTipText(tooltip);
            GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gridData.minimumWidth = 14;
            label.setLayoutData(gridData);
        }

        if (name.length() > 0) {
            label = new Label(this, SWT.NONE);
            label.setText(name);
            GridData data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            data.minimumWidth = 20;
            label.setLayoutData(data);
        }
        //label.setFont(JFaceResources.getBannerFont());

        Text value = new Text(this, SWT.READ_ONLY);
        value.setToolTipText(tooltip);
        value.setText(init);
        // bug: 39661 Junit test counters do not repaint correctly [JUnit]
        value.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.minimumWidth = 25;
        value.setLayoutData(data);
        return value;
    }

    public void reset() {
        setErrorValue(0);
        setFailureValue(0);
        setRunValue(0, "0");
    }

    public void setRunValue(int value, String total) {
        String runString = Integer.toString(value) + " / " + total;
        fNumberOfRuns.setText(runString);

        fNumberOfRuns.redraw();
        redraw();
    }

    public void setErrorValue(int value) {
        fNumberOfErrors.setText(Integer.toString(value));
        redraw();
    }

    public void setFailureValue(int value) {
        fNumberOfFailures.setText(Integer.toString(value));
        redraw();
    }
}
