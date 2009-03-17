package org.python.pydev.ui.wizards.gettingstarted;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class GettingStartedPage extends AbstractNewProjectPage{

    private Label generalInfo;

    public GettingStartedPage(String pageName) {
        super(pageName);
    }

    public void createControl(Composite parent) {
        generalInfo = new Label(parent, 0);
        generalInfo.setText(
                "The first step in configuring Pydev is properly configuring your interpreter.\n" +
                "To do so, please bla, bla bla...");
    }

}
