/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

/**
 * Interface for an information provider that'll show tooltips.
 */
public interface ITooltipInformationProvider {

    /**
     * @return The information to be passed to the information provider.
     */
    Object getInformation(Control fControl);

    /**
     * @return the position where the tooltip should be shown.
     */
    Point getPosition(Control fControl);

}
