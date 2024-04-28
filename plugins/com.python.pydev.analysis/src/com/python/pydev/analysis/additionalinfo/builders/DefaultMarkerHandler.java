package com.python.pydev.analysis.additionalinfo.builders;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;

public class DefaultMarkerHandler implements IMarkerHandler {

    @Override
    public void replaceMarkers(final List<MarkerInfo> lst, final IResource resource, final String markerType,
            final boolean removeUserEditable, IProgressMonitor monitor) {
        PyMarkerUtils.replaceMarkers(lst, resource, markerType, removeUserEditable, monitor);
    }

    @Override
    public void deleteAnalysisMarkers(IResource r) {
        AnalysisRunner.deleteMarkers(r);

    }

    @Override
    public void deleteOnlyPydevAnalysisMarkers(IResource r) {
        boolean onlyPydevAnalysisMarkers = true;
        AnalysisRunner.deleteMarkers(r, onlyPydevAnalysisMarkers);
    }

    @Override
    public void deleteMarkers(IResource resource, String problemMarkerId) {
        try {
            resource.deleteMarkers(problemMarkerId, false, IResource.DEPTH_ZERO);
        } catch (CoreException e) {
            Log.log(e);
        }
    }

}
