package com.python.pydev.analysis.external;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.markers.PyMarkerUtils;

public abstract class AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor implements IExternalCodeAnalysisVisitor {

    protected IResource resource;

    public AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor(IResource resource) {
        this.resource = resource;
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
    public List<PyMarkerUtils.MarkerInfo> getMarkers() {
        return null;
    }

    @Override
    public void deleteMarkers() {
        //Whenever PyLint is passed, the markers will be deleted.
        try {
            if (resource != null) {
                resource.deleteMarkers(getProblemMarkerId(), false, IResource.DEPTH_ZERO);
            }
        } catch (CoreException e3) {
            Log.log(e3);
        }
    }

    protected abstract String getProblemMarkerId();

}
