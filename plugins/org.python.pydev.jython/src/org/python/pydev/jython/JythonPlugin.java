/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.jython;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.python.core.PyException;
import org.python.core.PyJavaClass;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.bundle.BundleInfo;
import org.python.pydev.core.bundle.IBundleInfo;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.ui.JyScriptingPreferencesPage;
import org.python.util.PythonInterpreter;

/**
 * The main plugin class to be used in the desktop.
 */
public class JythonPlugin extends AbstractUIPlugin {
    
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
    
    public static synchronized void setDebugReload(boolean b){
        if(b != DEBUG_RELOAD){
            if(b == false){
                LOAD_FILE_SCRIPT = "#"+LOAD_FILE_SCRIPT;
                //System.out.println(">>"+LOAD_FILE_SCRIPT+"<<");
            }else{
                LOAD_FILE_SCRIPT = LOAD_FILE_SCRIPT.substring(1);
                //System.out.println(">>"+LOAD_FILE_SCRIPT+"<<");
            }
            DEBUG_RELOAD = b;
        }
    }
    
    // ----------------- SINGLETON THINGS -----------------------------
    public static IBundleInfo info;
    public static IBundleInfo getBundleInfo(){
        if(JythonPlugin.info == null){
            JythonPlugin.info = new BundleInfo(JythonPlugin.getDefault().getBundle());
        }
        return JythonPlugin.info;
    }
    public static void setBundleInfo(IBundleInfo b){
        JythonPlugin.info = b;
    }
    // ----------------- END BUNDLE INFO THINGS --------------------------

    //The shared instance.
    private static JythonPlugin plugin;
    
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

        public AllBundleClassLoader(Bundle[] bundles, ClassLoader parent) {
            super(parent);
            this.bundles = bundles;
            setPackageNames(bundles);
        }

        @SuppressWarnings("unchecked")
        public Class loadClass(String className) throws ClassNotFoundException {
            try {
                return super.loadClass(className);
            } catch (ClassNotFoundException e) {
                // Look for the class from the bundles.
                for (int i = 0; i < bundles.length; ++i) {
                    try {
                        if(bundles[i].getState() == Bundle.ACTIVE){ 
                            return bundles[i].loadClass(className);
                        }
                    } catch (Throwable e2) {
                    }
                }
                // Didn't find the class anywhere, rethrow e.
                throw e;
            }
        }
        
        
        /**
         * The package names the bundles provide
         */
        private String[] packageNames;
        
