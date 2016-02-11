package org.python.pydev.ui;

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
 *     Mark Leone <mark.leone@axiosengineering.com.> - shell size fix (line 81)
 *******************************************************************************/

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public abstract class EmulatedNativeCheckBoxLabelProvider extends
        ColumnLabelProvider {
    private static final String CHECKED_KEY = "CHECKED";
    private static final String UNCHECK_KEY = "UNCHECKED";

    public EmulatedNativeCheckBoxLabelProvider(ColumnViewer viewer) {
        if (JFaceResources.getImageRegistry().getDescriptor(CHECKED_KEY) == null) {
            JFaceResources.getImageRegistry().put(CHECKED_KEY, makeShot(viewer.getControl(), true));
            JFaceResources.getImageRegistry().put(UNCHECK_KEY, makeShot(viewer.getControl(), false));
        }
    }

    private Image makeShot(Control control, boolean type) {
        // Hopefully no platform uses exactly this color because we'll make
        // it transparent in the image.
        Color greenScreen = new Color(control.getDisplay(), 222, 223, 224);

        Shell shell = new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
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

        //In some cases, the image is not displayed unless the shell size is maximized for the GC
        GC gc = new GC(shell);
        Point shellSize = new Point(gc.getClipping().width, gc.getClipping().height);
        //but on some platforms the clipping is not set, so we use a reasonable fallback
        if (shellSize.x == 0) {
            shellSize = new Point(bsize.x + 3, bsize.y + 3);
        }
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