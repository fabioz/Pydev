/*
 * Create a file TestDependent.java with the contents in this file and substitute the
 * values as needed...
 */
package org.python.pydev.core;

public class TestDependent {

    //NOTE: this should be gotten from some variable to point to the python lib (less system dependence, but still, some).
    public static String PYTHON_EXE="/usr/bin/python";
    public static final String PYTHON_LIB="/usr/lib/python2.4/";
    public static final String PYTHON_LIB_DYNLOAD="/usr/lib/python2.4/lib-dynload/";
    public static final String PYTHON_SITE_PACKAGES="/usr/lib/python2.4/site-packages/";

	public static String GetCompletePythonLib(boolean addSitePackages) {
		if(!addSitePackages){
			return PYTHON_LIB+"|"+PYTHON_LIB_DYNLOAD;
		}else{
			return PYTHON_LIB+"|"+PYTHON_LIB_DYNLOAD+"|"+PYTHON_SITE_PACKAGES;
		}
	}

    public static final String PYTHON_WXPYTHON_PACKAGES=null;
    public static final String PYTHON_NUMARRAY_PACKAGES=null;
    
    
    //NOTE: this should set to the tests pysrc location, so that it can be added to the pythonpath.
    public static final String TEST_PYDEV_BASE_LOC = "/home/fabioz/dev/";
    public static final String TEST_PYSRC_LOC=TEST_PYDEV_BASE_LOC+"org.python.pydev/tests/pysrc/";
    public static final String TEST_PYSRC_LOC2=TEST_PYDEV_BASE_LOC+"org.python.pydev/tests/pysrc2/";
    public static final String TEST_PYDEV_PLUGIN_LOC = TEST_PYDEV_BASE_LOC+"org.python.pydev/";
    public static final String TEST_PYDEV_JYTHON_PLUGIN_LOC = TEST_PYDEV_BASE_LOC+"org.python.pydev.jython/";
    public static final String TEST_PYDEV_PARSER_PLUGIN_LOC = TEST_PYDEV_BASE_LOC+"org.python.pydev.parser/";
    public static final String TEST_PYDEV_DEBUG_PLUGIN_LOC = TEST_PYDEV_BASE_LOC+"org.python.pydev.debug/";
    public static final String TEST_PYDEV_REFACTORING_PLUGIN_LOC = TEST_PYDEV_BASE_LOC+"org.python.pydev.refactoring/";
    public static final String TEST_PYSRC_NAVIGATOR_LOC = TEST_PYDEV_PLUGIN_LOC+"tests_navigator/pysrc/";

    //java info 
    public static final String JAVA_LOCATION="/home/fabioz/bin/jdk1.5.0_11/jre/bin/java";
    public static final String JAVA_RT_JAR_LOCATION= "/home/fabioz/bin/jdk1.5.0_11/jre/lib/rt.jar";
    
    public static final String JYTHON_JAR_LOCATION="/home/fabioz/bin/jython2.2rc3/jython.jar";
    public static final String JYTHON_LIB_LOCATION="/home/fabioz/bin/jython2.2rc3/lib/";
    
    //we cannot test what we don't have...
    public static final boolean HAS_WXPYTHON_INSTALLED = false;
    public static final boolean HAS_QT_INSTALLED = false;
    public static final boolean HAS_GLU_INSTALLED = false;
    public static final boolean HAS_SWT_ON_PATH = false;
    public static final boolean HAS_NUMARRAY_INSTALLED = false;
	public static final boolean HAS_MX_DATETIME = false;
	public static final boolean HAS_NUMPY_INSTALLED = false;

	public static final String PYTHON_MX_PACKAGES = null;
	public static final String PYTHON_PIL_PACKAGES = null;
	public static final String PYTHON_NUMPY_PACKAGES = null;

	
	public static final boolean HAS_PIL = false;
	public static boolean HAS_CYGWIN = false;
	public static String CYGWIN_PYTHON_EXE="E:/install/Utils.Cygwin/bin/python2.4.exe";
	public static final String CYGWIN_CYGPATH_LOCATION = "c:/bin/cygwin/bin/cygpath.exe";
	public static final String CYGWIN_UNIX_CYGPATH_LOCATION = "/usr/bin/cygpath.exe";
	public static final boolean HAS_PYTHON_TESTS = false;
}
