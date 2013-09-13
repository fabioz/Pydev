/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

public interface IInformationPresenterAsTooltip {

    /**
     * Sets the control manager (used to hide it when needed)
     */
    void setInformationPresenterControlManager(IInformationPresenterControlManager informationPresenterControlManager);

    /**
     * Sets the data passed to the tooltip.
     */
    void setData(Object data);

}
