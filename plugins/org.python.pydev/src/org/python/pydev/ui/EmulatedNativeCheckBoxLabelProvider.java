/*******************************************************************************
 * Copyright (c) 2007 BestSolution Systemhaus GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of:
 * 1. The Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 2. LGPL v2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 3. MPL v1.1 which accompanies this distribution, and is available at
 * http://www.mozilla.org/MPL/MPL-1.1.html
 *
 * Contributors:
 *     Tom Schind <tom.schindl@bestsolution.at> - Initial API and implementation
 *     Mark Leone <mark.leone@axiosengineering.com.> - added workaround() method
 *******************************************************************************/
package org.python.pydev.ui;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Since SWT does not provide a checkbox renderer for Table cells (The
 * checkbox in the CheckBoxTableViewer applies to the entire table row),
 * we use this class to create an image of the native checkbox in checked
 * and unchecked states, and return that image for the table cell.
 *
 */
public abstract class EmulatedNativeCheckBoxLabelProvider extends
        ColumnLabelProvider {
    private static final String CHECKED_KEY = "CHECKED";
    private static final String UNCHECK_KEY = "UNCHECKED";

    private Shell shell = new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            SWT.NO_TRIM | SWT.NO_BACKGROUND);

    public EmulatedNativeCheckBoxLabelProvider(ColumnViewer viewer) {
        if (JFaceResources.getImageRegistry().getDescriptor(CHECKED_KEY) == null) {
            workaround(viewer.getControl().getDisplay());
            JFaceResources.getImageRegistry().put(CHECKED_KEY, makeShot(viewer.getControl(), true));
            JFaceResources.getImageRegistry().put(UNCHECK_KEY, makeShot(viewer.getControl(), false));
        }
    }

    private Image makeShot(Control control, boolean type) {
        /* Hopefully no platform uses exactly this color because we'll make
           it transparent in the image.*/
        Color greenScreen = new Color(control.getDisplay(), 222, 223, 224);

        shell = new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                SWT.NO_TRIM | SWT.NO_BACKGROUND);

        // otherwise we have a default gray color
        shell.setBackground(greenScreen);

        Button button = new Button(shell, SWT.CHECK | SWT.NO_BACKGROUND);
        button.setBackground(greenScreen);
        button.setSelection(type);

        // otherwise an image is located in a corner
        button.setLocation(1, 1);
        Point bsize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        // otherwise an image is stretched by width
        bsize.x = Math.max(bsize.x - 1, bsize.y - 1);
        bsize.y = Math.max(bsize.x - 1, bsize.y - 1);
        button.setSize(bsize);

        GC gc = new GC(shell);
        Point shellSize = new Point(32, 32);
        shell.setSize(shellSize);
        shell.open();

        Image image = new Image(control.getDisplay(), bsize.x, bsize.y);
        gc.copyArea(image, 0, 0);
        gc.dispose();
        shell.close();

        ImageData imageData = image.getImageData();
        imageData.transparentPixel = imageData.palette.getPixel(greenScreen
                .getRGB());

        Image img = new Image(control.getDisplay(), imageData);
        image.dispose();

        return img;
    }

    /* If we don't do this, the first time we call makeShot() will return
     * an image with a default background and no button. It's a mystery
     * why this workaround helps.
     */
    private void workaround(Display display) {
        shell.setSize(0, 0);
        shell.open();
        shell.close();
    }

    @Override
    public Image getImage(Object element) {
        if (isChecked(element)) {
            return JFaceResources.getImageRegistry().get(CHECKED_KEY);
        } else {
            return JFaceResources.getImageRegistry().get(UNCHECK_KEY);
        }
    }

    protected abstract boolean isChecked(Object element);
}