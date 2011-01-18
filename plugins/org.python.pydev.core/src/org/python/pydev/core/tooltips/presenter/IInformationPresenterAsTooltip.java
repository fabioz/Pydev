package org.python.pydev.core.tooltips.presenter;

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
