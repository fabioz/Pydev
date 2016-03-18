/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Point;

/**
 * Information presenter for tooltips.
 */
public abstract class AbstractTooltipInformationPresenter extends AbstractInformationPresenter {

    @Override
    public String updatePresentation(Drawable drawable, String hoverInfo, TextPresentation presentation, int maxWidth,
            int maxHeight) {
        if (drawable instanceof StyledText) {
            final StyledText styledText = (StyledText) drawable;
            styledText.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseDown(MouseEvent e) {
                    try {
                        int offset = styledText.getOffsetAtLocation(new Point(e.x, e.y));
                        StyleRange r = styledText.getStyleRangeAtOffset(offset);
                        if (r instanceof StyleRangeWithCustomData) {
                            StyleRangeWithCustomData styleRangeWithCustomData = (StyleRangeWithCustomData) r;
                            onHandleClick(styleRangeWithCustomData.customData);
                        }
                    } catch (IllegalArgumentException e1) {
                        //Don't care about wrong positions...
                    } catch (SWTException e1) {
                    }
                }
            });

            styledText.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    try {
                        if (e.keyCode == SWT.CR || e.keyCode == SWT.LF || e.keyCode == SWT.KEYPAD_CR) {
                            StyleRange r = styledText.getStyleRangeAtOffset(styledText.getSelection().y);
                            if (r instanceof StyleRangeWithCustomData) {
                                StyleRangeWithCustomData styleRangeWithCustomData = (StyleRangeWithCustomData) r;
                                onHandleClick(styleRangeWithCustomData.customData);
                            }
                        }
                    } catch (IllegalArgumentException e1) {
                        //Don't care about wrong positions...
                    } catch (SWTException e1) {
                    }
                }
            });
        }

        hoverInfo = this.correctLineDelimiters(hoverInfo);
        onUpdatePresentation(hoverInfo, presentation);

        return hoverInfo;
    }

    protected abstract void onUpdatePresentation(String hoverInfo, TextPresentation presentation);

    protected abstract void onHandleClick(Object data);

}
