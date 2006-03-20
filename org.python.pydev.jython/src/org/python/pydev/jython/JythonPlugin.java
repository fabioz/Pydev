package org.python.pydev.jython;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.python.core.PyObject;
import org.python.core.PySystemState;
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
	private class AllBundleClassLoader extends ClassLoader {

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
						return bundles[i].loadClass(className);
					} catch (ClassNotFoundException e2) {
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
		
		try {
			allBundleClassLoader = new AllBundleClassLoader(context.getBundles(), this.getClass().getClassLoader());
			PySystemState.initialize(System.getProperties(), null, new String[] { "" }, allBundleClassLoader);
			String[] packageNames = getDefault().allBundleClassLoader.getPackageNames();
			for (int i = 0; i < packageNames.length; ++i) {
				PySystemState.add_package(packageNames[i]);
			}
		} catch (Exception e) {
			Log.log(e);
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
	 *         
	 # @note If further info is needed (after the run), the interpreter itself should be checked for return values
	 * 
	 */
	public static void exec(HashMap<String, Object> locals, String fileToExec, IPythonInterpreter interpreter) {
		File fileWithinJySrc = JythonPlugin.getFileWithinJySrc(fileToExec);
    	exec(locals, interpreter, fileWithinJySrc);
	}
	
	public static void execAll(HashMap<String, Object> locals, final String startingWith, IPythonInterpreter interpreter) {
		File jySrc = JythonPlugin.getJySrcDirFile();
		File[] files = jySrc.listFiles(new FileFilter(){

			public boolean accept(File pathname) {
				if(pathname.getName().startsWith(startingWith)){
					return true;
				}
				return false;
			}
			
		});
		for(File f : files){
			exec(locals, interpreter, f);
		}
	}

	/**
	 * @see JythonPlugin#exec(HashMap, String, PythonInterpreter)
	 * Same as before but the file to execute is passed as a parameter
	 */
	public static void exec(HashMap<String, Object> locals, IPythonInterpreter interpreter, File fileToExec) {
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
						"f = open(r'%s')           \n" +
						"try:                      \n" +
						"    toExec = f.read()     \n" +
						"finally:                  \n" +
						"    f.close()             \n" +
                        "";
                String toExec = StringUtils.format(loadFile, path);
                REF.writeStrToFile("Will exec:>>"+toExec+"<<", "c:/temp/test.txt");
                interpreter.exec(toExec);
				String exec = StringUtils.format("code = compile(toExec, r'%s', 'exec')", path);
				interpreter.exec(exec);
			}
			
			interpreter.exec("exec(code)");
		} catch (Exception e) {
			Log.log(IStatus.ERROR, "Error while executing:"+fileToExec, e);
		}
	}

	public static IPythonInterpreter newPythonInterpreter() {
		PythonInterpreterWrapper interpreter = new PythonInterpreterWrapper();
		interpreter.set("False", 0);
		interpreter.set("True", 1);
		return interpreter;
	}
}
