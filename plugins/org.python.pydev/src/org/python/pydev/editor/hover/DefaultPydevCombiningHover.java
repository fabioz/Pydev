package org.python.pydev.editor.hover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class DefaultPydevCombiningHover extends AbstractPyEditorTextHover implements IExecutableExtensionFactory {

    public static final Object ID_DEFAULT_COMBINING_HOVER = "org.python.pydev.editor.hover.defaultCombiningHover";

    private ArrayList<PyEditorTextHoverDescriptor> fTextHoverSpecifications;

    private ArrayList<AbstractPyEditorTextHover> fInstantiatedTextHovers;

    private Map<AbstractPyEditorTextHover, PyEditorTextHoverDescriptor> hoverMap = new HashMap<AbstractPyEditorTextHover, PyEditorTextHoverDescriptor>();

    boolean preempt = false;

    Integer currentPriority = null;

    boolean contentTypeSupported = false;

    public DefaultPydevCombiningHover() {
        installTextHovers();
    }

    /**
     * Installs all text hovers.
     */
    private void installTextHovers() {

        // initialize lists - indicates that the initialization happened
        fTextHoverSpecifications = new ArrayList<PyEditorTextHoverDescriptor>(2);
        fInstantiatedTextHovers = new ArrayList<AbstractPyEditorTextHover>(2);

        // populate list
        PyEditorTextHoverDescriptor[] hoverDescs = PydevPlugin.getDefault().getPyEditorTextHoverDescriptors(false);
        for (int i = 0; i < hoverDescs.length; i++) {
            // ensure that we don't add ourselves to the list
            if (!ID_DEFAULT_COMBINING_HOVER.equals(hoverDescs[i].getId())) {
                fTextHoverSpecifications.add(hoverDescs[i]);
            }
        }
    }

    private void checkTextHovers() {
        if (fTextHoverSpecifications == null) {
            return;
        }

        boolean done = true;
        int i = -1;
        for (Iterator<PyEditorTextHoverDescriptor> iterator = fTextHoverSpecifications.iterator(); iterator
                .hasNext();) {
            i++;
            PyEditorTextHoverDescriptor spec = iterator.next();
            if (spec == null) {
                continue;
            }

            done = false;

            AbstractPyEditorTextHover hover = spec.createTextHover();
            if (hover != null) {
                fTextHoverSpecifications.set(i, null);
                if (i == fInstantiatedTextHovers.size()) {
                    fInstantiatedTextHovers.add(i, hover);
                } else {
                    fInstantiatedTextHovers.set(i, hover);
                }
                hoverMap.put(hover, spec);
            }

        }
        if (done) {
            fTextHoverSpecifications = null;
        }
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        FastStringBuffer buf = new FastStringBuffer();
        checkTextHovers();

        if (fInstantiatedTextHovers == null) {
            return null;
        }

        //hovers are sorted by priority in descending order
        for (Iterator<AbstractPyEditorTextHover> iterator = fInstantiatedTextHovers.iterator(); iterator.hasNext();) {
            AbstractPyEditorTextHover hover = iterator.next();
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
                    String hoverText = hover.getHoverInfo(textViewer, hoverRegion);
                    if (hoverText != null && hoverText.trim().length() > 0) {
                        if (buf.length() > 0) {
                            buf.append(PyInformationPresenter.LINE_DELIM);
                        }
                        buf.append(hoverText);
                    }
                }
                currentPriority = descr.getPriority();
                preempt = descr.isPreempt();
            }
        }
        currentPriority = null;
        preempt = false;
        return buf.toString();
    }

    @Override
    public boolean isContentTypeSupported(String contentType) {
        return true;
    }

    @Override
    public Object create() throws CoreException {
        try {
            PyEditorTextHoverDescriptor contributedHover = PydevPlugin.getDefault()
                    .getPyEditorCombiningTextHoverDescriptor(false);
            return contributedHover != null ? contributedHover : new DefaultPydevCombiningHover();
        } catch (CoreException e) {
            Log.log(e.getMessage());
            return new DefaultPydevCombiningHover();
        }
    }

}
