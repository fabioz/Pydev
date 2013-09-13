/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.debug.ui.actions;

import org.eclipse.jface.action.IAction;

public class DjangoTestAction extends DjangoAction {

    public void run(IAction action) {
        launchDjangoCommand("test", false);
    }

}
