/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;
import org.python.pydev.shared_ui.dialogs.DialogMemento;

/**
 * This is the class that shows the globals browser.
 * 
 * It shows 2 panels, one with the labels for a token and the second with the path to that token
 *
 * @author Fabio
 */
public class GlobalsTwoPaneElementSelector extends TwoPaneElementSelector {

    private DialogMemento memento;

    /**
     * Constructor
     */
    public GlobalsTwoPaneElementSelector(Shell parent) {
        super(parent, new NameIInfoLabelProvider(false), new ModuleIInfoLabelProvider());
        setTitle("PyDev: Globals Browser");
        memento = new DialogMemento(getShell(), "com.python.pydev.analysis.actions.GlobalsTwoPaneElementSelector");
    }

    @Override
    public boolean close() {
        memento.writeSettings(getShell());
        return super.close();
    }

    @Override
    public Control createDialogArea(Composite parent) {
        memento.readSettings();
        return super.createDialogArea(parent);
    }

    @Override
    protected Point getInitialSize() {
        return memento.getInitialSize(super.getInitialSize(), getShell());
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
    }

}
