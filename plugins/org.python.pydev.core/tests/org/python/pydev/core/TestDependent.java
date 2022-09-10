/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Create a file TestDependent.java with the contents in this file and substitute the
 * values as needed...
 */
package org.python.pydev.core;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.python.pydev.shared_core.io.FileUtils;

public class TestDependent {

    //Use defaults and override later as needed.

    //Python (required)
    // PYTHON_INSTALL is only used to set other variables in TestDependent
    public static String PYTHON_INSTALL = null;
    // TEST_PYDEV_BASE_LOC is only used to set other variables in TestDependent
    public static String TEST_PYDEV_BASE_LOC = null;

    // CONDA_PYTHON_38_ENV is used to get the default Conda Python environment for Conda tests.
    public static String CONDA_PYTHON_38_ENV = null;

    //Python (implicitly resolved based on the Python variables above if not specified).
    public static String PYTHON_LIB = null;
    // PYTHON_DLLS applies to Windows only
    public static String PYTHON_DLLS = null;
    public static String PYTHON_EXE = null;
    public static String PYTHON2_SITE_PACKAGES = null;
    public static String PYTHON_LIB_DYNLOAD = null;

    //Python (optional): related tests won't be run if not available
    public static String PYTHON38_QT5_PACKAGES = null;

    public static String PYTHON2_WXPYTHON_PACKAGES = null;
    public static String PYTHON_NUMPY_PACKAGES = null;
    public static String PYTHON_DJANGO_PACKAGES = null;
    public static String PYTHON2_OPENGL_PACKAGES = null;
    public static String PYTHON2_MX_PACKAGES = null;
    public static String PYTHON2_PIL_PACKAGES = null;

    // the following are all derived from TEST_PYDEV_BASE_LOC if unset
    public static String PYSRC_LOC = null;
    public static String HELPERS_LOC = null;
    public static String TEST_PYSRC_TESTING_LOC = null;
    public static String TEST_PYSRC_NAVIGATOR_LOC = null;
    public static String TEST_PYSRC_TESTING_LOC2 = null;
    public static String TEST_PYDEV_PLUGIN_LOC = null;
    public static String TEST_PYDEV_DEBUG_PLUGIN_LOC = null;
    public static String TEST_PYDEV_JYTHON_PLUGIN_LOC = null;
    public static String TEST_PYDEV_PARSER_PLUGIN_LOC = null;
    public static String TEST_PYDEV_REFACTORING_PLUGIN_LOC = null;
    public static String TEST_COM_REFACTORING_PYSRC_LOC = null;

    //java info
    public static String JAVA_LOCATION = null;
    public static String JAVA_RT_JAR_LOCATION = null;

    //Jython (required)
    public static String JYTHON_JAR_LOCATION = null;
    public static String JYTHON_LIB_LOCATION = null;
    public static String JYTHON_ANT_JAR_LOCATION = null;
    public static String JYTHON_JUNIT_JAR_LOCATION = null;

    //Iron Python (optional)
    public static String IRONPYTHON_EXE = null;
    public static String IRONPYTHON_LIB = null;

    //Boolean to know if we can run SWT tests
    public static boolean HAS_SWT_ON_PATH = false;

    //Cygwin (optional)
    public static String CYGWIN_CYGPATH_LOCATION = null;
    public static String CYGWIN_UNIX_CYGPATH_LOCATION = null;

    // Google App Engine
    public static String GOOGLE_APP_ENGINE_LOCATION = null;

