package org.python.pydev.core.tooltips.presenter;

import org.eclipse.swt.widgets.Control;

public interface IInformationPresenterControlManager {

    void hideInformationControl();

    void install(Control control);

    void setInformationProvider(ITooltipInformationProvider provider);

    void showInformation();
}
