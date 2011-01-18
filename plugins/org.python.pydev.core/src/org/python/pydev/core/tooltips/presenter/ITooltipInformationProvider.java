package org.python.pydev.core.tooltips.presenter;

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
