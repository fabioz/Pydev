/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.python.pydev.core.callbacks.CallbackWithListeners;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.ui.IViewCreatedObserver;
import org.python.pydev.ui.IViewWithControls;

/**
 * @author fabioz
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PyContextActivatorViewCreatedObserver implements IViewCreatedObserver {

    public static final class PyContextObserver implements IPyContextObserver, FocusListener {

        private final CallbackWithListeners onStateChange = new CallbackWithListeners();
        private boolean active = false;

        /*default*/PyContextObserver() {
            PyContextActivator.getSingleton().registerPyContextObserver(this);
        }

        public boolean isPyContextActive() {
            return active;
        }

        public CallbackWithListeners getOnStateChange() {
            return onStateChange;
        }

        public void focusLost(FocusEvent e) {
            this.active = false;
            onStateChange.call(null);
        }

        public void focusGained(FocusEvent e) {
            this.active = true;
            onStateChange.call(null);
        }

    }

    public static final PyContextObserver pyContextObserver = new PyContextObserver();

    private ICallbackListener onControlCreated;
    private ICallbackListener onControlDisposed;

    public PyContextActivatorViewCreatedObserver() {
        onControlCreated = new ICallbackListener() {

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

            public Object call(Object obj) {
                if (obj instanceof TreeViewer) {
                    TreeViewer treeViewer = (TreeViewer) obj;
                    obj = treeViewer.getTree();
                }

                if (obj instanceof Control) {
                    Control control = (Control) obj;
                    if(!control.isDisposed()){
                        control.removeFocusListener(pyContextObserver);
                    }
                }
                return null;
            }
        };
    }

    public void notifyViewCreated(IViewWithControls view) {
        view.getOnControlCreated().registerListener(this.onControlCreated);
        view.getOnControlDisposed().registerListener(this.onControlDisposed);
    }

}
