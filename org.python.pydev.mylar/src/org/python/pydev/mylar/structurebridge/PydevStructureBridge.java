package org.python.pydev.mylar.structurebridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.mylar.context.core.AbstractContextStructureBridge;
import org.eclipse.mylar.context.core.AbstractRelationProvider;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.context.core.IDegreeOfSeparation;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.ui.views.markers.internal.ConcreteMarker;
import org.python.pydev.navigator.IWrappedResource;

public class PydevStructureBridge extends AbstractContextStructureBridge {

    public final static String CONTENT_TYPE = "python";

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public String getParentHandle(String handle) {

        IResource resource = (IResource) getObjectForHandle(handle);
        if (resource != null) {
            IContainer parent = resource.getParent();
            // try to adapt to the corresponding content type's parent
            if (resource instanceof IFile) {
                for (String contentType : ContextCorePlugin.getDefault().getChildContentTypes(CONTENT_TYPE)) {
                    AbstractContextStructureBridge parentBridge = ContextCorePlugin.getDefault().getStructureBridge(contentType);
                    Object adaptedParent = parentBridge.getAdaptedParent(resource);
                    // HACK: only returns first
                    if (adaptedParent != null) {
                        return parentBridge.getHandleIdentifier(adaptedParent);
                    }
                }
            }
            return getHandleIdentifier(parent);
        } else {
            return null;
        }
    }

    @Override
    public List<String> getChildHandles(String handle) {
        Object object = getObjectForHandle(handle);
        if (object instanceof IResource) {
            IResource resource = (IResource) object;
            if (resource instanceof IContainer) {
                IContainer container = (IContainer) resource;
                IResource[] children;
                try {
                    children = container.members();
                    List<String> childHandles = new ArrayList<String>();
                    for (int i = 0; i < children.length; i++) {
                        String childHandle = getHandleIdentifier(children[i]);
                        if (childHandle != null)
                            childHandles.add(childHandle);
                    }
                    return childHandles;
                } catch (Exception e) {
                    MylarStatusHandler.fail(e, "could not get child", false);
                }
            } else if (resource instanceof IFile) {
                // delegate to child bridges
            }
        }
        return Collections.emptyList();
    }

    /**
     * Uses java-style path for projects.
     */
    @Override
    public String getHandleIdentifier(Object object) {
        if (object instanceof IWrappedResource) {
            Object actualObject = ((IWrappedResource) object).getActualObject();
            if (actualObject instanceof IResource) {
                IResource resource = (IResource) actualObject;
                return resource.getFullPath().toPortableString();
            }
        }
        return null;
    }

    @Override
    public Object getObjectForHandle(String handle) {
        if (handle == null)
            return null;
        IPath path = new Path(handle);
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (path.segmentCount() == 1) {
            String projectName = handle.substring(1);
            try {
                return workspace.getRoot().getProject(projectName);
            } catch (IllegalArgumentException e) {
                // not a file
                // MylarStatusHandler.fail(e, "bad path for handle: " + handle,
                // false);
                return null;
            }
        } else if (path.segmentCount() > 1) {
            return workspace.getRoot().findMember(path);
        } else {
            return null;
        }
    }

    @Override
    public String getName(Object object) {
        if (object instanceof IResource) {
            return ((IResource) object).getName();
        } else {
            return "";
        }
    }

    @Override
    public boolean canBeLandmark(String handle) {
        Object element = getObjectForHandle(handle);
        return element instanceof IFile;
    }

    @Override
    public boolean acceptsObject(Object object) {
        if (object instanceof IWrappedResource) {
            IWrappedResource w = (IWrappedResource) object;
            if (w.getActualObject() instanceof IResource) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canFilter(Object element) {
        return true;
    }

    @Override
    public boolean isDocument(String handle) {
        return getObjectForHandle(handle) instanceof IFile;
    }

    @Override
    public String getHandleForOffsetInObject(Object resource, int offset) {
        if (resource == null || !(resource instanceof ConcreteMarker))
            return null;
        ConcreteMarker marker = (ConcreteMarker) resource;
        // we can only get a handle for a marker with the resource plugin.xml
        try {
            IResource res = marker.getResource();

            if (res instanceof IFile) {
                IFile file = (IFile) res;
                return getHandleIdentifier(file);
            }
            return null;
        } catch (Throwable t) {
            MylarStatusHandler.log(t, "Could not find element for: " + marker);
            return null;
        }
    }

    @Override
    public String getContentType(String elementHandle) {
        return getContentType();
    }

    /**
     * These methods aren't needed since there is no generic active search
     */
    @Override
    public List<AbstractRelationProvider> getRelationshipProviders() {
        return Collections.emptyList();
    }

    @Override
    public List<IDegreeOfSeparation> getDegreesOfSeparation() {
        return Collections.emptyList();
    }

}
