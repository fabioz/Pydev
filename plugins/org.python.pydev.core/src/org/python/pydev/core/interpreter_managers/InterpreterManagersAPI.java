package org.python.pydev.core.interpreter_managers;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.shared_core.io.FileUtils;

public class InterpreterManagersAPI {

    public static IInterpreterManager pythonInterpreterManager;
    public static IInterpreterManager jythonInterpreterManager;
    public static IInterpreterManager ironpythonInterpreterManager;

    public static void setPythonInterpreterManager(IInterpreterManager interpreterManager) {
        pythonInterpreterManager = interpreterManager;
    }

    public static IInterpreterManager getPythonInterpreterManager() {
        return getPythonInterpreterManager(false);
    }

    public static IInterpreterManager getPythonInterpreterManager(boolean haltOnStub) {
        return pythonInterpreterManager;
    }

    public static void setJythonInterpreterManager(IInterpreterManager interpreterManager) {
        jythonInterpreterManager = interpreterManager;
    }

    public static IInterpreterManager getJythonInterpreterManager() {
        return getJythonInterpreterManager(false);
    }

    public static IInterpreterManager getJythonInterpreterManager(boolean haltOnStub) {
        return jythonInterpreterManager;
    }

    public static void setIronpythonInterpreterManager(IInterpreterManager interpreterManager) {
        ironpythonInterpreterManager = interpreterManager;
    }

    public static IInterpreterManager getIronpythonInterpreterManager(boolean haltOnStub) {
        return ironpythonInterpreterManager;
    }

    public static IInterpreterManager getIronpythonInterpreterManager() {
        return getIronpythonInterpreterManager(false);
    }

    public static IInterpreterManager[] getAllInterpreterManagers() {
        return new IInterpreterManager[] { getPythonInterpreterManager(),
                getJythonInterpreterManager(), getIronpythonInterpreterManager() };
    }

    public static List<IInterpreterInfo> getAllInterpreterInfos() {
        List<IInterpreterInfo> infos = new ArrayList<>();
        IInterpreterManager[] allInterpreterManagers = getAllInterpreterManagers();
        for (IInterpreterManager iInterpreterManager : allInterpreterManagers) {
            if (iInterpreterManager != null) {
                infos.addAll(Arrays.asList(iInterpreterManager.getInterpreterInfos()));
            }
        }
        return infos;
    }

    /**
     * returns the interpreter manager for a given nature
     * @param nature the nature from where we want to get the associated interpreter manager
     *
     * @return the interpreter manager
     */
    public static IInterpreterManager getInterpreterManager(IPythonNature nature) {
        try {
            return getInterpreterManagerFromType(nature.getInterpreterType());
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static IInterpreterManager getInterpreterManagerFromType(int interpreterType) {
        try {
            switch (interpreterType) {
                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    return jythonInterpreterManager;
                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                    return pythonInterpreterManager;
                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                    return ironpythonInterpreterManager;
                default:
                    throw new RuntimeException("Unable to get the interpreter manager for unknown interpreter type: "
                            + interpreterType);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Given a resource get the string in the filesystem for it.
     */
    public static String getIResourceOSString(IResource f) {
        URI locationURI = f.getLocationURI();
        if (locationURI != null) {
            try {
                //RTC source control not return a valid uri
                return FileUtils.getFileAbsolutePath(new File(locationURI));
            } catch (IllegalArgumentException e) {
            }
        }
    
        IPath rawLocation = f.getRawLocation();
        if (rawLocation == null) {
            return null; //yes, we could have a resource that was deleted but we still have it's representation...
        }
        String fullPath = rawLocation.toOSString();
        //now, we have to make sure it is canonical...
        File file = new File(fullPath);
        if (file.exists()) {
            return FileUtils.getFileAbsolutePath(file);
        } else {
            //it does not exist, so, we have to check its project to validate the part that we can
            IProject project = f.getProject();
            IPath location = project.getLocation();
            File projectFile = location.toFile();
            if (projectFile.exists()) {
                String projectFilePath = FileUtils.getFileAbsolutePath(projectFile);
    
                if (fullPath.startsWith(projectFilePath)) {
                    //the case is all ok
                    return fullPath;
                } else {
                    //the case appears to be different, so, let's check if this is it...
                    if (fullPath.toLowerCase().startsWith(projectFilePath.toLowerCase())) {
                        String relativePart = fullPath.substring(projectFilePath.length());
    
                        //at least the first part was correct
                        return projectFilePath + relativePart;
                    }
                }
            }
        }
    
        //it may not be correct, but it was the best we could do...
        return fullPath;
    }

}
