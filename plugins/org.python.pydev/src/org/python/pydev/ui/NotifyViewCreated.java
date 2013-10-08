/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui;

import java.util.List;

import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.utils.IViewWithControls;

public class NotifyViewCreated {

    @SuppressWarnings("unchecked")
    public static void notifyViewCreated(IViewWithControls view) {
        try {
            List<IViewCreatedObserver> participants = ExtensionHelper
                    .getParticipants(ExtensionHelper.PYDEV_VIEW_CREATED_OBSERVER);
            for (IViewCreatedObserver iViewCreatedObserver : participants) {
                try {
                    iViewCreatedObserver.notifyViewCreated(view);
                } catch (Throwable e) {
                    Log.log(e);
                }
            }
        } catch (Throwable e) {
            Log.log(e);
        }

    }

}
