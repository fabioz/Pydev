package org.python.pydev.debug.referrers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.IVariableLocator;
import org.python.pydev.debug.model.XMLUtils;
import org.python.pydev.debug.model.XMLUtils.XMLToReferrersInfo;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.debug.model.remote.RunCustomOperationCommand;
import org.python.pydev.debug.views.BaseDebugView;
import org.python.pydev.debug.views.ILaunchAndDebugListener;
import org.python.pydev.shared_ui.utils.UIUtils;

public class ReferrersView extends BaseDebugView {

    private static final String REFERRERS_VIEW_ID = "org.python.pydev.views.ReferrersView";

    /**
     * May only be called in the UI thread. If the view is not visible, shows it if the
     * preference to do that is set to true.
     * 
     * Note that it may return null if the preference to show it is false and the view is not currently shown.
     */
    public static ReferrersView getView(boolean forceVisible) {
        return (ReferrersView) UIUtils.getView(REFERRERS_VIEW_ID, forceVisible);
    }

    @Override
    protected ITreeContentProvider createContentProvider() {
        return new ReferrersViewContentProvider();
    }

    private static final class ReferrerCommandResponseListener implements ICommandResponseListener {
        private final IVariableLocator locator;
        private final AbstractDebugTarget debugTarget;
        private ReferrersView referrersView;

        private ReferrerCommandResponseListener(ReferrersView referrersView, IVariableLocator locator,
                AbstractDebugTarget debugTarget) {
            this.locator = locator;
            this.debugTarget = debugTarget;
            this.referrersView = referrersView;
        }

        @Override
        public void commandComplete(AbstractDebuggerCommand cmd) {
            try {
                if (cmd instanceof RunCustomOperationCommand) {
                    RunCustomOperationCommand c = (RunCustomOperationCommand) cmd;
                    String responsePayload = c.getResponsePayload();
                    if (responsePayload != null) {
                        XMLToReferrersInfo xmlToReferrers = XMLUtils.XMLToReferrers(debugTarget, locator,
                                responsePayload);
                        if (xmlToReferrers != null) {
                            referrersView.addReferrersInfo(xmlToReferrers);
                        }
                    } else {
                        Log.log("Command to get referrers did not return proper value.");
                    }
                }
            } finally {
                this.finish();
            }
        }

        private void finish() {
            boolean removedNow;
            synchronized (referrersView.listenersLock) {
                removedNow = referrersView.listeners.remove(this);
            }

            if (removedNow) {
                referrersView.endProgress();
            }
        }
    }

    @Override
    protected ILaunchAndDebugListener createListener() {
        return new ILaunchAndDebugListener() {

            @Override
            public void launchRemoved(ILaunch launch) {
                IDebugTarget debugTarget = launch.getDebugTarget();
                if (debugTarget instanceof AbstractDebugTarget) {
                    remove((AbstractDebugTarget) debugTarget);
                }
            }

            @Override
            public void launchChanged(ILaunch launch) {
                if (launch.isTerminated()) {
                    this.launchRemoved(launch);
                }
            }

            @Override
            public void launchAdded(ILaunch launch) {
            }

            @Override
            public void handleDebugEvents(DebugEvent[] events) {
                for (DebugEvent debugEvent : events) {
                    if (debugEvent.getSource() instanceof AbstractDebugTarget) {
                        if (debugEvent.getKind() == DebugEvent.TERMINATE) {
                            AbstractDebugTarget debugTarget = (AbstractDebugTarget) debugEvent.getSource();
                            remove(debugTarget);
                        }
                    }
                }

            }

            private void remove(AbstractDebugTarget debugTarget) {
                if (debugTarget.isTerminated()) {
                    synchronized (xmlToReferrersLock) {
                        Iterator<XMLToReferrersInfo> iterator = xmlToReferrers.iterator();
                        while (iterator.hasNext()) {
                            XMLToReferrersInfo next = iterator.next();
                            if (next.target == debugTarget) {
                                iterator.remove();
                            }
                        }
                    }
                    updateTreeJob.schedule();
                }
            }
        };
    }

    public ReferrersView() {
    }

    @Override
    protected void configureToolBar(IViewSite viewSite) {
        IActionBars actionBars = viewSite.getActionBars();
        IToolBarManager toolBar = actionBars.getToolBarManager();
        //IMenuManager menuManager = actionBars.getMenuManager(); -- not adding anything to the menu for now.

        toolBar.add(new ClearCurrentReferrers(this));

    }

    private final Set<ReferrerCommandResponseListener> listeners = new HashSet<>();
    protected final Object listenersLock = new Object();

    @Override
    public void clear() {
        super.clear();
        //Any registered pending command should be stopped now!
        synchronized (listenersLock) {
            for (ReferrerCommandResponseListener referrerCommandResponseListener : listeners) {
                referrerCommandResponseListener.finish();
            }
            listeners.clear();
            synchronized (xmlToReferrersLock) {
                this.xmlToReferrers.clear();
            }
        }
    }

    public void showReferrersFor(final AbstractDebugTarget debugTarget, final IVariableLocator locator) {
        RunCustomOperationCommand cmd = new RunCustomOperationCommand(debugTarget, locator,
                "from pydevd_referrers import get_referrer_info",
                "get_referrer_info");

        ReferrerCommandResponseListener listener = new ReferrerCommandResponseListener(this, locator, debugTarget);

        synchronized (listenersLock) {
            startProgress();
            listeners.add(listener);
        }
        cmd.setCompletionListener(listener);

        debugTarget.postCommand(cmd);
    }

    // Information to add to the tree and updating it ------------------------------------------------------------------

    protected final List<XMLToReferrersInfo> xmlToReferrers = new ArrayList<>();
    protected final Object xmlToReferrersLock = new Object();

    @Override
    protected void onSetTreeInput() {
        XMLToReferrersInfo[] array;
        int size = xmlToReferrers.size();
        synchronized (xmlToReferrersLock) {
            array = xmlToReferrers.toArray(new XMLToReferrersInfo[size]);
        }
        viewer.setInput(array);
    }

    protected void addReferrersInfo(XMLToReferrersInfo xmlToReferrers) {
        synchronized (xmlToReferrersLock) {
            this.xmlToReferrers.add(xmlToReferrers);
        }
        updateTreeJob.schedule();
    }

    @Override
    protected void makeLastVisibleInTree(Object input) {
        if (input instanceof XMLToReferrersInfo[]) {
            XMLToReferrersInfo[] xmlToReferrersInfos = (XMLToReferrersInfo[]) input;
            if (xmlToReferrersInfos.length > 0) {
                //i.e.: scroll to the last added element.
                XMLToReferrersInfo element = xmlToReferrersInfos[xmlToReferrersInfos.length - 1];
                if (element.forVar != null) {
                    viewer.reveal(element.forVar);
                }
            }
        }
    }

}
