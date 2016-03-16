/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_ui.utils.IViewWithControls;
import org.python.pydev.ui.IViewCreatedObserver;


/**
 * @author fabioz
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PyContextActivatorViewCreatedObserver implements IViewCreatedObserver {

    public static final class PyContextObserver implements IPyContextObserver, FocusListener, DisposeListener {

        private final CallbackWithListeners onStateChange = new CallbackWithListeners();
        private boolean active = false;
        private Widget currWidget = null;

        /*default*/PyContextObserver() {
            PyContextActivator.getSingleton().registerPyContextObserver(this);
        }

        @Override
        public boolean isPyContextActive() {
            return active;
        }

        @Override
        public CallbackWithListeners getOnStateChange() {
            return onStateChange;
        }

        @Override
        public void focusLost(FocusEvent e) {
            Widget widget = e.widget;
            stop(widget);
        }

        protected void stop(Widget widget) {
            if (widget == currWidget) {
                if (!currWidget.isDisposed()) {
                    currWidget.removeDisposeListener(this);
                }
                currWidget = null;
                changeState(false);
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (e.widget == currWidget) {
                return; //Nothing did really change...
            }

            if (currWidget != null) {
                if (!currWidget.isDisposed()) {
                    currWidget.removeDisposeListener(this);
                }
                currWidget = null;
            }

            Widget widget = e.widget;
            if (widget.isDisposed()) {
                Log.log("Gained focus on disposed widget?");
                stop(widget);
                return; //this can't be right...
            }

            widget.addDisposeListener(this);
            currWidget = widget;
            changeState(true);
        }

        protected void changeState(boolean newActiveState) {
            if (this.active != newActiveState) {
                this.active = newActiveState;
                onStateChange.call(null);
            }
        }

        @Override
        public void widgetDisposed(DisposeEvent e) {
            if (e.widget != currWidget) {
                Log.log("Heard disposed on non current widget?");
            }
            stop(currWidget);
        }

    }

    public static final PyContextObserver pyContextObserver = new PyContextObserver();

    private ICallbackListener onControlCreated;
    private ICallbackListener onControlDisposed;

    public PyContextActivatorViewCreatedObserver() {
        onControlCreated = new ICallbackListener() {

            @Override
            public Object call(Object obj) {
                if (obj instanceof TreeViewer) {
                    TreeViewer treeViewer = (TreeViewer) obj;
                    obj = treeViewer.getTree();
                }

                if (obj instanceof Control) {
                    Control control = (Control) obj;
                    control.addFocusListener(pyContextObserver);
                }
                return null;
            }
        };

        onControlDisposed = new ICallbackListener() {

            @Override
            public Object call(Object obj) {
                if (obj instanceof TreeViewer) {
                    TreeViewer treeViewer = (TreeViewer) obj;
                    obj = treeViewer.getTree();
                }

                if (obj instanceof Control) {
                    Control control = (Control) obj;
                    if (!control.isDisposed()) {
                        control.removeFocusListener(pyContextObserver);
                    }
                }
                return null;
            }
        };
    }

    @Override
    public void notifyViewCreated(IViewWithControls view) {
        view.getOnControlCreated().registerListener(this.onControlCreated);
        view.getOnControlDisposed().registerListener(this.onControlDisposed);
    }

}
