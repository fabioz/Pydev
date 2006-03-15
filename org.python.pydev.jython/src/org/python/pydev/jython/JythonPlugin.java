package org.python.pydev.jython;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.python.core.PyObject;
import org.python.pydev.core.REF;
import org.python.pydev.core.bundle.BundleInfo;
import org.python.pydev.core.bundle.IBundleInfo;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.util.PythonInterpreter;

/**
 * The main plugin class to be used in the desktop.
 */
public class JythonPlugin extends Plugin {
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
	private static PythonInterpreter staticInterpreter;
	
	/**
	 * The constructor.
	 */
	public JythonPlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
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

	public synchronized static PythonInterpreter getInterpreter(){
		synchronized (plugin) {
			if(staticInterpreter == null){
				staticInterpreter = new PythonInterpreter();
			}
		}
		return staticInterpreter;
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
	 * Holds a cache with the name of the created code to the file timestamp
	 */
	private static Map<File, Long> codeCache = new HashMap<File,Long>();
	
	/**
	 * This is a helper for:
	 * - Loading a file from the filesystem with jython code
	 * - Compiling it to a code object (that will remain in the 'code' local for the interpreter)
	 * - Making a call to exec that code
	 * - Returning the local in the interpreter regarded as jythonResult
	 * 
	 * Additional notes:
	 * - The code object will be regenerated only if:
	 * 		- It still didn't exist (dought!!)
	 * 		- The timestamp of the file changed
	 * 
	 * @param locals Those are the locals that should be added to the interpreter before calling the actual code
	 * @param fileToExec the file that should be executed (relative to the JythonPlugin jysrc folder)
	 * @param interpreter the interpreter that should be used to execute the code
	 * @return A boolean indicating the jythonResult value (nonzero). 
	 *         If it was null or some exception happened while executing it, false will be returned.
	 *         If further info is needed, the interpreter itself should be checked for return values
	 * 
	 */
	public static boolean exec(HashMap<String, Object> locals, String fileToExec, IPythonInterpreter interpreter) {
		File fileWithinJySrc = JythonPlugin.getFileWithinJySrc(fileToExec);
    	return exec(locals, interpreter, fileWithinJySrc);
	}

	/**
	 * @see JythonPlugin#exec(HashMap, String, PythonInterpreter)
	 * Same as before but the file to execute is passed as a parameter
	 */
	public static boolean exec(HashMap<String, Object> locals, IPythonInterpreter interpreter, File fileToExec) {
		try {
			for (Map.Entry<String, Object> entry : locals.entrySet()) {
				interpreter.set(entry.getKey(), entry.getValue());
			}
			
			boolean regenerate = false;
			Long timestamp = codeCache.get(fileToExec);
			if(timestamp == null || timestamp != fileToExec.lastModified()){
				//the file timestamp changed, so, we have to regenerate it
				regenerate = true;
				codeCache.put(fileToExec, fileToExec.lastModified());
			}
			
			if(!regenerate){
				//if the 'code' object does not exist, we have to regenerate it. 
				PyObject obj = interpreter.get("code");
				if (obj == null){
					regenerate = true;
				}
			}
			
			if(regenerate){
				String path = REF.getFileAbsolutePath(fileToExec);
                String loadFile = "" +
"f = open('"+fileToExec+"')\n" +
"try:                      \n" +
"    toExec = f.read()     \n" +
"finally:                  \n" +
"    f.close()             \n" +
                        "";
                interpreter.exec(loadFile);
				String exec = StringUtils.format("code = compile(toExec, '%s', 'exec')", path);
				interpreter.exec(exec);
			}
			
			interpreter.exec("exec(code)");
			PyObject obj = interpreter.get("jythonResult");
			if(obj == null){
				return false;
			}
			return obj.__nonzero__();
		} catch (Exception e) {
			Log.log(IStatus.ERROR, "Error while executing:"+fileToExec, e);
			return false;
		}
	}

	public static IPythonInterpreter newPythonInterpreter() {
		PythonInterpreterWrapper interpreter = new PythonInterpreterWrapper();
		interpreter.set("False", 0);
		interpreter.set("True", 1);
		return interpreter;
	}
}