    public static String getCompletePythonLib(boolean addSitePackages, boolean isPython3) {
        String dlls = "";
        if (PYTHON_LIB_DYNLOAD == null) {
            PYTHON_LIB_DYNLOAD = "";
        }
        if (isWindows()) {
            dlls = "|" + PYTHON_DLLS;
        }
        if (!addSitePackages) {
            return PYTHON_LIB + "|" + PYTHON_LIB_DYNLOAD + dlls;
        } else {
            return PYTHON_LIB + "|" + PYTHON_LIB_DYNLOAD + "|" + PYTHON2_SITE_PACKAGES + dlls;
        }
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);

    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);

    }

    public static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);

    }

    static {
        try {
            String platform;
            platform = System.getenv("PYDEV_TEST_PLATFORM");
            if (platform == null) {
                if (isWindows()) {
                    platform = "windows";
                } else if (isUnix()) {
                    platform = "linux";
                } else if (isMac()) {
                    platform = "mac";
                } else {
                    platform = null;
                }
            }

            String propertiesFile = "undefined";
            if (platform != null) {
                propertiesFile = "org/python/pydev/core/TestDependent." + platform + ".properties";
                InputStream stream = TestDependent.class.getClassLoader().getResourceAsStream(propertiesFile);
                if (stream != null) {
                    //Initialize the static contents of the class.
                    String streamContents = FileUtils.getStreamContents(stream, null, null);
                    Properties props = PropertiesHelper.createPropertiesFromString(streamContents);
                    Map<String, String> map = PropertiesHelper.createMapFromProperties(props);
                    Set<Entry<String, String>> entrySet = map.entrySet();
                    for (Entry<String, String> entry : entrySet) {
                        String key = entry.getKey();
                        Field field = TestDependent.class.getField(key);
                        String value = entry.getValue().trim();

                        if ("true".equals(value)) {
                            field.set(null, true);
                        } else if ("false".equals(value)) {
                            field.set(null, false);
                        } else if ("null".equals(value) || "".equals(value)) {
                            field.set(null, null);
                        } else {
                            field.set(null, value);
                        }

                    }
                } else {
                    System.err.println("Could not get stream to: " + propertiesFile
                            + " to initialize TestDependent.java values.");
                }
            }

            //Checking and setting variables that do not exist (if possible).
            if (PYTHON_INSTALL == null) {
                System.err.println("PYTHON_INSTALL variable MUST be set in " + propertiesFile + " to run tests.");
            } else if (!new File(PYTHON_INSTALL).exists()) {
                System.err.println("PYTHON_INSTALL variable points to path that does NOT exist: " + PYTHON_INSTALL);
            }

            if (TEST_PYDEV_BASE_LOC == null) {
                System.err.println("TEST_PYDEV_BASE_LOC variable MUST be set in " + propertiesFile + " to run tests.");
            } else if (!new File(TEST_PYDEV_BASE_LOC).exists()) {
                System.err.println("TEST_PYDEV_BASE_LOC variable points to path that does NOT exist: "
                        + TEST_PYDEV_BASE_LOC);
            }

            if (PYTHON_EXE == null) {
                if (isWindows()) {
                    PYTHON_EXE = PYTHON_INSTALL + "python.exe";
                } else {
                    PYTHON_EXE = PYTHON_INSTALL + "python";
                }
            }
            if (!new File(PYTHON_EXE).exists()) {
                System.err.println("PYTHON_EXE variable points to path that does NOT exist: " + PYTHON_EXE);
            }

            if (CONDA_PYTHON_38_ENV != null) {
                if (!new File(CONDA_PYTHON_38_ENV).exists()) {
                    System.err.println(
                            "CONDA_PYTHON_38_ENV variable points to path that does NOT exist: " + CONDA_PYTHON_38_ENV);
                }
                if (!CONDA_PYTHON_38_ENV.endsWith("/")) {
                    throw new RuntimeException("Expecting CONDA_PYTHON_38_ENV to end with '/'");
                }
            }

            if (PYTHON_LIB == null) {
                PYTHON_LIB = PYTHON_INSTALL + "Lib/";
            }
            if (PYTHON_DLLS == null) {
                PYTHON_DLLS = PYTHON_INSTALL + "DLLs/";
            }
            if (PYTHON2_SITE_PACKAGES == null) {
                PYTHON2_SITE_PACKAGES = PYTHON_LIB + "site-packages/";
            }

            if (TEST_PYSRC_TESTING_LOC == null) {
                TEST_PYSRC_TESTING_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev/tests/pysrc/";
            }
            if (!TEST_PYSRC_TESTING_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYSRC_TESTING_LOC to end with '/'");
            }

            if (TEST_PYSRC_NAVIGATOR_LOC == null) {
                TEST_PYSRC_NAVIGATOR_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev/tests_navigator/pysrc/";
            }
            if (!TEST_PYSRC_NAVIGATOR_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYSRC_NAVIGATOR_LOC to end with '/'");
            }

            if (TEST_PYSRC_TESTING_LOC2 == null) {
                TEST_PYSRC_TESTING_LOC2 = TEST_PYDEV_BASE_LOC + "org.python.pydev/tests/pysrc2/";
            }
            if (!TEST_PYSRC_TESTING_LOC2.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYSRC_TESTING_LOC2 to end with '/'");
            }

            if (TEST_PYDEV_PLUGIN_LOC == null) {
                TEST_PYDEV_PLUGIN_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev/";
            }
            if (!TEST_PYDEV_PLUGIN_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYDEV_PLUGIN_LOC to end with '/'");
            }

            if (HELPERS_LOC == null) {
                HELPERS_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev.core/helpers/";
            }
            if (!HELPERS_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting HELPERS_LOC to end with '/'");
            }
            if (!new File(HELPERS_LOC).exists()) {
                throw new RuntimeException("HELPERS_LOC variable points to path that does NOT exist: "
                        + HELPERS_LOC);
            }

            if (PYSRC_LOC == null) {
                PYSRC_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev.core/pysrc/";
            }
            if (!PYSRC_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting PYSRC_LOC to end with '/'");
            }
            if (!new File(PYSRC_LOC).exists()) {
                throw new RuntimeException("PYSRC_LOC variable points to path that does NOT exist: "
                        + PYSRC_LOC);
            }

            if (TEST_PYDEV_DEBUG_PLUGIN_LOC == null) {
                TEST_PYDEV_DEBUG_PLUGIN_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev.debug/";
            }
            if (!TEST_PYDEV_DEBUG_PLUGIN_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYDEV_DEBUG_PLUGIN_LOC to end with '/'");
            }

            if (TEST_PYDEV_JYTHON_PLUGIN_LOC == null) {
                TEST_PYDEV_JYTHON_PLUGIN_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev.jython/";
            }
            if (!TEST_PYDEV_JYTHON_PLUGIN_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYDEV_JYTHON_PLUGIN_LOC to end with '/'");
            }

            if (TEST_PYDEV_PARSER_PLUGIN_LOC == null) {
                TEST_PYDEV_PARSER_PLUGIN_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev.parser/";
            }
            if (!TEST_PYDEV_PARSER_PLUGIN_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYDEV_PARSER_PLUGIN_LOC to end with '/'");
            }

            if (TEST_PYDEV_REFACTORING_PLUGIN_LOC == null) {
                TEST_PYDEV_REFACTORING_PLUGIN_LOC = TEST_PYDEV_BASE_LOC + "org.python.pydev.refactoring/";
            }
            if (!TEST_PYDEV_REFACTORING_PLUGIN_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_PYDEV_REFACTORING_PLUGIN_LOC to end with '/'");
            }

            if (TEST_COM_REFACTORING_PYSRC_LOC == null) {
                TEST_COM_REFACTORING_PYSRC_LOC = TEST_PYDEV_BASE_LOC
                        + "com.python.pydev.refactoring/tests/pysrcrefactoring/";
            }
            if (!TEST_COM_REFACTORING_PYSRC_LOC.endsWith("/")) {
                throw new RuntimeException("Expecting TEST_COM_REFACTORING_PYSRC_LOC to end with '/'");
            }

        } catch (Exception e) {
            System.err.println("--- Error getting contents to properly initialize TestDependent.java values ---");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println("now");
    }

}
