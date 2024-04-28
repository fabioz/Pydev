package com.python.pydev.analysis.external;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.markers.PyMarkerUtils;

import com.python.pydev.analysis.additionalinfo.builders.IMarkerHandler;

public abstract class AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor implements IExternalCodeAnalysisVisitor {

    protected final IResource resource;
    private IMarkerHandler markerHandler;

    public AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor(IResource resource) {
        this.resource = resource;
    }

    @Override
    public void setMarkerHandler(IMarkerHandler markerHandler) {
        this.markerHandler = markerHandler;
    }

    @Override
    public void startVisit() {
        deleteMarkers();
    }

    @Override
    public void join() {
        //no-op
    }

    @Override
    public List<PyMarkerUtils.MarkerInfo> getMarkers(IResource resource) {
        return null;
    }

    @Override
    public void deleteMarkers() {
        //Whenever PyLint is passed, the markers will be deleted.
        try {
            if (resource != null) {
                if (markerHandler != null) {
                    markerHandler.deleteMarkers(resource, getProblemMarkerId());
                } else {
                    resource.deleteMarkers(getProblemMarkerId(), false, IResource.DEPTH_ZERO);
                }
            }
        } catch (CoreException e3) {
            Log.log(e3);
        }
    }

    @Override
    public boolean getRequiresAnalysis() {
        return false; // If only to remove does not require analysis.
    }

}
