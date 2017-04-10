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

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
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
                            buf.appendN(DIVIDER_CHAR, 20);
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

    /*
     * (non-Javadoc)
     * @see org.python.pydev.editor.hover.AbstractPyEditorTextHover#isContentTypeSupported(java.lang.String)
     */
    @Override
    public boolean isContentTypeSupported(String contentType) {
        return true;
    }

}
