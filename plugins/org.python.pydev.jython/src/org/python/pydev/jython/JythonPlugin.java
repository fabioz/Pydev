/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.jython;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.python.core.PyClass;
import org.python.core.PyException;
import org.python.core.PyJavaType;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.pydev.core.NullOutputStream;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.ui.JyScriptingPreferencesPage;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.ConsoleColorCache;
import org.python.pydev.shared_ui.bundle.BundleInfo;
import org.python.pydev.shared_ui.bundle.IBundleInfo;
import org.python.util.PythonInterpreter;

/**
 * The main plugin class to be used in the desktop.
 */
public class JythonPlugin extends AbstractUIPlugin {

    private static final File[] EMPTY_FILES = new File[0];
    private static final boolean DEBUG = false;
    public static boolean DEBUG_RELOAD = true;

    /**
     * While in tests, errors are thrown even if we don't have a shared instance for JythonPlugin
     */
    public static boolean IN_TESTS = false;

    private static String LOAD_FILE_SCRIPT = "" +
            "print '--->  reloading', r'%s'\n" +
            "import sys                    \n" + //sys will always be on the namespace (so that we can set sys.path)
            "f = open(r'%s')               \n" +
            "try:                          \n" +
            "    toExec = f.read()         \n" +
            "finally:                      \n" +
            "    f.close()                 \n" +
            "%s                            \n" + //space to put the needed folders on sys.path
            "";

    public static synchronized void setDebugReload(boolean b) {
        if (b != DEBUG_RELOAD) {
            if (b == false) {
                LOAD_FILE_SCRIPT = "#" + LOAD_FILE_SCRIPT;
                //System.out.println(">>"+LOAD_FILE_SCRIPT+"<<");
            } else {
                LOAD_FILE_SCRIPT = LOAD_FILE_SCRIPT.substring(1);
                //System.out.println(">>"+LOAD_FILE_SCRIPT+"<<");
            }
            DEBUG_RELOAD = b;
        }
    }

    // ----------------- SINGLETON THINGS -----------------------------
    public static IBundleInfo info;

    public static IBundleInfo getBundleInfo() {
        if (JythonPlugin.info == null) {
            JythonPlugin.info = new BundleInfo(JythonPlugin.getDefault().getBundle());
        }
        return JythonPlugin.info;
    }

    public static void setBundleInfo(IBundleInfo b) {
        JythonPlugin.info = b;
    }

    // ----------------- END BUNDLE INFO THINGS --------------------------

    //The shared instance.
    private static JythonPlugin plugin;
    private static Bundle[] bundles;

    /**
     * The constructor.
     */
    public JythonPlugin() {
        plugin = this;
    }

    // ------------------------------------------
    /**
     * Classloader that knows about all the bundles...
     */
    public static class AllBundleClassLoader extends ClassLoader {

        private Bundle[] bundles;

        public AllBundleClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Class loadClass(String className) throws ClassNotFoundException {
            try {
                return super.loadClass(className);
            } catch (ClassNotFoundException e) {
                String searchedClass = e.getMessage();
                for (int i = 0; i < PACKAGES_MUST_START_WITH.length; i++) {
                    if (searchedClass.startsWith(PACKAGES_MUST_START_WITH[i])) {

                        // Look for the class from the bundles.
                        int len = bundles.length;
                        for (int j = 0; j < len; ++j) {
                            try {
                                Bundle bundle = bundles[j];
                                if (bundle.getState() == Bundle.ACTIVE) {
                                    return bundle.loadClass(className);
                                }
                            } catch (Throwable e2) {
                            }
                        }

                        break;
                    }
                }
                // Didn't find the class anywhere, rethrow e.
                throw e;
            }
        }

        /**
         * Only the packages listed here will be added as Jython packages (less memory allocated).
         */
        private final String[] PACKAGES_MUST_START_WITH = new String[] {
                "com.python.pydev",
                //"org.eclipse.ui",
                //"org.eclipse.core",
                //"org.eclipse.debug",
                "org.eclipse.jface",
                //"org.eclipse.swt",
                //"org.eclipse.text",
                "org.junit",
                //"org.python.pydev",
                "org.python",

                //No need to add those.
                //"javax.",
                //"java."
        };

