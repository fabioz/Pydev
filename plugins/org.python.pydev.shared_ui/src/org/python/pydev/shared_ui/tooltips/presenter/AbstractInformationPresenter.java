/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Base implementation for an information presenter.
 */
public abstract class AbstractInformationPresenter implements DefaultInformationControl.IInformationPresenter,
        DefaultInformationControl.IInformationPresenterExtension, IInformationPresenterAsTooltip {

    /**
     * The line delimiter that should be used in the tooltip.
     */
    public static final String LINE_DELIM = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    protected IInformationPresenterControlManager informationPresenterControlManager;
    protected Object data;

    /**
     * This method shouldn't really be used, but if it is, just forward it to the new method.
     */
    @Override
    public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth,
            int maxHeight) {
        return updatePresentation((Drawable) display, hoverInfo, presentation, maxWidth, maxHeight);
    }

    /**
     * The line delimiters must match the platform for the bolds to be correct, so, in this function we remove
     * the ones existing and add the ones dependent on the platform
     */
    protected String correctLineDelimiters(String str) {
        FastStringBuffer buf = new FastStringBuffer();
        for (String s : StringUtils.splitInLines(str)) {

            boolean found = false;
            while (s.endsWith("\r") || s.endsWith("\n")) {
                found = true;
                s = s.substring(0, s.length() - 1);
            }
            buf.append(s);
            if (found) {
                buf.append(LINE_DELIM);
            }
        }
        str = buf.toString();
        return str;
    }

    @Override
    public void setInformationPresenterControlManager(
            IInformationPresenterControlManager informationPresenterControlManager) {
        this.informationPresenterControlManager = informationPresenterControlManager;
    }

    public void hideInformationControl(boolean activateEditor, boolean restoreFocus) {
        if (this.informationPresenterControlManager != null) {
            this.informationPresenterControlManager.hideInformationControl(activateEditor, restoreFocus);
        }
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }

}
