package org.python.pydev.debug.referrers;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;

public class ClearCurrentReferrers extends Action {

    private WeakReference<ReferrersView> referrersView;

    public ClearCurrentReferrers(ReferrersView referrersView) {
        this.referrersView = new WeakReference<ReferrersView>(referrersView);
        this.setImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(UIConstants.REMOVE_ALL));
        this.setToolTipText("Clears the current referrers in the view.");
    }

    @Override
    public void run() {
        referrersView.get().clear();
    }
}
