/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.UIConstants;


public class InterpreterInfo implements IInterpreterInfo{
    
    /**
     * For jython, this is the jython.jar
     * 
     * For python, this is the path to the python executable 
     */
    public volatile String executableOrJar; 
    
    public String getExecutableOrJar() {
        return executableOrJar;
    }
    
    /**
     * folders - they should be passed to the pythonpath
     */
    public final java.util.List<String> libs = new ArrayList<String>(); 
    
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
    private final Set<String> forcedLibs = new TreeSet<String>(); 
    
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
    private final String version;

    /**
     * This are the environment variables that should be used when this interpreter is specified.
     * May be null if no env. variables are specified.
     */
    private String[] envVariables;
    
    /**
     * This is the way that the interpreter should be referred. Can be null (in which case the executable is
     * used as the name)
     */
    private String name;

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
        this(version, exe, libs0, dlls, forced, null);
    }

    public InterpreterInfo(String version, String exe, List<String> libs0, List<String> dlls, List<String> forced, List<String> envVars) {
        this(version, exe, libs0, dlls);
        forcedLibs.addAll(forced);
        
        if(envVars == null){
            this.setEnvVariables(null);
        }else{
            this.setEnvVariables(envVars.toArray(new String[envVars.size()]));
        }
        
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
        
        if(this.envVariables != null){
            if(info.envVariables == null){
                return false;
            }
            //both not null
            if(!Arrays.equals(this.envVariables, info.envVariables)){
                return false;
            }
        }else{
            //env is null -- the other must be too
            if(info.envVariables != null){
                return false;
            }
        }
        
        
        return true;
    }

    public static InterpreterInfo fromString(String received) {
        return fromString(received, true);
    }
    
    /**
     * Format we receive should be:
     * 
     * Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2
     * 
     * or
     * 
     * Version2.5Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2
     * (added only when version 2.5 was added, so, if the string does not have it, it is regarded as 2.4)
     * 
     * or
     * 
     * Name:MyInterpreter:EndName:Version2.5Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2
     * 
     * Symbols ': @ $'
     */
    public static InterpreterInfo fromString(String received, boolean askUserInOutPath) {
        if(received.toLowerCase().indexOf("executable") == -1){
            throw new RuntimeException("Unable to recreate the Interpreter info (Its format changed. Please, re-create your Interpreter information).Contents found:"+received);
        }
        received = received.replaceAll("\n", "").replaceAll("\r", "");
        String name=null;
        if(received.startsWith("Name:")){
            int endNameIndex = received.indexOf(":EndName:");
            if(endNameIndex != -1){
                name = received.substring("Name:".length(), endNameIndex);
                received = received.substring(endNameIndex+":EndName:".length());
            }
            
        }
        
        Tuple<String, String> envVarsSplit = StringUtils.splitOnFirst(received, '^');
        Tuple<String, String> forcedSplit = StringUtils.splitOnFirst(envVarsSplit.o1, '$');
        Tuple<String, String> libsSplit = StringUtils.splitOnFirst(forcedSplit.o1, '@');
        String exeAndLibs = libsSplit.o1;
        
        
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
        if(libsSplit.o2.length() > 1){
            fillList(libsSplit, l1);
        }
            
        ArrayList<String> l2 = new ArrayList<String>();
        if(forcedSplit.o2.length() > 1){
            fillList(forcedSplit, l2);
        }    
        
        ArrayList<String> l3 = new ArrayList<String>();
        if(envVarsSplit.o2.length() > 1){
            fillList(envVarsSplit, l3);
        }
        InterpreterInfo info = new InterpreterInfo(version, executable, l, l1, l2, l3);
        info.setName(name);
        return info;
    }

    
    private static void fillList(Tuple<String, String> forcedSplit, ArrayList<String> l2) {
        String forcedLibs = forcedSplit.o2;
        for (String trimmed:StringUtils.split(forcedLibs, '|')) {
            trimmed = trimmed.trim();
            if(trimmed.length() > 0){
                l2.add(trimmed);
            }
        }
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        if(this.name != null){
            buffer.append("Name:");
            buffer.append(this.name);
            buffer.append(":EndName:");
        }
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
        
        if(this.envVariables != null){
            buffer.append("^");
            for(String s:envVariables){
                buffer.append(s);
                buffer.append("|");
            }
        }
        
        
        return buffer.toString();
    }

    /**
     * Adds the compiled libs (dlls)
     */
    public void restoreCompiledLibs(IProgressMonitor monitor) {
        //the compiled with the interpreter should be already gotten.
        
        //we have it in source, but want to interpret it, source info (ast) does not give us much
        forcedLibs.add("os"); 
        
        
        //we also need to add this submodule (because even though it's documented as such, it's not really 
        //implemented that way with a separate file -- there's black magic to put it there)
        forcedLibs.add("os.path"); 
        
        //as it is a set, there is no problem to add it twice
        if(this.version.startsWith("2") || this.version.startsWith("1")){
            //don't add it for 3.0 onwards.
            forcedLibs.add("__builtin__"); //jython bug: __builtin__ is not added
        }
        forcedLibs.add("sys"); //jython bug: sys is not added
        forcedLibs.add("email"); //email has some lazy imports that pydev cannot handle through the source
        

        if(isJythonInfo()){
            //by default, we don't want to force anything to python.
            forcedLibs.add("StringIO"); //jython bug: StringIO is not added
            forcedLibs.add("re"); //re is very strange in Jython (while it's OK in Python)
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
        getModulesManager().changePythonPath(path, null, monitor);
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
        return PythonNature.getGrammarVersionFromStr(version);
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

    
    /**
     * Sets the environment variables to be kept in the interpreter info.
     * 
     * Some notes:
     * - Will remove (and warn) about any PYTHONPATH env. var.
     * - Will keep the env. variables sorted internally.
     */
    public void setEnvVariables(String[] env) {

        if(env != null){
            ArrayList<String> lst = new ArrayList<String>();
            //We must make sure that the PYTHONPATH is not in the env. variables.
            for(String s: env){
                Tuple<String, String> sp = StringUtils.splitOnFirst(s, '=');
                if(sp.o1.length() != 0 && sp.o2.length() != 0){
                    if(!checkIfPythonPathEnvVarAndWarnIfIs(sp.o1)){
                        lst.add(s);
                    }
                }
            }
            Collections.sort(lst);
            env = lst.toArray(new String[lst.size()]);
        }

        if(env != null && env.length == 0){
            env = null;
        }

        this.envVariables = env;
    }
    
    public String[] getEnvVariables() {
        return this.envVariables;
    }

    public String[] updateEnv(String[] env) {
        return updateEnv(env, null);
    }
    
    public String[] updateEnv(String[] env, Set<String> keysThatShouldNotBeUpdated) {
        if(this.envVariables == null || this.envVariables.length == 0){
            return env; //nothing to change
        }
        //Ok, it's not null... 
        
        if(env == null || env.length == 0){
            //if the passed was null, just repass the ones contained here
            return this.envVariables;
        }
        
        //both not null, let's merge them
        HashMap<String, String> hashMap = new HashMap<String, String>();
        
        fillMapWithEnv(env, hashMap);
        fillMapWithEnv(envVariables, hashMap, keysThatShouldNotBeUpdated); //will override the keys already there unless they're in keysThatShouldNotBeUpdated
        String[] ret = createEnvWithMap(hashMap);
        
        return ret;
    }

    public static String[] createEnvWithMap(Map<String, String> hashMap) {
        Set<Entry<String, String>> entrySet = hashMap.entrySet();
        String[] ret = new String[entrySet.size()];
        int i=0;
        for (Entry<String, String> entry : entrySet) {
            ret[i] = entry.getKey()+"="+entry.getValue();
            i++;
        }
        return ret;
    }

    public static void fillMapWithEnv(String[] env, HashMap<String, String> hashMap) {
        fillMapWithEnv(env, hashMap, null);
    }
    
    public static void fillMapWithEnv(String[] env, HashMap<String, String> hashMap, Set<String> keysThatShouldNotBeUpdated) {
        if(keysThatShouldNotBeUpdated == null){
            keysThatShouldNotBeUpdated = new HashSet<String>();
        }

        for(String s: env){
            Tuple<String, String> sp = StringUtils.splitOnFirst(s, '=');
            if(sp.o1.length() != 0 && sp.o2.length() != 0 && !keysThatShouldNotBeUpdated.contains(sp.o1)){
                hashMap.put(sp.o1, sp.o2);
            }
        }
    }
    
    
    /**
     * This function will remove any PYTHONPATH entry from the given map (considering the case based on the system)
     * and will give a warning to the user if that's actually done.
     */
    public static void removePythonPathFromEnvMapWithWarning(HashMap<String, String> map) {
        if(map == null){
            return;
        }
        
        for(Iterator<Map.Entry<String, String>> it=map.entrySet().iterator();it.hasNext();){
            Map.Entry<String, String> next = it.next();
            
            String key = next.getKey();
            
            if(checkIfPythonPathEnvVarAndWarnIfIs(key)){
                it.remove();
            }
        }
    }

    /**
     * Warns if the passed key is the PYTHONPATH env. var.
     * 
     * @param key the key to check.
     * @return true if the passed key is a PYTHONPATH env. var. (considers platform)
     */
    public static boolean checkIfPythonPathEnvVarAndWarnIfIs(String key) {
        boolean isPythonPath = false;
        boolean win32 = REF.isWindowsPlatform();
        if(win32){
            key = key.toUpperCase();
        }
        final String keyPlatformDependent = key;
        if(keyPlatformDependent.equals("PYTHONPATH") || keyPlatformDependent.equals("CLASSPATH")){
            final String msg = "Ignoring "+keyPlatformDependent+" specified in the interpreter info.\n" +
            "It's managed depending on the project and other configurations and cannot be directly specified in the interpreter.";
            try {
                RunInUiThread.async(new Runnable(){
                    public void run() {
                        MessageBox message = new MessageBox(PyAction.getShell(), SWT.OK | SWT.ICON_INFORMATION);
                        message.setText("Ignoring "+keyPlatformDependent);
                        message.setMessage(msg);
                        message.open();
                    }
                });
            } catch (Throwable e) {
                // ignore error communication error
            }
            
            Log.log(IStatus.WARNING, msg, null);
            isPythonPath = true;
        }
        return isPythonPath;
    }

    
    /**
     * @return a new interpreter info that's a copy of the current interpreter info.
     */
    public InterpreterInfo makeCopy() {
        return fromString(toString());
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        if(this.name != null){
            return this.name;
        }
        return this.executableOrJar;
    }
 
    public String getNameForUI() {
        if(this.name != null){
            return this.name+"  ("+this.executableOrJar+")";
        }else{
            return this.executableOrJar;
        }
    }
    
    public boolean matchNameBackwardCompatible(String interpreter) {
        if(this.name != null){
            if(interpreter.equals(this.name)){
                return true;
            }
        }
        if(REF.isWindowsPlatform()){
            return interpreter.equalsIgnoreCase(executableOrJar);
        }
        return interpreter.equals(executableOrJar);
    }
}