        private boolean addPackageNames(Bundle bundle, List<String> addNamesToThisList, String commaSeparatedPackages) {
            boolean addedSomePackage = false;
            if (commaSeparatedPackages != null) {
                List<String> packageNames = StringUtils.split(commaSeparatedPackages, ',');
                int size = packageNames.size();
                for (int i = 0; i < size; ++i) {
                    String pname = packageNames.get(i).trim();

                    for (int j = 0; j < PACKAGES_MUST_START_WITH.length; j++) {
                        if (pname.startsWith(PACKAGES_MUST_START_WITH[j])) {
                            addedSomePackage = true;
                            addNamesToThisList.add(pname); // and the name
                            //System.out.println("Added "+bundle.getHeaders().get("Bundle-Name")+" - "+pname);
                            break; // and break inner for
                        }
                    }
                }
            }
            return addedSomePackage;
        }

        /**
         * @return the package names available for the passed bundles
         */
        public List<String> setBundlesAndGetPackageNames(Bundle[] bundles) {
            List<Bundle> acceptedBundles = new ArrayList<Bundle>();
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < bundles.length; ++i) {
                boolean addedSomePackage = false;
                Bundle bundle = bundles[i];
                Dictionary<String, String> headers = bundle.getHeaders();
                addedSomePackage |= addPackageNames(bundle, names, headers.get("Provide-Package"));
                addedSomePackage |= addPackageNames(bundle, names, headers.get("Export-Package"));
                if (addedSomePackage) {
                    acceptedBundles.add(bundle);
                }
            }
            this.bundles = acceptedBundles.toArray(new Bundle[acceptedBundles.size()]);
            //for(Bundle b:this.bundles){
            //    System.out.println("Accepted:"+b.getHeaders().get("Bundle-Name"));
            //}
            return names;
        }
    }

    /**
     * This method is called upon plug-in activation
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        bundles = context.getBundles();
    }

    private static final Object lock = new Object();

    private static void setupJython() {
        synchronized (lock) {
            if (bundles != null && plugin != null) {
                //initialize the Jython runtime
                Properties prop2 = new Properties();
                prop2.put("python.home", FileUtils.getFileAbsolutePath(plugin.getPluginRootDir()));
                prop2.put("python.path", FileUtils.getFileAbsolutePath(getJySrcDirFile()));
                prop2.put("python.console.encoding", "UTF-8"); // Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
                prop2.put("python.security.respectJavaAccessibility", "false"); //don't respect java accessibility, so that we can access protected members on subclasses
                try {
                    AllBundleClassLoader allBundleClassLoader = new AllBundleClassLoader(plugin.getClass()
                            .getClassLoader());

                    PySystemState.initialize(System.getProperties(), prop2, new String[0], allBundleClassLoader);
                    List<String> packageNames = allBundleClassLoader.setBundlesAndGetPackageNames(bundles);
                    int size = packageNames.size();
                    for (int i = 0; i < size; ++i) {
                        String name = packageNames.get(i);
                        if (name.contains("internal")) {
                            continue;
                        }
                        int iToSplit = name.indexOf(';');
                        if (iToSplit != -1) {
                            name = name.substring(0, iToSplit);
                        }
                        //System.out.println("Added: " + name);
                        PySystemState.add_package(name);
                    }
                } catch (Exception e) {
                    Log.log(e);
                } finally {
                    bundles = null;
                }
            }
        }
    }

    private File getPluginRootDir() {
        try {
            IPath relative = new Path(".");
            return getBundleInfo().getRelativePath(relative);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is called when the plug-in is stopped
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     */
    public static JythonPlugin getDefault() {
        return plugin;
    }

    public static File getJythonLibDir() {
        try {
            IPath relative = new Path("Lib");
            return getBundleInfo().getRelativePath(relative);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File getFileWithinJySrc(String f) {
        try {
            IPath relative = new Path("jysrc").addTrailingSeparator().append(f);
            return getBundleInfo().getRelativePath(relative);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the jysrc (org.python.pydev.jython/jysrc) directory
     */
    public static File getJySrcDirFile() {
        try {
            IPath relative = new Path("jysrc");
            return getBundleInfo().getRelativePath(relative);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is a helper for:
     * - Loading a file from the filesystem with jython code
     * - Compiling it to a code object (that will remain in the 'code' local for the interpreter)
     * - Making a call to exec that code
     * - Returning the local in the interpreter regarded as jythonResult
     *
     * Additional notes:
     * - The code object will be regenerated only if:
     *         - It still didn't exist (dought!!)
     *         - The timestamp of the file changed
     *
     * @param locals Those are the locals that should be added to the interpreter before calling the actual code
     * @param fileToExec the file that should be executed (relative to the JythonPlugin jysrc folder)
     * @param interpreter the interpreter that should be used to execute the code
     *
     # @note If further info is needed (after the run), the interpreter itself should be checked for return values
     * @return any error that happened while executing the script
     *
     */
    public static Throwable exec(HashMap<String, Object> locals, String fileToExec, IPythonInterpreter interpreter) {
        File fileWithinJySrc = JythonPlugin.getFileWithinJySrc(fileToExec);
        return exec(locals, interpreter, fileWithinJySrc, new File[] { fileWithinJySrc.getParentFile() });
    }

    public static List<Throwable> execAll(HashMap<String, Object> locals, final String startingWith,
            IPythonInterpreter interpreter) {
        //exec files beneath jysrc in org.python.pydev.jython
        File jySrc = JythonPlugin.getJySrcDirFile();
        File additionalScriptingLocation = JyScriptingPreferencesPage.getAdditionalScriptingLocation();

        return execAll(locals, startingWith, interpreter, new File[] { jySrc, additionalScriptingLocation }, null);

    }

    /**
     * Executes all the scripts beneath some folders
     * @param beneathFolders the folders we want to get the scripts from
     * @return the errors that occured while executing the scripts
     */
    public static List<Throwable> execAll(HashMap<String, Object> locals, final String startingWith,
            IPythonInterpreter interpreter, File[] beneathFolders, File[] additionalPythonpathFolders) {
        List<Throwable> errors = new ArrayList<Throwable>();

        ArrayList<File> pythonpath = new ArrayList<File>();
        pythonpath.addAll(Arrays.asList(beneathFolders));
        if (additionalPythonpathFolders != null) {
            pythonpath.addAll(Arrays.asList(additionalPythonpathFolders));
        }
        File[] pythonpathFolders = pythonpath.toArray(new File[pythonpath.size()]);

        for (File file : beneathFolders) {
            if (file != null) {
                if (!file.exists()) {
                    String msg = "The folder:" + file +
                            " does not exist and therefore cannot be used to " +
                            "find scripts to run starting with:" + startingWith;
                    Log.log(IStatus.ERROR, msg, null);
                    errors.add(new RuntimeException(msg));
                }
                File[] files = getFilesBeneathFolder(startingWith, file);
                for (File f : files) {
                    Throwable throwable = exec(locals, interpreter, f, pythonpathFolders);
                    if (throwable != null) {
                        errors.add(throwable);
                    }
                }
            }
        }
        return errors;
    }

    /**
     * List all the 'target' scripts available beneath some folder. A non-null array is always returned.
     */
    public static File[] getFilesBeneathFolder(final String startingWith, File jySrc) {
        File[] files = jySrc.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                String name = pathname.getName();
                if (name.startsWith(startingWith) && name.endsWith(".py")) {
                    return true;
                }
                return false;
            }

        });
        if (files == null) {
            files = EMPTY_FILES;
        }
        return files;
    }

    /**
     * Holds a cache with the name of the created code to a tuple with the file timestamp and the Code Object
     * that was generated with the contents of that timestamp.
     */
    private static Map<File, Tuple<Long, Object>> codeCache = new HashMap<File, Tuple<Long, Object>>();

    /**
     * @param pythonpathFolders folders that should be in the pythonpath when executing the script
     * @see JythonPlugin#exec(HashMap, String, PythonInterpreter)
     * Same as before but the file to execute is passed as a parameter
     */
    public static Throwable exec(HashMap<String, Object> locals, IPythonInterpreter interpreter, File fileToExec,
            File[] pythonpathFolders, String... argv) {
        if (locals == null) {
            locals = new HashMap<String, Object>();
        }
        if (interpreter == null) {
            return null; //already disposed
        }
        locals.put("__file__", fileToExec.toString());
        try {
            String codeObjName;
            synchronized (codeCache) { //hold on there... one at a time... please?
                String fileName = fileToExec.getName();
                if (!fileName.endsWith(".py")) {
                    throw new RuntimeException("The script to be executed must be a python file. Name:" + fileName);
                }
                codeObjName = "code" + fileName.substring(0, fileName.indexOf('.'));
                final String codeObjTimestampName = codeObjName + "Timestamp";

                for (Map.Entry<String, Object> entry : locals.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }

                boolean regenerate = false;
                if (interpreter instanceof PythonInterpreterWrapperNotShared) {
                    //Always regenerate if the state is not shared! (otherwise the pythonpath might be wrong as the sys is not the same)
                    regenerate = true;
                }

                Tuple<Long, Object> timestamp = codeCache.get(fileToExec);
                final long lastModified = FileUtils.lastModified(fileToExec);
                if (timestamp == null || timestamp.o1 != lastModified) {
                    //the file timestamp changed, so, we have to regenerate it
                    regenerate = true;
                }

                if (!regenerate) {
                    //if the 'code' object does not exist or if it's timestamp is outdated, we have to re-set it.
                    PyObject obj = interpreter.get(codeObjName);
                    PyObject pyTime = interpreter.get(codeObjTimestampName);
                    if (obj == null || pyTime == null || !pyTime.__tojava__(Long.class).equals(timestamp.o1)) {
                        if (DEBUG) {
                            System.out.println("Resetting object: " + codeObjName);
                        }
                        interpreter.set(codeObjName, timestamp.o2);
                        interpreter.set(codeObjTimestampName, timestamp.o1);
                    }
                }

                if (regenerate) {
                    if (DEBUG) {
                        System.out.println("Regenerating: " + codeObjName);
                    }
                    String path = FileUtils.getFileAbsolutePath(fileToExec);

                    StringBuffer strPythonPathFolders = new StringBuffer();
                    strPythonPathFolders.append("[");
                    for (File file : pythonpathFolders) {
                        if (file != null) {
                            strPythonPathFolders.append("r'");
                            strPythonPathFolders.append(FileUtils.getFileAbsolutePath(file));
                            strPythonPathFolders.append("',");
                        }
                    }
                    strPythonPathFolders.append("]");

                    StringBuffer addToSysPath = new StringBuffer();

                    //we will only add the paths to the pythonpath if it was still not set or if it changed (but it will never remove the ones added before).
                    addToSysPath.append("if not hasattr(sys, 'PYDEV_PYTHONPATH_SET') or sys.PYDEV_PYTHONPATH_SET != "); //we have to put that in sys because it is the same across different interpreters
                    addToSysPath.append(strPythonPathFolders);
                    addToSysPath.append(":\n");

                    addToSysPath.append("    sys.PYDEV_PYTHONPATH_SET = ");
                    addToSysPath.append(strPythonPathFolders);
                    addToSysPath.append("\n");

                    addToSysPath.append("    sys.path += ");
                    addToSysPath.append(strPythonPathFolders);
                    addToSysPath.append("\n");

                    if (argv.length > 0) {
                        addToSysPath.append("sys.argv = [");
                        for (String s : argv) {
                            addToSysPath.append(s);
                            addToSysPath.append(",");
                        }
                        addToSysPath.append("];");
                        addToSysPath.append("\n");
                    }

                    String toExec = StringUtils.format(LOAD_FILE_SCRIPT, path,
                            path,
                            addToSysPath.toString());
                    interpreter.exec(toExec);
                    String exec = StringUtils.format(
                            "%s = compile(toExec, r'%s', 'exec')", codeObjName, path);
                    interpreter.exec(exec);
                    //set its timestamp
                    interpreter.set(codeObjTimestampName, lastModified);

                    Object codeObj = interpreter.get(codeObjName);
                    codeCache.put(fileToExec, new Tuple<Long, Object>(lastModified, codeObj));
                }
            }

            interpreter.exec(StringUtils.format("exec(%s)", codeObjName));
        } catch (Throwable e) {
            if (!IN_TESTS && JythonPlugin.getDefault() == null) {
                //it is already disposed
                return null;
            }
            //the user requested it to exit
            if (e instanceof ExitScriptException) {
                return null;
            }
            //actually, this is more likely to happen when raising an exception in jython
            if (e instanceof PyException) {
                PyException pE = (PyException) e;
                if (pE.type instanceof PyJavaType) {
                    PyJavaType t = (PyJavaType) pE.type;
                    if (t.getName() != null && t.getName().equals("org.python.pydev.jython.ExitScriptException")) {
                        return null;
                    }
                } else if (pE.type instanceof PyClass) {
                    PyClass t = (PyClass) pE.type;
                    if (t.__name__ != null && t.__name__.equals("SystemExit")) {
                        return null;
                    }
                }
            }

            if (JyScriptingPreferencesPage.getShowScriptingOutput()) {
                Log.log(IStatus.ERROR, "Error while executing:" + fileToExec, e);
            }
            return e;
        }
        return null;
    }

    // -------------- static things

    /**
     * This is the console we are writing to
     */
    private static MessageConsole fConsole;
    private static IOConsoleOutputStream fOutputStream;
    private static IOConsoleOutputStream fErrorStream;

    /**
     * @return the console to use
     */
    private static MessageConsole getConsole() {
        try {
            if (fConsole == null) {
                fConsole = new MessageConsole("PyDev Scripting", JythonPlugin.getBundleInfo().getImageCache()
                        .getDescriptor("icons/python_scripting.png"));

                fOutputStream = fConsole.newOutputStream();
                fErrorStream = fConsole.newOutputStream();

                HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
                themeConsoleStreamToColor.put(fOutputStream, "console.output");
                themeConsoleStreamToColor.put(fErrorStream, "console.error");

                fConsole.setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);

                ConsoleColorCache.getDefault().keepConsoleColorsSynched(fConsole);

                ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { fConsole });
            }
            return fConsole;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static IPythonInterpreter newPythonInterpreter() {
        return newPythonInterpreter(true, true);
    }

    /**
     * Creates a new Python interpreter (with jython) and returns it.
     *
     * Note that if the sys is not shared, clients should be in a Thread for it to be really separate).
     */
    public static IPythonInterpreter newPythonInterpreter(boolean redirect, boolean shareSys) {
        setupJython(); //Important: setup the pythonpath for the jython process.

        IPythonInterpreter interpreter;
        if (shareSys) {
            interpreter = new PythonInterpreterWrapper();
        } else {
            interpreter = new PythonInterpreterWrapperNotShared();
        }
        if (redirect) {
            interpreter.setOut(new ScriptOutput(new ICallback0<IOConsoleOutputStream>() {

                public IOConsoleOutputStream call() {
                    getConsole(); //Just to make sure it's initialized.
                    return fOutputStream;
                }
            }));

            interpreter.setErr(new ScriptOutput(new ICallback0<IOConsoleOutputStream>() {

                public IOConsoleOutputStream call() {
                    getConsole(); //Just to make sure it's initialized.
                    return fErrorStream;
                }
            }));
        } else {
            interpreter.setErr(NullOutputStream.singleton);
            interpreter.setOut(NullOutputStream.singleton);
        }
        return interpreter;
    }
}
