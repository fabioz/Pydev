package org.python.pydev.django.debug.ui.actions;
import org.eclipse.jface.action.IAction;

public class DjangoSyncDB extends DjangoAction {

    public void run(IAction action) {
    	launchDjangoCommand("syncdb", true);
    }

}
