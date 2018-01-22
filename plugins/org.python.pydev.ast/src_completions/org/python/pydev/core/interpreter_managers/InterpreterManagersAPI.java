package org.python.pydev.core.interpreter_managers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;

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
     * @param file
     * @return
     *
     */
    public static Tuple<IPythonNature, String> getInfoForManager(File file,
            IInterpreterManager pythonInterpreterManager) {
        if (pythonInterpreterManager != null) {
            if (pythonInterpreterManager.isConfigured()) {
                IInterpreterInfo[] interpreterInfos = pythonInterpreterManager.getInterpreterInfos();
                for (IInterpreterInfo iInterpreterInfo : interpreterInfos) {
                    try {
                        SystemPythonNature systemPythonNature = new SystemPythonNature(pythonInterpreterManager,
                                iInterpreterInfo);
                        String modName = systemPythonNature.resolveModule(file);
                        if (modName != null) {
                            return new Tuple<IPythonNature, String>(systemPythonNature, modName);
                        }
                    } catch (Exception e) {
                        // that's ok
                    }
                }
            }
        }
        return null;
    }

    /**
     * This is the last resort (should not be used anywhere else).
     */
    public static String getModNameFromFile(File file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        int i = name.indexOf('.');
        if (i != -1) {
            return name.substring(0, i);
        }
        return name;
    }

    /**
     * @param file the file we want to get info on.
     * @return a tuple with the nature to be used and the name of the module represented by the file in that scenario.
     */
    public static Tuple<IPythonNature, String> getInfoForFile(File file) {

        IInterpreterManager pythonInterpreterManager2 = getPythonInterpreterManager(false);
        IInterpreterManager jythonInterpreterManager2 = getJythonInterpreterManager(false);
        IInterpreterManager ironpythonInterpreterManager2 = getIronpythonInterpreterManager(false);

        if (file != null) {
            //Check if we can resolve the manager for the passed file...
            Tuple<IPythonNature, String> infoForManager = getInfoForManager(file,
                    pythonInterpreterManager2);
            if (infoForManager != null) {
                return infoForManager;
            }

            infoForManager = getInfoForManager(file, jythonInterpreterManager2);
            if (infoForManager != null) {
                return infoForManager;
            }

            infoForManager = getInfoForManager(file, ironpythonInterpreterManager2);
            if (infoForManager != null) {
                return infoForManager;
            }

            //Ok, the file is not part of the interpreter configuration, but it's still possible that it's part of a
            //project... (external projects), so, let's go on and see if there's some match there.

            List<IPythonNature> allPythonNatures = PythonNature.getAllPythonNatures();
            int size = allPythonNatures.size();
            for (int i = 0; i < size; i++) {
                IPythonNature nature = allPythonNatures.get(i);
                try {
                    //Note: only resolve in the project sources, as we've already checked the system and we'll be
                    //checking all projects anyways.
                    String modName = nature
                            .resolveModuleOnlyInProjectSources(FileUtils.getFileAbsolutePath(file), true);
                    if (modName != null) {
                        return new Tuple<IPythonNature, String>(nature, modName);
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        if (pythonInterpreterManager2.isConfigured()) {
            try {
                return new Tuple<IPythonNature, String>(new SystemPythonNature(pythonInterpreterManager2),
                        getModNameFromFile(file));
            } catch (MisconfigurationException e) {
            }
        }

        if (jythonInterpreterManager2.isConfigured()) {
            try {
                return new Tuple<IPythonNature, String>(new SystemPythonNature(jythonInterpreterManager2),
                        getModNameFromFile(file));
            } catch (MisconfigurationException e) {
            }
        }

        if (ironpythonInterpreterManager2.isConfigured()) {
            try {
                return new Tuple<IPythonNature, String>(new SystemPythonNature(ironpythonInterpreterManager2),
                        getModNameFromFile(file));
            } catch (MisconfigurationException e) {
            }
        }

        //Ok, nothing worked, let's just do a call which'll ask to configure python and return null!
        try {
            pythonInterpreterManager2.getDefaultInterpreterInfo(true);
        } catch (MisconfigurationException e) {
            //Ignore
        }
        return null;
    }

    public static Map<IInterpreterManager, Map<String, IInterpreterInfo>> getInterpreterManagerToInterpreterNameToInfo() {
        Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo = new HashMap<>();
        IInterpreterManager[] allInterpreterManagers = getAllInterpreterManagers();
        for (int i = 0; i < allInterpreterManagers.length; i++) {
            IInterpreterManager manager = allInterpreterManagers[i];
            if (manager == null) {
                continue;
            }
            Map<String, IInterpreterInfo> nameToInfo = new HashMap<>();
            managerToNameToInfo.put(manager, nameToInfo);
    
            IInterpreterInfo[] interpreterInfos = manager.getInterpreterInfos();
            for (int j = 0; j < interpreterInfos.length; j++) {
                IInterpreterInfo internalInfo = interpreterInfos[j];
                nameToInfo.put(internalInfo.getName(), internalInfo);
            }
        }
        return managerToNameToInfo;
    }

}
