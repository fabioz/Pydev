/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.ui.console.IHyperlink;
import org.python.pydev.debug.ui.ILinkContainer;
import org.python.pydev.debug.ui.PythonConsoleLineTracker;
import org.python.pydev.shared_ui.tooltips.presenter.AbstractTooltipInformationPresenter;
import org.python.pydev.shared_ui.tooltips.presenter.StyleRangeWithCustomData;

public class InformationPresenterWithLineTracker extends AbstractTooltipInformationPresenter {

    @Override
    protected void onUpdatePresentation(final String hoverInfo, final TextPresentation presentation) {
        if (hoverInfo == null || hoverInfo.length() == 0) {
            return;
        }

        //now, after the line tracker, let's also add a bold to the first words (until a space), as that's the
        //name of the test.

        //(the first line is: TestName Status: status Time: time\n\n)
        //See: org.python.pydev.debug.pyunit.PyUnitView.notifyTest(PyUnitTestResult, boolean)
        int firstSpace = hoverInfo.indexOf(' ');
        if (firstSpace > 0) {
            StyleRangeWithCustomData range = new StyleRangeWithCustomData();
            range.fontStyle = SWT.BOLD;
            range.underline = true;
            try {
                range.underlineStyle = SWT.UNDERLINE_LINK;
            } catch (Throwable e) {
                //Ignore (not available on earlier versions of eclipse)
            }
            range.start = 0;
            range.length = firstSpace;
            if (this.data instanceof PyUnitTestResult) {
                final PyUnitTestResult pyUnitTestResult = (PyUnitTestResult) this.data;
                range.customData = new IHyperlink() {

                    @Override
                    public void linkExited() {
                    }

                    @Override
                    public void linkEntered() {
                    }

                    @Override
                    public void linkActivated() {
                        pyUnitTestResult.open();
                    }
                };
            }
            presentation.addStyleRange(range);
        }

        PythonConsoleLineTracker lineTracker = new PythonConsoleLineTracker();
        lineTracker.init(new ILinkContainer() {

            @Override
            public void addLink(IHyperlink link, int offset, int length) {
                StyleRangeWithCustomData range = new StyleRangeWithCustomData();
                range.underline = true;
                try {
                    range.underlineStyle = SWT.UNDERLINE_LINK;
                } catch (Throwable e) {
                    //Ignore (not available on earlier versions of eclipse)
                }

                //Set the proper color if it's available -- we don't do that here because our background is
                //the default (usually yellow), so, there's no point in changing it to the theme. An option
                //could be setting the theme for the popup, but we're not doing that now.
                //                TextAttribute textAttribute = ColorManager.getDefault().getHyperlinkTextAttribute();
                //                if (textAttribute != null) {
                //                    range.foreground = textAttribute.getForeground();
                //                }
                range.start = offset;
                range.length = length + 1;
                range.customData = link;
                presentation.addStyleRange(range);
            }

            @Override
            public String getContents(int lineOffset, int lineLength) throws BadLocationException {
                if (lineLength <= 0) {
                    return "";
                }
                return hoverInfo.substring(lineOffset, lineOffset + lineLength + 1);
            }
        });
        lineTracker.splitInLinesAndAppendToLineTracker(hoverInfo);
    }

    @Override
    protected void onHandleClick(Object data) {
        if (data instanceof IHyperlink) {
            //The order is important (when activating it'll do a hide automatically,
            //but we want to do a hide without focusing the previous editor).
            this.hideInformationControl(false, false);
            ((IHyperlink) data).linkActivated();
        }
    }

}
