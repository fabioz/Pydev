/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;


public class InterpreterInfo implements IInterpreterInfo{
    
    /**
     * For jython, this is the jython.jar
     * 
     * For python, this is the path to the python executable 
     */
    public String executableOrJar; 
    
    /**
     * folders - they should be passed to the pythonpath
     */
    public java.util.List<String> libs = new ArrayList<String>(); 
    
    /**
     * Those libraries are not really used in python (they are found in the system pythonpath), and 
     * don't need to be passed in the pythonpath (they are only here so that the user can see
     * this information)
     *  
     * files: .pyd, .dll, etc.
     * 
     * for jython, the jars should appear here.
     * 
     * This attribute is simply not used, so, it is deprecated (it's only maintained for backward
     * compatibility of the string representation of the InterpreterInfo, but should not be
     * used anywhere.)
     */
    @Deprecated
    private java.util.List<String> dllLibs = new ArrayList<String>(); 
    
    /**
     * __builtin__, os, math, etc for python 
     * 
     * check sys.builtin_module_names and others that should
     * be forced to use code completion as builtins, such os, math, etc.
     */
    private Set<String> forcedLibs = new TreeSet<String>(); 
    
    /**
     * This is the cache for the builtins (that's the same thing as the forcedLibs, but in a different format,
     * so, whenever the forcedLibs change, this should be changed too). 
     */
    private String[] builtinsCache;
    
    /**
     * module management for the system is always binded to an interpreter (binded in this class)
     * 
     * The modules manager is no longer persisted. It is restored from a separate file, because we do
     * not want to keep it in the 'configuration', as a giant Base64 string.
     */
    private ISystemModulesManager modulesManager;
    
    /**
     * This callback is only used in tests, to configure the paths that should be chosen after the interpreter is selected.
     */
    public static ICallback<Boolean, Tuple<List<String>, List<String>>> configurePathsCallback = null;

    /**
     * This is the version for the python interpreter (it is regarded as a String with Major and Minor version
     * for python in the format '2.5' or '2.4'.
     */
    private String version;


    /**
     * Sets the modules manager that should be used in this interpreter info.
     * 
     * @param modulesManager the modules manager that is contained within this info.
     * 
     * @note: the side-effect of this method is that it sets in the modules manager that this is the
     * info that it should use.
     */
    public void setModulesManager(ISystemModulesManager modulesManager) {
        modulesManager.setInfo(this);
        this.modulesManager = modulesManager;
    }

    public ISystemModulesManager getModulesManager() {
        return modulesManager;
    }

    /**
     * @return the pythonpath to be used (only the folders)
     */
    public List<String> getPythonPath() {
        ArrayList<String> ret = new ArrayList<String>();
        ret.addAll(libs);
        return ret;
    }
    
    public InterpreterInfo(String version, String exe, Collection<String> libs0){
        this.executableOrJar = exe;
        this.version = version;
        //deprecated (keep it cleared)
	    dllLibs.clear();

        setModulesManager(new SystemModulesManager());
        libs.addAll(libs0);
    }
    
    public InterpreterInfo(String version, String exe, Collection<String> libs0, Collection<String> dlls){
        this(version, exe, libs0);
    }
    
