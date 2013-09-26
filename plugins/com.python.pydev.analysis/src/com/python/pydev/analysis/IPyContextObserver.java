/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.python.pydev.shared_core.callbacks.CallbackWithListeners;

/**
 * @author fabioz
 *
 */
public interface IPyContextObserver {

    /**
     * If any of the implementors of this interface return true here, it means that the PyDev context is active.
     */
    public boolean isPyContextActive();

    @SuppressWarnings("rawtypes")
    public CallbackWithListeners getOnStateChange();

}