        /**
         * Set the package names available given the bundles that we can access
         */
        private void setPackageNames(Bundle[] bundles) {
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < bundles.length; ++i) {
                String packages = (String) bundles[i].getHeaders().get("Provide-Package");
                if (packages != null) {
                    String[] pnames = packages.split(",");
                    for (int j = 0; j < pnames.length; ++j) {
                        names.add(pnames[j].trim());
                    }
                }
                packages = (String) bundles[i].getHeaders().get("Export-Package");
                if (packages != null) {
                    String[] pnames = packages.split(",");
                    for (int j = 0; j < pnames.length; ++j) {
                        names.add(pnames[j].trim());
                    }
                }
            }
            packageNames = (String[]) names.toArray(new String[names.size()]);
        }
        
        /**
         * @return the package names available for the passed bundles
         */
        public String[] getPackageNames() {
            return packageNames;
        }
    }    

    //------------------------------------------
    AllBundleClassLoader allBundleClassLoader;
    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        //initialize the Jython runtime
        Properties prop2 = new Properties();
        prop2.put("python.home", REF.getFileAbsolutePath(getPluginRootDir()));
        prop2.put("python.path", REF.getFileAbsolutePath(getJySrcDirFile()));
        prop2.put("python.security.respectJavaAccessibility", "false"); //don't respect java accessibility, so that we can access protected members on subclasses
        
        try {
            allBundleClassLoader = new AllBundleClassLoader(context.getBundles(), this.getClass().getClassLoader());
            PySystemState.initialize(System.getProperties(), prop2, new String[0], allBundleClassLoader);
            String[] packageNames = getDefault().allBundleClassLoader.getPackageNames();
            for (int i = 0; i < packageNames.length; ++i) {
                PySystemState.add_package(packageNames[i]);
            }
        } catch (Exception e) {
            Log.log(e);
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
    
    public static File getJythonLibDir(){
        try {
            IPath relative = new Path("Lib");
            return getBundleInfo().getRelativePath(relative);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static File getFileWithinJySrc(String f){
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
        return exec(locals, interpreter, fileWithinJySrc, new File[]{fileWithinJySrc.getParentFile()});
    }
    
    public static List<Throwable> execAll(HashMap<String, Object> locals, final String startingWith, 
            IPythonInterpreter interpreter) {
        //exec files beneath jysrc in org.python.pydev.jython
        File jySrc = JythonPlugin.getJySrcDirFile();
        File additionalScriptingLocation = JyScriptingPreferencesPage.getAdditionalScriptingLocation();
        
        return execAll(locals, startingWith, interpreter, new File[]{jySrc, additionalScriptingLocation}, null);
        
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
        if(additionalPythonpathFolders != null){
            pythonpath.addAll(Arrays.asList(additionalPythonpathFolders));
        }
        File[] pythonpathFolders = pythonpath.toArray(new File[pythonpath.size()]);
        
        for (File file : beneathFolders) {
            if(file != null){
                if(!file.exists()){
                    String msg = "The folder:"+file+" does not exist and therefore cannot be used to " +
                                                "find scripts to run starting with:"+startingWith;
                    Log.log(IStatus.ERROR, msg, null);
                    errors.add(new RuntimeException(msg));
                }
                File[] files = getFilesBeneathFolder(startingWith, file);
                if(files != null){
                    for(File f : files){
                        Throwable throwable = exec(locals, interpreter, f, pythonpathFolders);
                        if(throwable != null){
                            errors.add(throwable);
                        }
                    }
                }
            }
        }
        return errors;
    }
    
    /**
     * List all the 'target' scripts available beneath some folder.
     */
    public static File[] getFilesBeneathFolder(final String startingWith, File jySrc) {
        File[] files = jySrc.listFiles(new FileFilter(){

            public boolean accept(File pathname) {
                String name = pathname.getName();
                if(name.startsWith(startingWith) && name.endsWith(".py")){
                    return true;
                }
                return false;
            }
            
        });
        return files;
    }


    /**
     * Holds a cache with the name of the created code to a tuple with the file timestamp and the Code Object
     * that was generated with the contents of that timestamp.
     */
    private static Map<File, Tuple<Long, Object>> codeCache = new HashMap<File,Tuple<Long, Object>>();
    
    /**
     * @param pythonpathFolders folders that should be in the pythonpath when executing the script
     * @see JythonPlugin#exec(HashMap, String, PythonInterpreter)
     * Same as before but the file to execute is passed as a parameter
     */
    public static Throwable exec(HashMap<String, Object> locals, IPythonInterpreter interpreter, 
            File fileToExec, File[] pythonpathFolders, String ... argv) {
        if(locals == null){
            locals = new HashMap<String, Object>();
        }
        if(interpreter == null){
        	return null; //already disposed
        }
        locals.put("__file__", fileToExec.toString());
        try {
            String codeObjName;
            synchronized (codeCache) { //hold on there... one at a time... please?
                String fileName = fileToExec.getName();
                if(!fileName.endsWith(".py")){
                    throw new RuntimeException("The script to be executed must be a python file. Name:"+fileName);
                }
                codeObjName = "code"+fileName.substring(0, fileName.indexOf('.'));
                final String codeObjTimestampName = codeObjName+"Timestamp";
                
                for (Map.Entry<String, Object> entry : locals.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }
                
                boolean regenerate = false;
                if(interpreter instanceof PythonInterpreterWrapperNotShared){
                    //Always regenerate if the state is not shared! (otherwise the pythonpath might be wrong as the sys is not the same)
                    regenerate = true;
                }
                
                Tuple<Long, Object> timestamp = codeCache.get(fileToExec);
                final long lastModified = fileToExec.lastModified();
                if(timestamp == null || timestamp.o1 != lastModified){
                    //the file timestamp changed, so, we have to regenerate it
                    regenerate = true;
                }
                
                if(!regenerate){
                    //if the 'code' object does not exist or if it's timestamp is outdated, we have to re-set it. 
                    PyObject obj = interpreter.get(codeObjName);
                    PyObject pyTime = interpreter.get(codeObjTimestampName);
                    if (obj == null || pyTime == null || !pyTime.__tojava__(Long.class).equals(timestamp.o1)){
                        if(DEBUG){
                            System.out.println("Resetting object: "+codeObjName);
                        }
                        interpreter.set(codeObjName, timestamp.o2);
                        interpreter.set(codeObjTimestampName, timestamp.o1);
                    }
                }
                
                if(regenerate){
                    if(DEBUG){
                        System.out.println("Regenerating: "+codeObjName);
                    }
                    String path = REF.getFileAbsolutePath(fileToExec);
                    
                    StringBuffer strPythonPathFolders = new StringBuffer();
                    strPythonPathFolders.append("[");
                    for (File file : pythonpathFolders) {
                        if (file != null){
                            strPythonPathFolders.append("r'");
                            strPythonPathFolders.append(REF.getFileAbsolutePath(file));
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
                    
                    if(argv.length > 0){
                        addToSysPath.append("sys.argv = [");
                        for(String s:argv){
                            addToSysPath.append(s);
                            addToSysPath.append(",");
                        }
                        addToSysPath.append("];");
                        addToSysPath.append("\n");
                    }
                    
                    String toExec = StringUtils.format(LOAD_FILE_SCRIPT, path, path, addToSysPath.toString());
                    interpreter.exec(toExec);
                    String exec = StringUtils.format("%s = compile(toExec, r'%s', 'exec')", codeObjName, path);
                    interpreter.exec(exec);
                    //set its timestamp
                    interpreter.set(codeObjTimestampName, lastModified);
                    
                    Object codeObj = interpreter.get(codeObjName);
                    codeCache.put(fileToExec, new Tuple<Long, Object>(lastModified, codeObj));
                }
            }
            
            interpreter.exec(StringUtils.format("exec(%s)" , codeObjName));
        } catch (Throwable e) {
            if(!IN_TESTS && JythonPlugin.getDefault() == null){
                //it is already disposed
                return null;
            }
            //the user requested it to exit
            if(e instanceof ExitScriptException){
                return null;
            }
            //actually, this is more likely to happen when raising an exception in jython
            if(e instanceof PyException){
                PyException pE = (PyException) e;
                if (pE.type instanceof PyJavaClass){
                    PyJavaClass t = (PyJavaClass) pE.type;
                    if(t.__name__ != null && t.__name__.equals("org.python.pydev.jython.ExitScriptException")){
                        return null;
                    }
                }
            }
            
            if(JyScriptingPreferencesPage.getShowScriptingOutput()){
                Log.log(IStatus.ERROR, "Error while executing:"+fileToExec, e);
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
                fConsole = new MessageConsole(
                		"PyDev Scripting", JythonPlugin.getBundleInfo().getImageCache().getDescriptor("icons/python_scripting.png"));
                
                fOutputStream = fConsole.newOutputStream();
                fErrorStream = fConsole.newOutputStream();
                
    			HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
    			themeConsoleStreamToColor.put(fOutputStream, "console.output");
    			themeConsoleStreamToColor.put(fErrorStream, "console.error");

                fConsole.setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);

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
        IPythonInterpreter interpreter;
        if(shareSys){
            interpreter = new PythonInterpreterWrapper();
        }else{
            interpreter = new PythonInterpreterWrapperNotShared();
        }
        if(redirect){
            MessageConsole console = getConsole();
			interpreter.setOut(new ScriptOutput(console, fOutputStream));
            interpreter.setErr(new ScriptOutput(console, fErrorStream));
        }
        interpreter.set("False", 0);
        interpreter.set("True", 1);
        return interpreter;
    }
    

    public static IInteractiveConsole newInteractiveConsole() {
        return new InteractiveConsoleWrapper();
    }
    

}
