/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 *
 * Author: Mark Leone
 * Created: Feb 11, 2016
 *
 * Loosely follows the JDT implementation of a best match hover. Code for obtaining
 * and configuring contributed Hovers was copied from <code>BestMatchHover</code>,
 * but this implementation combines Hover info in priority order, whereas the JDT
 * implementation chooses the best fit hover.
 */
package org.python.pydev.editor.hover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PydevCombiningHover extends AbstractPyEditorTextHover {

    public static final Object ID_DEFAULT_COMBINING_HOVER = "org.python.pydev.editor.hover.defaultCombiningHover";

    // Note that the specification is static and is redone when the preferences are changed.
    private static List<PyEditorTextHoverDescriptor> fTextHoverSpecifications;

    private static List<AbstractPyEditorTextHover> fInstantiatedTextHovers;

    private Map<AbstractPyEditorTextHover, PyEditorTextHoverDescriptor> hoverMap = new HashMap<>();

    boolean preempt = false;

    Integer currentPriority = null;

    boolean contentTypeSupported = false;

    private int lastDividerLen;

    protected ITextViewer viewer;

    private static final String DIVIDER_CHAR = Character.toString((char) 0xfeff2015);

    public PydevCombiningHover() {
        installTextHovers();
        this.addInformationPresenterControlListener(new ControlListener() {

            @Override
            public void controlMoved(ControlEvent e) {
            }

            @Override
            public void controlResized(ControlEvent e) {
                if (hoverControlPreferredWidth != null) {
                    informationControl.setSize(hoverControlPreferredWidth, informationControl.getBounds().height);
                }
                hoverControlWidth = informationControl.getBounds().width;
                StyledText text = (StyledText) e.getSource();
                if (PyHoverPreferencesPage.getUseHoverDelimiters()) {
                    resizeDividerText(text, hoverControlWidth);
                }
            }

        });
    }

    /**
     * Installs all text hovers.
     */
    public static void installTextHovers() {

        // initialize lists - indicates that the initialization happened
        fTextHoverSpecifications = new ArrayList<PyEditorTextHoverDescriptor>(5);
        fInstantiatedTextHovers = new ArrayList<AbstractPyEditorTextHover>(5);

        // populate list
        PyEditorTextHoverDescriptor[] hoverDescs = PydevPlugin.getDefault().getPyEditorTextHoverDescriptors();
        for (PyEditorTextHoverDescriptor desc : hoverDescs) {
            // ensure that we don't add ourselves to the list
            if (!ID_DEFAULT_COMBINING_HOVER.equals(desc.getId())) {
                fTextHoverSpecifications.add(desc);
            }
        }
    }

    private void checkTextHovers() {
        if (fTextHoverSpecifications == null) {
            return;
        }
        hoverMap.clear();

        List<PyEditorTextHoverDescriptor> specifications = fTextHoverSpecifications;
        fTextHoverSpecifications = null;
        for (PyEditorTextHoverDescriptor spec : specifications) {
            if (spec == null) {
                continue;
            }

            AbstractPyEditorTextHover hover = spec.createTextHover();
            if (hover != null) {
                fInstantiatedTextHovers.add(hover);
                hoverMap.put(hover, spec);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
     */
    @Override
    public String getHoverInfo(final ITextViewer textViewer, IRegion hoverRegion) {
        this.viewer = textViewer;
        final FastStringBuffer buf = new FastStringBuffer();
        checkTextHovers();

        if (fInstantiatedTextHovers == null) {
            return null;
        }

        boolean firstHoverInfo = true;
        //hovers are sorted by priority in descending order
        for (final AbstractPyEditorTextHover hover : fInstantiatedTextHovers) {
            if (hover == null) {
                continue;
            }

            PyEditorTextHoverDescriptor descr = hoverMap.get(hover);
            if (!descr.isEnabled()) {
                continue;
            }
            if (hoverMap.get(hover) != null) {
                if (currentPriority == null) {
                    currentPriority = descr.getPriority();
                }
                if (descr.getPriority().equals(currentPriority) || !preempt) {
                    @SuppressWarnings("deprecation")
                    final String hoverText = hover.getHoverInfo(textViewer, hoverRegion);
                    if (hoverText != null && hoverText.trim().length() > 0) {
                        if (!firstHoverInfo && PyHoverPreferencesPage.getUseHoverDelimiters()) {
                            buf.append(PyInformationPresenter.LINE_DELIM);
                            viewer.getTextWidget().getDisplay().syncExec(new Runnable() {

                                @Override
                                public void run() {
                                    if (hoverControlWidth != null) {
                                        buf.append(createDivider(hoverControlWidth));
                                    } else {
                                        buf.append(createDivider(getMaxExtent(hoverText)));
                                    }
                                }

                            });
                            buf.append(PyInformationPresenter.LINE_DELIM);
                        } else if (buf.length() > 0) {
                            buf.append(PyInformationPresenter.LINE_DELIM);
                        }
                        buf.append(hoverText);
                        firstHoverInfo = false;
                        viewer.getTextWidget().getDisplay().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                checkHoverControlWidth(hover);
                            }

                        });
                    }
                }
                currentPriority = descr.getPriority();

                /* If preempt has already been set, don't unset it if a hover with the same priority
                 * does not have preempt set
                 */
                if (!preempt) {
                    preempt = descr.isPreempt();
                }
            }
        }
        currentPriority = null;
        preempt = false;
        return buf.toString();
    }

    /**
     * Ensures that the width of the control for this Hover is equal to the
     * largest width, if any, set for contributing Hovers.
     * @param hover a contributing Hover
     */
    private void checkHoverControlWidth(AbstractPyEditorTextHover hover) {
        if (hover.getHoverControlPreferredWidth() != null) {
            if (this.hoverControlWidth == null) {
                this.hoverControlPreferredWidth = hover.getHoverControlPreferredWidth();
                if (informationControl != null) {
                    informationControl.setSize(hoverControlPreferredWidth, informationControl.getBounds().height);
                }
            } else if (hover.getHoverControlPreferredWidth() > this.hoverControlWidth) {
                this.hoverControlPreferredWidth = hover.getHoverControlPreferredWidth();
                if (informationControl != null) {
                    informationControl.setSize(hoverControlPreferredWidth, informationControl.getBounds().height);
                }
            }
        }
    }

    /**
     * Resizes the divider between Hovers when the control is resized, to
     * fit exactly in the control's client area.
     * @param text the <code>StyledText</code> containing the hover info
     * @param width the desired width of the divider in pixels
     */
    protected void resizeDividerText(StyledText text, final int width) {
        if (width != lastDividerLen) {
            final String[] newDivider = new String[1];
            int oldLen = lastDividerLen;
            text.getDisplay().syncExec(new Runnable() {

                @Override
                public void run() {
                    newDivider[0] = createDivider(width);
                }

            });
            String regex = "\\" + DIVIDER_CHAR + "{" + oldLen + "}\\n\\s\\" + DIVIDER_CHAR + "{" +
                    Math.abs(oldLen - lastDividerLen) + "}";
            StyleRange[] ranges = text.getStyleRanges();
            text.setText(text.getText().replaceAll(regex, newDivider[0]));
            text.setStyleRanges(ranges);
        }
    }

    /**
     * Creates divider text of a specified width
     * Must be called from the event dispatch thread
     *
     * @param width the desired width of the divider in pixels
     * @return the divider text
     */
    private String createDivider(final int width) {
        Assert.isTrue(Display.getCurrent().getThread() == Thread.currentThread(),
                "This method must be called from the UI thread");
        final StringBuilder divider = new StringBuilder();
        getHoverControlCreator();
        GC gc = new GC(viewer.getTextWidget().getDisplay());
        while (gc.stringExtent(divider.toString()).x < width) {
            divider.append(DIVIDER_CHAR);
        }
        divider.deleteCharAt(divider.length() - 1);
        gc.dispose();
        lastDividerLen = divider.length();
        return divider.toString();
    }

    private int getMaxExtent(String hoverText) {
        GC gc = new GC(viewer.getTextWidget().getDisplay());
        int max = 0;
        for (String line : hoverText.split("\\n")) {
            /*TODO we need a way to skip lines that will be formatted by the InformationPresenter
              For now, we hard-code it to skip file paths embedded in the hover info*/
            if (!line.startsWith("FILE_PATH=")) {
                int extent = gc.stringExtent(line).x;
                if (extent > max) {
                    max = extent;
                }
            }
        }
        gc.dispose();
        return max;
    }

    /*
     * (non-Javadoc)
     * @see org.python.pydev.editor.hover.AbstractPyEditorTextHover#isContentTypeSupported(java.lang.String)
     */
    @Override
    public boolean isContentTypeSupported(String contentType) {
        return true;
    }

}
