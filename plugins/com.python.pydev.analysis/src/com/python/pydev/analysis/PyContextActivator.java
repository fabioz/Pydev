/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.callbacks.ListenerList;

/**
 * Activates the PyDev context.
 * 
 * @author fabioz
 */
@SuppressWarnings("rawtypes")
public class PyContextActivator implements ICallbackListener {

    private static PyContextActivator singleton;

    public synchronized static PyContextActivator getSingleton() {
        if (singleton == null) {
            singleton = new PyContextActivator();
        }
        return singleton;
    }

    private IContextActivation activateContext;
    private final ListenerList<IPyContextObserver> observers;

    private PyContextActivator() {
        observers = new ListenerList<IPyContextObserver>(IPyContextObserver.class);
    }

    /*default*/@SuppressWarnings("unchecked")
    void registerPyContextObserver(IPyContextObserver observer) {
        observer.getOnStateChange().registerListener(this);
        this.observers.add(observer);
    }

    private void handleStateChange() {
        boolean isActive = false;
        for (IPyContextObserver obs : this.observers.getListeners()) {
            if (obs.isPyContextActive()) {
                isActive = true;
                break;
            }
        }

        IContextService contextService = (IContextService) PlatformUI.getWorkbench().getService(IContextService.class);
        //May be null on shutdown on Eclipse 4. 
        if (contextService != null) {
            if (isActive) {
                if (activateContext == null) {
                    activateContext = contextService.activateContext("com.python.pydev.contexts.window");
                }
            } else {
                if (activateContext != null) {
                    contextService.deactivateContext(activateContext);
                }
                activateContext = null;
            }
        }
    }

    public Object call(Object obj) {
        //Context changed!
        handleStateChange();
        return null;
    }
}
