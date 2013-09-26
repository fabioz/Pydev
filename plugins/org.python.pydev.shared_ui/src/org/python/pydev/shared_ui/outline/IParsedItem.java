package org.python.pydev.shared_ui.outline;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.model.ErrorDescription;

public interface IParsedItem {

    IParsedItem[] getChildren();

    /**
     * @return the begin line of the node. Note: 1-based (and not 0-based).
     * -1 means unable to get.
     */
    int getBeginLine();

    /**
     * If this item denotes an error, return the error description.
     */
    ErrorDescription getErrorDesc();

    Object getParent();

    Image getImage();

    void updateTo(IParsedItem newItem);

    boolean sameNodeType(IParsedItem newItem);

    void updateShallow(IParsedItem newItem);

    void setParent(IParsedItem parsedItem);

}
