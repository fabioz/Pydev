/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.wizards.gettingstarted;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class GettingStartedPage extends AbstractNewProjectPage {

    private Label generalInfo;

    public GettingStartedPage(String pageName) {
        super(pageName);
    }

    @Override
    public void createControl(Composite parent) {
        generalInfo = new Label(parent, 0);
        generalInfo.setText("The first step in configuring Pydev is properly configuring your interpreter.\n"
                + "To do so, please bla, bla bla...");
    }

}
