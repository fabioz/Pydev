package org.python.pydev.django.debug.ui.actions;
import org.eclipse.jface.action.IAction;


public class DjangoDevServer extends DjangoAction {

    public void run(IAction action) {
    	launchDjangoCommand("runserver --noreload", false);
    }

}
