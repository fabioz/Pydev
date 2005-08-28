/*
 * Created on 29/07/2005
 */
package org.python.pydev.core;

public class TestDependent {

    //NOTE: this should be gotten from some variable to point to the python lib (less system dependence, but still, some).
    public static String PYTHON_EXE="C:/bin/Python24/python.exe";
    public static final String PYTHON_INSTALL="C:/bin/Python24/";
    public static final String PYTHON_LIB="C:/bin/Python24/Lib/";
    public static final String PYTHON_SITE_PACKAGES="C:/bin/Python24/Lib/site-packages/";
    public static final String PYTHON_WXPYTHON_PACKAGES="C:/bin/Python24/Lib/site-packages/wx-2.6-msw-ansi";
    
    //NOTE: this should set to the tests pysrc location, so that it can be added to the pythonpath.
    public static final String TEST_PYSRC_LOC="D:/eclipse_workspace/org.python.pydev/tests/pysrc/";

    //"C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" "-Dpython.home=C:\bin\jython21" 
    //-classpath "C:\bin\jython21\jython.jar;%CLASSPATH%" org.python.util.jython %ARGS%
    public static final String JAVA_LOCATION="C:/Program Files/Java/jdk1.5.0_04/bin/java.exe";
    public static final String JAVA_RT_JAR_LOCATION= "C:/Program Files/Java/jdk1.5.0_04/jre/lib/rt.jar";
    
    public static final String JYTHON_JAR_LOCATION="C:/bin/jython21/jython.jar";
    public static final String JYTHON_LIB_LOCATION="C:/bin/jython21/lib";
    
    //we cannot test what we don't have...
    public static final boolean HAS_WXPYTHON_INSTALLED = true;
    public static final boolean HAS_QT_INSTALLED = false;
    public static final boolean HAS_GLU_INSTALLED = false;
    public static final boolean HAS_SWT_ON_PATH = false;
    
}
