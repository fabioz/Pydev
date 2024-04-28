package com.python.pydev.analysis.additionalinfo.builders;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;

public interface IMarkerHandler {

    void replaceMarkers(List<MarkerInfo> lst, IResource resource, String markerType,
            boolean removeUserEditable, IProgressMonitor monitor);

    void deleteAnalysisMarkers(IResource r);

    void deleteOnlyPydevAnalysisMarkers(IResource r);

    void deleteMarkers(IResource resource, String problemMarkerId);

}
