/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Aug 20, 2003
 */
package org.python.pydev.debug.core;


/**
 * all the public constants for pydev.debug
 */
public interface Constants {

    // Icons
    static final String MAIN_ICON = "icons/python_run.png";
    static final String ARGUMENTS_ICON = "icons/arguments.gif";
    static final String PYTHON_ORG_ICON = "icons/python_16x16.png";

    // Plugin constants
    static final String PLUGIN_ID = "org.python.pydev.debug";

    static final String ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.debug.regularLaunchConfigurationType";
    static final String ID_PYTHON_COVERAGE_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.debug.coverageLaunchConfigurationType";
    static final String ID_PYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.debug.unittestLaunchConfigurationType";

    static final String ID_JYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.debug.jythonUnittestLaunchConfigurationType";
    static final String ID_JYTHON_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.debug.jythonLaunchConfigurationType";

    static final String ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.debug.ironpythonLaunchConfigurationType";
    static final String ID_IRONPYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE = "org.python.pydev.debug.ironpythonUnittestLaunchConfigurationType";

    static final String PROCESS_TYPE = "PYDEV.PYTHON";

    static final String PYDEV_DEBUG_IPROCESS_ATTR = "PYDEV_DEBUG_IPROCESS_ATTR";
    static final String PYDEV_DEBUG_IPROCESS_ATTR_TRUE = "true";

    static final String ATTR_VM_ARGUMENTS = "org.python.pydev.debug.vm.arguments";
    static final String JAVA_NATURE = "org.eclipse.jdt.core.javanature";

    // LaunchConfiguration properties
    // ATTR_* are copied from IExternalToolConstants, replicated since I have no access to originals
    static final String ATTR_LOCATION = "org.eclipse.ui.externaltools" + ".ATTR_LOCATION";
    /** Attribute holding alternate location that is used to actually run the resource. */
    static final String ATTR_ALTERNATE_LOCATION = "org.python.pydev.debug.core" + ".ATTR_ALTERNATE_LOCATION";
    /** Attribute to control if a wrapper for test runner should be used. */
    static final String ATTR_WORKING_DIRECTORY = "org.eclipse.ui.externaltools" + ".ATTR_WORKING_DIRECTORY";
    static final String ATTR_OTHER_WORKING_DIRECTORY = "org.eclipse.ui.externaltools" + ".ATTR_OTHER_WORKING_DIRECTORY";
    static final String ATTR_PROGRAM_ARGUMENTS = "org.eclipse.ui.externaltools" + ".ATTR_TOOL_ARGUMENTS";

    static final String ATTR_INTERPRETER = PLUGIN_ID + ".ATTR_INTERPRETER";
    static final String ATTR_INTERPRETER_DEFAULT = "__default"; //$NO-NLS-1$; 
    static final String ATTR_PROJECT = PLUGIN_ID + ".ATTR_PROJECT";
    static final String ATTR_RESOURCE_TYPE = PLUGIN_ID + ".ATTR_RESOURCE_TYPE";
    static final String ATTR_UNITTEST_TESTS = PLUGIN_ID + ".ATTR_UNITTEST_TESTS";
    static final String ATTR_UNITTEST_CONFIGURATION_FILE = PLUGIN_ID + ".ATTR_UNITTEST_CONFIGURATION_FILE";
    static final String PYDEV_CONFIG_RUN = "PYDEV_CONFIG_RUN";

}
