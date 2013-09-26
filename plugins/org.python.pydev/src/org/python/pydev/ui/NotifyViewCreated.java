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
