/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.dialogs;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.shared_ui.SharedUiPlugin;

/**
 * Recommended use:
 * 
 * As a field from a dialog. Override:
 * 
  
     public boolean close() {
         memento.writeSettings(getShell());
         return super.close();
     }
 
     public Control createDialogArea(Composite parent) {
         memento.readSettings();
         return super.createDialogArea(parent);
     }
 
    protected Point getInitialSize() {
        return memento.getInitialSize(super.getInitialSize(), getShell());
    }
 
     protected Point getInitialLocation(Point initialSize) {
         return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
    }
 
 *
 */
public class DialogMemento {

    private IDialogSettings fSettings;
    private Point fLocation;
    private Point fSize;

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";

    public DialogMemento(Shell parent, String dialogSettings) {
        IDialogSettings settings = SharedUiPlugin.getDefault().getDialogSettings();
        fSettings = settings.getSection(dialogSettings);
        if (fSettings == null) {
            fSettings = new DialogSettings(dialogSettings);
            settings.addSection(fSettings);
            fSettings.put(WIDTH, 480);
            fSettings.put(HEIGHT, 320);
        }
    }

    public Point getInitialSize(Point initialSize, Shell shell) {
        if (fSize != null) {
            initialSize.x = Math.max(initialSize.x, fSize.x);
            initialSize.y = Math.max(initialSize.y, fSize.y);
            Rectangle display = shell.getDisplay().getClientArea();
            initialSize.x = Math.min(initialSize.x, display.width);
            initialSize.y = Math.min(initialSize.y, display.height);
        }
        return initialSize;
    }

    public Point getInitialLocation(Point initialSize, Point initialLocation, Shell shell) {
        if (fLocation != null) {
            initialLocation.x = fLocation.x;
            initialLocation.y = fLocation.y;
            Rectangle display = shell.getDisplay().getClientArea();
            int xe = initialLocation.x + initialSize.x;
            if (xe > display.width) {
                initialLocation.x -= xe - display.width;
            }
            int ye = initialLocation.y + initialSize.y;
            if (ye > display.height) {
                initialLocation.y -= ye - display.height;
            }
        }
        return initialLocation;
    }

    /**
     * Initializes itself from the dialog settings with the same state
     * as at the previous invocation.
     */
    public void readSettings() {
        try {
            int x = fSettings.getInt("x");
            int y = fSettings.getInt("y");
            fLocation = new Point(x, y);
        } catch (NumberFormatException e) {
            fLocation = null;
        }
        try {
            int width = fSettings.getInt("width");
            int height = fSettings.getInt("height");
            fSize = new Point(width, height);

        } catch (NumberFormatException e) {
            fSize = null;
        }
    }

    /**
     * Stores it current configuration in the dialog store.
     */
    public void writeSettings(Shell shell) {
        Point location = shell.getLocation();
        fSettings.put("x", location.x);
        fSettings.put("y", location.y);

        Point size = shell.getSize();
        fSettings.put("width", size.x);
        fSettings.put("height", size.y);
    }
}
