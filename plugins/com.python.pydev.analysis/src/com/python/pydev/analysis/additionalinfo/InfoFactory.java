/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IMemento;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.actions.AdditionalInfoAndIInfo;

/**
 * The InfoFactory is used to save and recreate an AdditionalInfoAndIInfo object.
 *
 * @see org.eclipse.ui.internal.ide.model.ResourceFactory
 */
@SuppressWarnings("restriction")
public class InfoFactory {

    //These constants are stored in the XML. Do not change them.

    private static final String TAG_MODULE_NAME = "module_name";

    private static final String TAG_PATH = "path";

    private static final String TAG_NAME = "name";

    private static final String TAG_TYPE = "type";

    private static final String TAG_PROJECT_NAME = "project";

    /**
     * This one is deprecated and should not actually appear unless for backward compatibility.
     */
    private static final String TAG_MANAGER_IS_PYTHON = "is_python";

    /**
     * Substituted the is_python tag for a tag that maps to the interpreter type.
     */
    private static final String TAG_MANAGER_INTERPRETER_TYPE = "interpreter_type";

    private static final String TAG_MANAGER_INTERPRETER = "interpreter";

    /**
     * Data to persist
     */
    private AdditionalInfoAndIInfo info;

    public InfoFactory() {
    }

    public InfoFactory(AdditionalInfoAndIInfo input) {
        info = input;
    }

    public AdditionalInfoAndIInfo createElement(IMemento memento) {
        String[] attributeKeys = null;
        try {
            attributeKeys = memento.getAttributeKeys();
            HashSet<String> keys = new HashSet<String>(Arrays.asList(attributeKeys));
            if (!keys.contains(TAG_NAME) || !keys.contains(TAG_MODULE_NAME) || !keys.contains(TAG_PATH)
                    || !keys.contains(TAG_TYPE)) {
                return null;
            }

            String name = memento.getString(TAG_NAME);
            String moduleName = memento.getString(TAG_MODULE_NAME);
            String path = memento.getString(TAG_PATH);
            final int type = memento.getInteger(TAG_TYPE);

            String infoName = null;
            String infoModule = null;
            String infoPath = null;

            if (name != null && name.length() > 0) {
                infoName = name;
            }

            if (moduleName != null && moduleName.length() > 0) {
                infoModule = moduleName;
            }

            if (path != null && path.length() > 0) {
                infoPath = path;
            }

            String projectName = null;
            if (keys.contains(TAG_PROJECT_NAME)) {
                projectName = memento.getString(TAG_PROJECT_NAME);
            }

            IPythonNature nature = null;
            if (projectName != null) {
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                if (project != null) {
                    nature = PythonNature.getPythonNature(project);
                }
            }

            AbstractInfo info;
            if (type == IInfo.ATTRIBUTE_WITH_IMPORT_TYPE) {
                info = new AttrInfo(infoName, infoModule, infoPath, nature);

            } else if (type == IInfo.CLASS_WITH_IMPORT_TYPE) {
                info = new ClassInfo(infoName, infoModule, infoPath, nature);

            } else if (type == IInfo.METHOD_WITH_IMPORT_TYPE) {
                info = new FuncInfo(infoName, infoModule, infoPath, nature);

            } else if (type == IInfo.NAME_WITH_IMPORT_TYPE) {
                info = new NameInfo(infoName, infoModule, infoPath, nature);

            } else if (type == IInfo.MOD_IMPORT_TYPE) {
                info = new ModInfo(infoModule, nature);

            } else {
                throw new AssertionError("Cannot restore type: " + type);
            }

            IInterpreterManager manager = null;
            if (projectName != null) {
                if (nature != null) {
                    AbstractAdditionalDependencyInfo additionalInfo;
                    try {
                        additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
                    } catch (Exception e) {
                        return null; //don't even report the error (this could happen if a saved option doesn't exist anymore)
                    }
                    return new AdditionalInfoAndIInfo(additionalInfo, info);
                }

            } else if (keys.contains(TAG_MANAGER_INTERPRETER_TYPE) && keys.contains(TAG_MANAGER_INTERPRETER)) {
                Integer interpreterType = memento.getInteger(TAG_MANAGER_INTERPRETER_TYPE);
                if (interpreterType != null) {
                    switch (interpreterType) {
                        case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                            manager = PydevPlugin.getPythonInterpreterManager();
                            break;

                        case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                            manager = PydevPlugin.getJythonInterpreterManager();
                            break;

                        case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                            manager = PydevPlugin.getIronpythonInterpreterManager();
                            break;
                    }

                }

            } else if (keys.contains(TAG_MANAGER_IS_PYTHON) && keys.contains(TAG_MANAGER_INTERPRETER)) {
                //Kept for backward compatibility
                Boolean isTagPython = memento.getBoolean(TAG_MANAGER_IS_PYTHON);
                if (isTagPython != null) {
                    if (isTagPython) {
                        manager = PydevPlugin.getPythonInterpreterManager();
                    } else {
                        manager = PydevPlugin.getJythonInterpreterManager();

                    }
                }
            }

            //If it gets here, it MUST contain the TAG_MANAGER_INTERPRETER
            if (manager != null) {
                String interpreter = memento.getString(TAG_MANAGER_INTERPRETER);

                AbstractAdditionalTokensInfo additionalInfo;
                try {
                    additionalInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(manager, interpreter);
                } catch (Exception e) {
                    return null; //don't even report the error (this could happen if a saved option doesn't exist anymore)
                }
                if (additionalInfo != null) {
                    return new AdditionalInfoAndIInfo(additionalInfo, info);
                }
            }
        } catch (Throwable e) {
            //Don't fail because we weren't able to restore some info, just log and return null (which clients should expect).
            Log.log(e);
            return null;
        }

        return null;
    }

    public void saveState(IMemento memento) {
        if (info.info == null) {
            return;
        }
        String declaringModuleName = info.info.getDeclaringModuleName();
        if (declaringModuleName == null) {
            declaringModuleName = "";
        }
        memento.putString(TAG_MODULE_NAME, declaringModuleName);

        String path = info.info.getPath();
        if (path == null) {
            path = "";
        }
        memento.putString(TAG_PATH, path);

        String name = info.info.getName();
        if (name == null) {
            name = "";
        }
        memento.putString(TAG_NAME, name);

        memento.putString(TAG_TYPE, info.info.getType() + "");
        if (info.additionalInfo instanceof AdditionalProjectInterpreterInfo) {
            AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) info.additionalInfo;
            memento.putString(TAG_PROJECT_NAME, projectInterpreterInfo.getProject().getName());

        } else if (info.additionalInfo instanceof AdditionalSystemInterpreterInfo) {
            AdditionalSystemInterpreterInfo systemInterpreterInfo = (AdditionalSystemInterpreterInfo) info.additionalInfo;
            IInterpreterManager manager = systemInterpreterInfo.getManager();
            memento.putInteger(TAG_MANAGER_INTERPRETER_TYPE, manager.getInterpreterType());
            memento.putString(TAG_MANAGER_INTERPRETER, systemInterpreterInfo.getAdditionalInfoInterpreter());

        }

    }
}