    public InterpreterInfo(String version, String exe, List<String> libs0, List<String> dlls, List<String> forced) {
        this(version, exe, libs0, dlls);
        forcedLibs.addAll(forced);
        this.builtinsCache = null; //force cache recreation
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof InterpreterInfo)){
            return false;
        }

        InterpreterInfo info = (InterpreterInfo) o;
        if(info.executableOrJar.equals(this.executableOrJar) == false){
            return false;
        }
        
        if(info.libs.equals(this.libs) == false){
            return false;
        }
        
        if (info.forcedLibs.equals(this.forcedLibs) == false){
            return false;
        }
        
        return true;
    }

    public static InterpreterInfo fromString(String received) {
        return fromString(received, true);
    }
    
    /**
     * Format we receive should be:
     * 
     * Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2
     * 
     * or
     * 
     * Version2.5Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2
     * (added only when version 2.5 was added, so, if the string does not have it, it is regarded as 2.4)
     * 
     * Symbols ': @ $'
     */
    public static InterpreterInfo fromString(String received, boolean askUserInOutPath) {
        if(received.toLowerCase().indexOf("executable") == -1){
            throw new RuntimeException("Unable to recreate the Interpreter info (Its format changed. Please, re-create your Interpreter information).Contents found:"+received);
        }
    	received = received.replaceAll("\n", "").replaceAll("\r", "");
        String[] forcedSplit = received.split("\\$");
        String[] libsSplit = forcedSplit[0].split("\\@");
        String exeAndLibs = libsSplit[0];
        String version = "2.4"; //if not found in the string, the grammar version is regarded as 2.4 
        
        String[] exeAndLibs1 = exeAndLibs.split("\\|");
        
        String exeAndVersion = exeAndLibs1[0];
        String lowerExeAndVersion = exeAndVersion.toLowerCase();
        if(lowerExeAndVersion.startsWith("version")){
            int execut = lowerExeAndVersion.indexOf("executable");
            version = exeAndVersion.substring(0,execut).substring(7);
            exeAndVersion = exeAndVersion.substring(7+version.length());
        }
        String executable = exeAndVersion.substring(exeAndVersion.indexOf(":")+1, exeAndVersion.length());
        
        
        
        final ArrayList<String> l = new ArrayList<String>();
        final ArrayList<String> toAsk = new ArrayList<String>();
        for (int i = 1; i < exeAndLibs1.length; i++) { //start at 1 (0 is exe)
            String trimmed = exeAndLibs1[i].trim();
            if(trimmed.length() > 0){
                if(trimmed.endsWith("OUT_PATH")){
                    trimmed = trimmed.substring(0, trimmed.length()-8);
                    if(askUserInOutPath){
                        toAsk.add(trimmed);
                    }else{
                        //is out by 'default'
                    }
                    
                }else if(trimmed.endsWith("INS_PATH")){
                    trimmed = trimmed.substring(0, trimmed.length()-8);
                    if(askUserInOutPath){
                        toAsk.add(trimmed);
                        l.add(trimmed);
                    }else{
                        l.add(trimmed);
                    }
                }else{
                    l.add(trimmed);
                }
            }
        }

        final Boolean[] result = new Boolean[]{true}; //true == OK, false == CANCELLED
        if(ProjectModulesManager.IN_TESTS){
        	if(InterpreterInfo.configurePathsCallback != null){
        		InterpreterInfo.configurePathsCallback.call(new Tuple<List<String>, List<String>>(toAsk, l));
        	}
        }else{
	        if(toAsk.size() > 0){
	            Runnable runnable = new Runnable(){
	
	                public void run() {
	                    ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), toAsk, 
	                            new IStructuredContentProvider(){
	
	                                @SuppressWarnings("unchecked")
	                                public Object[] getElements(Object inputElement) {
	                                    List<String> elements = (List<String>) inputElement;
	                                    return elements.toArray(new String[0]);
	                                }
	
	                                public void dispose() {
	                                }
	
	                                public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	                                }},
	                                
	                                
	                                new ILabelProvider(){
	
	                                public Image getImage(Object element) {
	                                    return PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM);
	                                }
	
	                                public String getText(Object element) {
	                                    return element.toString();
	                                }
	
	                                public void addListener(ILabelProviderListener listener) {
	                                }
	
	                                public void dispose() {
	                                }
	
	                                public boolean isLabelProperty(Object element, String property) {
	                                    return true;
	                                }
	
	                                public void removeListener(ILabelProviderListener listener) {
	                                }}, 
	                                
	                            "Select the folders to be added to the SYSTEM pythonpath!\n" +
	                            "\n" +
	                            "IMPORTANT: The folders for your PROJECTS should NOT be added here, but in your project configuration.\n\n" +
	                            "Check:http://fabioz.com/pydev/manual_101_interpreter.html for more details.");
	                    dialog.setInitialSelections(l.toArray(new String[0]));
	                    int i = dialog.open();
                        if(i == Window.OK){
                            result[0] = true;
    	                    Object[] result = dialog.getResult();
    	                    l.clear();
    	                    for (Object string : result) {
    	                        l.add((String) string);
    	                    }
                        }else{
                            result[0] = false;
                            
                        }
	                    
	                }
	                
	            };
	            try{
	                RunInUiThread.sync(runnable);
	            }catch(NoClassDefFoundError e){
	            }catch(UnsatisfiedLinkError e){
	                //this means that we're running unit-tests, so, we don't have to do anything about it
	                //as 'l' is already ok.
	            }
	        }
        }

        if(result[0] == false){
            //cancelled by the user
            return null;
        }
        
        ArrayList<String> l1 = new ArrayList<String>();
        if(libsSplit.length > 1){
	        String dllLibs = libsSplit[1];
	        String[] dllLibs1 = dllLibs.split("\\|");
	        for (int i = 0; i < dllLibs1.length; i++) {
	            String trimmed = dllLibs1[i].trim();
	            if(trimmed.length() > 0){
	                l1.add(trimmed);
	            }
	        }
        }
	        
        ArrayList<String> l2 = new ArrayList<String>();
        if(forcedSplit.length > 1){
	        String forcedLibs = forcedSplit[1];
	        String[] forcedLibs1 = forcedLibs.split("\\|");
	        for (int i = 0; i < forcedLibs1.length; i++) {
	            String trimmed = forcedLibs1[i].trim();
	            if(trimmed.length() > 0){
	                l2.add(trimmed);
	            }
	        }
        }	            
        return new InterpreterInfo(version, executable, l, l1, l2);
	        
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("Version");
        buffer.append(version);
        buffer.append("Executable:");
        buffer.append(executableOrJar);
        for (Iterator<String> iter = libs.iterator(); iter.hasNext();) {
            buffer.append("|");
            buffer.append(iter.next().toString());
        }
        buffer.append("@");
        
        buffer.append("$");
        if(forcedLibs.size() > 0){
	        for (Iterator<String> iter = forcedLibs.iterator(); iter.hasNext();) {
	            buffer.append("|");
	            buffer.append(iter.next().toString());
	        }
        }
        
        return buffer.toString();
    }

    /**
     * Adds the compiled libs (dlls)
     */
    public void restoreCompiledLibs(IProgressMonitor monitor) {
	    //the compiled with the interpreter should be already gotten.
	    forcedLibs.add("os"); //we have it in source, but want to interpret it, source info (ast) does not give us much
        
        //as it is a set, there is no problem to add it twice
	    forcedLibs.add("__builtin__"); //jython bug: __builtin__ is not added
	    forcedLibs.add("sys"); //jython bug: sys is not added
	    forcedLibs.add("email"); //email has some lazy imports that pydev cannot handle through the source
        

        if(isJythonInfo()){
            //by default, we don't want to force anything to python.
            forcedLibs.add("StringIO"); //jython bug: StringIO is not added
        }else{
            //those are sources, but we want to get runtime info on them.
            forcedLibs.add("OpenGL");
            forcedLibs.add("wxPython");
            forcedLibs.add("wx");
        }
        this.builtinsCache = null; //force cache recreation
    }

    /**
     * Restores the path given non-standard libraries
     * @param path
     */
    public void restorePythonpath(String path, IProgressMonitor monitor) {
        //no managers involved here...
        getModulesManager().changePythonPath(path, null, monitor, null);
    }
    
    /**
     * Restores the path with the discovered libs
     * @param path
     */
    public void restorePythonpath(IProgressMonitor monitor) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<String> iter = libs.iterator(); iter.hasNext();) {
            String folder = (String) iter.next();
            buffer.append(folder);
            buffer.append("|");
        }
        restorePythonpath(buffer.toString(), monitor);
    }
    
    /**
     * @return whether this info belongs to jython 
     */
    public boolean isJythonInfo() {
        return isJythonExecutable(executableOrJar);
    }

    /**
     * @param executable the executable we want to know about
     * @return if the executable is the jython jar.
     */
    public static boolean isJythonExecutable(String executable) {
        if (executable.endsWith("\"")) {
            return executable.endsWith(".jar\"");
        }
        return executable.endsWith(".jar");
    }

    public String getExeAsFileSystemValidPath() {
        //   /\:*?"<>|
        char[] invalidChars = new char[]{
                '/',
                '\\',
                ':',
                '*',
                '?',
                '"',
                '<',
                '>',
                '|'};
        String systemValid = new String(REF.encodeBase64(executableOrJar.getBytes()));
        for (char c : invalidChars) {
            systemValid = systemValid.replace(c, '_');
        }
        return systemValid;
    }

    public String getVersion() {
        return version;
    }
 
    public int getGrammarVersion() {
        int grammarVersion = IPythonNature.GRAMMAR_PYTHON_VERSION_2_4;
        if(getVersion().equals("2.5")){
            grammarVersion = IPythonNature.GRAMMAR_PYTHON_VERSION_2_5;
        }
        return grammarVersion;
    }
    
    

    //START: Things related to the builtins (forcedLibs) ---------------------------------------------------------------
    public String[] getBuiltins() {
        if(this.builtinsCache == null){
            this.builtinsCache = forcedLibs.toArray(new String[0]);
        }
        return this.builtinsCache;
    }

    public void addForcedLib(String forcedLib) {
        this.forcedLibs.add(forcedLib);
        this.builtinsCache = null;
    }

    public void removeForcedLib(String forcedLib) {
        this.forcedLibs.remove(forcedLib);
        this.builtinsCache = null;
    }

    public Iterator<String> forcedLibsIterator() {
        return forcedLibs.iterator();
    }
    //END: Things related to the builtins (forcedLibs) -----------------------------------------------------------------
    
}