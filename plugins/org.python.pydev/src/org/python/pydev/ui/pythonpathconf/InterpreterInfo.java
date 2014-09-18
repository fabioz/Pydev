/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterEditor.CancelException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class InterpreterInfo implements IInterpreterInfo {

    //We want to force some libraries to be analyzed as source (e.g.: django)
    private static String[] LIBRARIES_TO_IGNORE_AS_FORCED_BUILTINS = new String[] { "django" };

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
     * Folders or zip files: they should be passed to the pythonpath
     */
    public final java.util.List<String> libs = new ArrayList<String>();

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
    private Map<String, File> predefinedBuiltinsCache;

    /**
     * module management for the system is always binded to an interpreter (binded in this class)
     *
     * The modules manager is no longer persisted. It is restored from a separate file, because we do
     * not want to keep it in the 'configuration', as a giant Base64 string.
     */
    private final ISystemModulesManager modulesManager;

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

    private Properties stringSubstitutionVariables;

    private final Set<String> predefinedCompletionsPath = new TreeSet<String>();

    /**
     * This is the way that the interpreter should be referred. Can be null (in which case the executable is
     * used as the name)
     */
    private String name;

    public ISystemModulesManager getModulesManager() {
        return modulesManager;
    }

    /**
     * Variables manager to resolve variables in the interpreters environment.
     * initStringVariableManager() creates an appropriate version when running
     * within Eclipse, for test the stringVariableManagerForTests can be set to
     * an appropriate mock object
     */
    /*default*/IStringVariableManager stringVariableManagerForTests;

    private IStringVariableManager getStringVariableManager() {
        if (SharedCorePlugin.inTestMode()) {
            return stringVariableManagerForTests;
        }
        VariablesPlugin variablesPlugin = VariablesPlugin.getDefault();
        return variablesPlugin.getStringVariableManager();
    }

    /**
     * @return the pythonpath to be used (only the folders)
     */
    public List<String> getPythonPath() {
        return new ArrayList<String>(libs);
    }

    public InterpreterInfo(String version, String exe, Collection<String> libs0) {
        this.executableOrJar = exe;
        this.version = version;
        ISystemModulesManager modulesManager = new SystemModulesManager(this);

        this.modulesManager = modulesManager;
        libs.addAll(libs0);
    }

    /*default*/InterpreterInfo(String version, String exe, Collection<String> libs0, Collection<String> dlls) {
        this(version, exe, libs0);
    }

    /*default*/InterpreterInfo(String version, String exe, List<String> libs0, List<String> dlls, List<String> forced) {
        this(version, exe, libs0, dlls, forced, null, null);
    }

    /**
     * Note: dlls is no longer used!
     */
    /*default*/InterpreterInfo(String version, String exe, List<String> libs0, List<String> dlls, List<String> forced,
            List<String> envVars, Properties stringSubstitution) {
        this(version, exe, libs0, dlls);
        for (String s : forced) {
            if (!isForcedLibToIgnore(s)) {
                forcedLibs.add(s);
            }
        }

        if (envVars == null) {
            this.setEnvVariables(null);
        } else {
            this.setEnvVariables(envVars.toArray(new String[envVars.size()]));
        }

        this.setStringSubstitutionVariables(stringSubstitution);

        this.clearBuiltinsCache(); //force cache recreation
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof InterpreterInfo)) {
            return false;
        }

        InterpreterInfo info = (InterpreterInfo) o;
        if (info.executableOrJar.equals(this.executableOrJar) == false) {
            return false;
        }

        if (info.libs.equals(this.libs) == false) {
            return false;
        }

        if (info.forcedLibs.equals(this.forcedLibs) == false) {
            return false;
        }

        if (info.predefinedCompletionsPath.equals(this.predefinedCompletionsPath) == false) {
            return false;
        }

        if (this.envVariables != null) {
            if (info.envVariables == null) {
                return false;
            }
            //both not null
            if (!Arrays.equals(this.envVariables, info.envVariables)) {
                return false;
            }
        } else {
            //env is null -- the other must be too
            if (info.envVariables != null) {
                return false;
            }
        }

        //Consider null stringSubstitutionVariables equal to empty stringSubstitutionVariables.
        if (this.stringSubstitutionVariables != null) {
            if (info.stringSubstitutionVariables == null) {
                if (this.stringSubstitutionVariables.size() != 0) {
                    return false;
                }
            } else {
                //both not null
                if (!this.stringSubstitutionVariables.equals(info.stringSubstitutionVariables)) {
                    return false;
                }
            }
        } else {
            //ours is null -- the other must be too
            if (info.stringSubstitutionVariables != null && info.stringSubstitutionVariables.size() > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return this.executableOrJar.hashCode();
    }

    /**
     *
     * @param received
     *            String to parse
     * @param askUserInOutPath
     *            true to prompt user about which paths to include. 
     * @param userSpecifiedExecutable the path the the executable as specified by the user, or null to use that in received
     * @return new interpreter info
     */
    public static InterpreterInfo fromString(String received, boolean askUserInOutPath, String userSpecifiedExecutable) {
        if (received.toLowerCase().indexOf("executable") == -1) {
            throw new RuntimeException(
                    "Unable to recreate the Interpreter info (Its format changed. Please, re-create your Interpreter information).Contents found:"
                            + received);
        }
        received = received.trim();
        int startXml = received.indexOf("<xml>");
        int endXML = received.indexOf("</xml>");

        if (startXml == -1 || endXML == -1) {
            return fromStringOld(received, askUserInOutPath);
        } else {
            received = received.substring(startXml, endXML + "</xml>".length());

            DocumentBuilder parser;
            try {
                parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = parser.parse(new InputSource(new StringReader(received)));
                NodeList childNodes = document.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (!("xml".equals(nodeName))) {
                        continue;
                    }
                    NodeList xmlNodes = item.getChildNodes();

                    boolean fromPythonBackend = false;
                    String infoExecutable = null;
                    String infoName = null;
                    String infoVersion = null;
                    List<String> selection = new ArrayList<String>();
                    List<String> toAsk = new ArrayList<String>();
                    List<String> forcedLibs = new ArrayList<String>();
                    List<String> envVars = new ArrayList<String>();
                    List<String> predefinedPaths = new ArrayList<String>();
                    Properties stringSubstitutionVars = new Properties();

                    DefaultPathsForInterpreterInfo defaultPaths = new DefaultPathsForInterpreterInfo();

                    for (int j = 0; j < xmlNodes.getLength(); j++) {
                        Node xmlChild = xmlNodes.item(j);
                        String name = xmlChild.getNodeName();
                        String data = xmlChild.getTextContent().trim();
                        if ("version".equals(name)) {
                            infoVersion = data;

                        } else if ("name".equals(name)) {
                            infoName = data;

                        } else if ("executable".equals(name)) {
                            infoExecutable = data;

                        } else if ("lib".equals(name)) {
                            NamedNodeMap attributes = xmlChild.getAttributes();
                            Node pathIncludeItem = attributes.getNamedItem("path");

                            if (pathIncludeItem != null) {
                                if (defaultPaths.exists(data)) {
                                    //The python backend is expected to put path='ins' or path='out'
                                    //While our own toString() is not expected to do that.
                                    //This is probably not a very good heuristic, but it maps the current state of affairs.
                                    fromPythonBackend = true;
                                    if (askUserInOutPath) {
                                        toAsk.add(data);
                                    }
                                    //Select only if path is not child of a root path
                                    if (defaultPaths.selectByDefault(data)) {
                                        selection.add(data);
                                    }
                                }

                            } else {
                                //If not specified, included by default (i.e.: if the path="ins" or path="out" is not
                                //given, this string was generated internally and not from the python backend, meaning
                                //that we want to keep it exactly as the user selected).
                                selection.add(data);
                            }

                        } else if ("forced_lib".equals(name)) {
                            forcedLibs.add(data);

                        } else if ("env_var".equals(name)) {
                            envVars.add(data);

                        } else if ("string_substitution_var".equals(name)) {
                            NodeList stringSubstitutionVarNode = xmlChild.getChildNodes();
                            Node keyNode = getNode(stringSubstitutionVarNode, "key");
                            Node valueNode = getNode(stringSubstitutionVarNode, "value");
                            stringSubstitutionVars.put(keyNode.getTextContent().trim(), valueNode.getTextContent()
                                    .trim());

                        } else if ("predefined_completion_path".equals(name)) {
                            predefinedPaths.add(data);

                        } else if ("#text".equals(name)) {
                            if (data.length() > 0) {
                                throw new RuntimeException("Unexpected text content: " + xmlChild.getTextContent());
                            }

                        } else {
                            throw new RuntimeException("Unexpected node: " + name + " Text content: "
                                    + xmlChild.getTextContent());
                        }
                    }

                    if (fromPythonBackend) {
                        //Ok, when the python backend generated the interpreter information, go on and fill it with 
                        //additional entries (i.e.: not only when we need to ask the user), as this information may
                        //be later used to check if the interpreter information is valid or missing paths.
                        AdditionalEntries additionalEntries = new AdditionalEntries();
                        Collection<String> additionalLibraries = additionalEntries.getAdditionalLibraries();
                        if (askUserInOutPath) {
                            addUnique(toAsk, additionalLibraries);
                        }
                        addUnique(selection, additionalLibraries);
                        addUnique(forcedLibs, additionalEntries.getAdditionalBuiltins());

                        //Load environment variables
                        Map<String, String> existingEnv = new HashMap<String, String>();
                        Collection<String> additionalEnvVariables = additionalEntries.getAdditionalEnvVariables();
                        for (String var : additionalEnvVariables) {
                            Tuple<String, String> sp = StringUtils.splitOnFirst(var, '=');
                            existingEnv.put(sp.o1, sp.o2);
                        }
                        for (String var : envVars) {
                            Tuple<String, String> sp = StringUtils.splitOnFirst(var, '=');
                            existingEnv.put(sp.o1, sp.o2);
                        }
                        envVars.clear();
                        Set<Entry<String, String>> set = existingEnv.entrySet();
                        for (Entry<String, String> entry : set) {
                            envVars.add(entry.getKey() + "=" + entry.getValue());
                        }

                        //Additional string substitution variables
                        Map<String, String> additionalStringSubstitutionVariables = additionalEntries
                                .getAdditionalStringSubstitutionVariables();
                        Set<Entry<String, String>> entrySet = additionalStringSubstitutionVariables.entrySet();
                        for (Entry<String, String> entry : entrySet) {
                            if (!stringSubstitutionVars.containsKey(entry.getKey())) {
                                stringSubstitutionVars.setProperty(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                    try {
                        selection = filterUserSelection(selection, toAsk);
                    } catch (CancelException e) {
                        return null;
                    }

                    if (userSpecifiedExecutable != null) {
                        infoExecutable = userSpecifiedExecutable;
                    }
                    InterpreterInfo info = new InterpreterInfo(infoVersion, infoExecutable, selection,
                            new ArrayList<String>(), forcedLibs, envVars, stringSubstitutionVars);
                    info.setName(infoName);
                    for (String s : predefinedPaths) {
                        info.addPredefinedCompletionsPath(s);
                    }
                    return info;
                }
                throw new RuntimeException("Could not find 'xml' node as root of the document.");

            } catch (Exception e) {
                Log.log("Error loading: " + received, e);
                throw new RuntimeException(e); //What can we do about that?
            }

        }
    }

    /**
     *
     * @param received
     *            String to parse
     * @param askUserInOutPath
     *            true to prompt user about which paths to include. When the
     *            user is prompted, IInterpreterNewCustomEntries extension will
     *            be run to contribute additional entries
     * @return new interpreter info
     */
    public static InterpreterInfo fromString(String received, boolean askUserInOutPath) {
        return fromString(received, askUserInOutPath, null);
    }

    /**
     * Add additions that are not already in col
     */
    private static void addUnique(Collection<String> col, Collection<String> additions) {
        for (String string : additions) {
            if (!col.contains(string)) {
                col.add(string);
            }
        }
    }

    /**
     *  Implementation of extension point to get all additions.
     */
    private static class AdditionalEntries implements IInterpreterNewCustomEntries {
        private final List<IInterpreterNewCustomEntries> fParticipants;

        @SuppressWarnings("unchecked")
        AdditionalEntries() {
            fParticipants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_NEW_CUSTOM_ENTRIES);
        }

        public Collection<String> getAdditionalLibraries() {
            final Collection<String> additions = new ArrayList<String>();
            for (final IInterpreterNewCustomEntries newEntriesProvider : fParticipants) {
                SafeRunner.run(new SafeRunnable() {
                    public void run() {
                        additions.addAll(newEntriesProvider.getAdditionalLibraries());
                    }
                });
            }
            return additions;
        }

        public Collection<String> getAdditionalEnvVariables() {
            final Collection<String> additions = new ArrayList<String>();
            for (final IInterpreterNewCustomEntries newEntriesProvider : fParticipants) {
                SafeRunner.run(new SafeRunnable() {
                    public void run() {
                        additions.addAll(newEntriesProvider.getAdditionalEnvVariables());
                    }
                });
            }
            return additions;
        }

        public Collection<String> getAdditionalBuiltins() {
            final Collection<String> additions = new ArrayList<String>();
            for (final IInterpreterNewCustomEntries newEntriesProvider : fParticipants) {
                SafeRunner.run(new SafeRunnable() {
                    public void run() {
                        additions.addAll(newEntriesProvider.getAdditionalBuiltins());
                    }
                });
            }
            return additions;
        }

        public Map<String, String> getAdditionalStringSubstitutionVariables() {
            final Map<String, String> additions = new HashMap<String, String>();
            for (final IInterpreterNewCustomEntries newEntriesProvider : fParticipants) {
                SafeRunner.run(new SafeRunnable() {
                    public void run() {
                        additions.putAll(newEntriesProvider.getAdditionalStringSubstitutionVariables());
                    }
                });
            }
            return additions;
        }

    }

    private static Node getNode(NodeList nodeList, String string) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if (string.equals(item.getNodeName())) {
                return item;
            }
        }
        throw new RuntimeException("Unable to find node: " + string);
    }

    /**
     * Format we receive should be:
     *
     * Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2@PYDEV_STRING_SUBST_VARS@PropertiesObjectAsString
     *
     * or
     *
     * Version2.5Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2@PYDEV_STRING_SUBST_VARS@PropertiesObjectAsString
     * (added only when version 2.5 was added, so, if the string does not have it, it is regarded as 2.4)
     *
     * or
     *
     * Name:MyInterpreter:EndName:Version2.5Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2@PYDEV_STRING_SUBST_VARS@PropertiesObjectAsString
     *
     * Symbols ': @ $'
     */
    private static InterpreterInfo fromStringOld(String received, boolean askUserInOutPath) {

        Tuple<String, String> predefCompsPath = StringUtils.splitOnFirst(received, "@PYDEV_PREDEF_COMPS_PATHS@");
        received = predefCompsPath.o1;

        //Note that the new lines are important for the string substitution, so, we must remove it before removing new lines
        Tuple<String, String> stringSubstitutionVarsSplit = StringUtils.splitOnFirst(received,
                "@PYDEV_STRING_SUBST_VARS@");
        received = stringSubstitutionVarsSplit.o1;

        received = received.replaceAll("\n", "").replaceAll("\r", "");
        String name = null;
        if (received.startsWith("Name:")) {
            int endNameIndex = received.indexOf(":EndName:");
            if (endNameIndex != -1) {
                name = received.substring("Name:".length(), endNameIndex);
                received = received.substring(endNameIndex + ":EndName:".length());
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
        if (lowerExeAndVersion.startsWith("version")) {
            int execut = lowerExeAndVersion.indexOf("executable");
            version = exeAndVersion.substring(0, execut).substring(7);
            exeAndVersion = exeAndVersion.substring(7 + version.length());
        }
        String executable = exeAndVersion.substring(exeAndVersion.indexOf(":") + 1, exeAndVersion.length());

        List<String> selection = new ArrayList<String>();
        List<String> toAsk = new ArrayList<String>();
        for (int i = 1; i < exeAndLibs1.length; i++) { //start at 1 (0 is exe)
            String trimmed = exeAndLibs1[i].trim();
            if (trimmed.length() > 0) {
                if (trimmed.endsWith("OUT_PATH")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 8);
                    if (askUserInOutPath) {
                        toAsk.add(trimmed);
                    } else {
                        //Change 2.0.1: if not asked, it's included by default!
                        selection.add(trimmed);
                    }

                } else if (trimmed.endsWith("INS_PATH")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 8);
                    if (askUserInOutPath) {
                        toAsk.add(trimmed);
                        selection.add(trimmed);
                    } else {
                        selection.add(trimmed);
                    }
                } else {
                    selection.add(trimmed);
                }
            }
        }

        try {
            selection = filterUserSelection(selection, toAsk);
        } catch (CancelException e) {
            return null;
        }

        ArrayList<String> l1 = new ArrayList<String>();
        if (libsSplit.o2.length() > 1) {
            fillList(libsSplit, l1);
        }

        ArrayList<String> l2 = new ArrayList<String>();
        if (forcedSplit.o2.length() > 1) {
            fillList(forcedSplit, l2);
        }

        ArrayList<String> l3 = new ArrayList<String>();
        if (envVarsSplit.o2.length() > 1) {
            fillList(envVarsSplit, l3);
        }
        Properties p4 = null;
        if (stringSubstitutionVarsSplit.o2.length() > 1) {
            p4 = PropertiesHelper.createPropertiesFromString(stringSubstitutionVarsSplit.o2);
        }
        InterpreterInfo info = new InterpreterInfo(version, executable, selection, l1, l2, l3, p4);
        if (predefCompsPath.o2.length() > 1) {
            List<String> split = StringUtils.split(predefCompsPath.o2, '|');
            for (String s : split) {
                s = s.trim();
                if (s.length() > 0) {
                    info.addPredefinedCompletionsPath(s);
                }
            }
        }
        info.setName(name);
        return info;
    }

    public static List<String> filterUserSelection(List<String> selection, List<String> toAsk) throws CancelException {
        boolean result = true;//true == OK, false == CANCELLED
        if (ProjectModulesManager.IN_TESTS) {
            if (InterpreterInfo.configurePathsCallback != null) {
                InterpreterInfo.configurePathsCallback.call(new Tuple<List<String>, List<String>>(toAsk, selection));
            }
        } else {
            if (toAsk.size() > 0) {
                PythonSelectionLibrariesDialog runnable = new PythonSelectionLibrariesDialog(selection, toAsk, true);
                try {
                    RunInUiThread.sync(runnable);
                } catch (NoClassDefFoundError e) {
                } catch (UnsatisfiedLinkError e) {
                    //this means that we're running unit-tests, so, we don't have to do anything about it
                    //as 'l' is already ok.
                }
                result = runnable.getOkResult();
                if (result == false) {
                    //Canceled by the user
                    throw new CancelException();
                }
                selection = runnable.getSelection();
            }
        }
        return selection;
    }

    private static void fillList(Tuple<String, String> forcedSplit, ArrayList<String> l2) {
        String forcedLibs = forcedSplit.o2;
        for (String trimmed : StringUtils.splitAndRemoveEmptyTrimmed(forcedLibs, '|')) {
            trimmed = trimmed.trim();
            if (trimmed.length() > 0) {
                l2.add(trimmed);
            }
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("<xml>\n");
        if (this.name != null) {
            buffer.append("<name>");
            buffer.append(escape(this.name));
            buffer.append("</name>\n");
        }
        buffer.append("<version>");
        buffer.append(escape(version));
        buffer.append("</version>\n");

        buffer.append("<executable>");
        buffer.append(escape(executableOrJar));
        buffer.append("</executable>\n");

        for (Iterator<String> iter = libs.iterator(); iter.hasNext();) {
            buffer.append("<lib>");
            buffer.append(escape(iter.next().toString()));
            buffer.append("</lib>\n");
        }

        if (forcedLibs.size() > 0) {
            for (Iterator<String> iter = forcedLibs.iterator(); iter.hasNext();) {
                buffer.append("<forced_lib>");
                buffer.append(escape(iter.next().toString()));
                buffer.append("</forced_lib>\n");
            }
        }

        if (this.envVariables != null) {
            for (String s : envVariables) {
                buffer.append("<env_var>");
                buffer.append(escape(s));
                buffer.append("</env_var>\n");
            }
        }

        if (this.stringSubstitutionVariables != null && this.stringSubstitutionVariables.size() > 0) {
            Set<Entry<Object, Object>> entrySet = this.stringSubstitutionVariables.entrySet();
            for (Entry<Object, Object> entry : entrySet) {
                buffer.append("<string_substitution_var>");
                buffer.append("<key>");
                buffer.appendObject(escape(entry.getKey()));
                buffer.append("</key>");
                buffer.append("<value>");
                buffer.appendObject(escape(entry.getValue()));
                buffer.append("</value>");
                buffer.append("</string_substitution_var>\n");
            }
        }

        if (this.predefinedCompletionsPath.size() > 0) {
            for (String s : this.predefinedCompletionsPath) {
                buffer.append("<predefined_completion_path>");
                buffer.append(escape(s));
                buffer.append("</predefined_completion_path>");
            }
        }
        buffer.append("</xml>");

        return buffer.toString();
    }

    private static String escape(Object str) {
        if (str == null) {
            return null;
        }
        return new FastStringBuffer(str.toString(), 10).replaceAll("&", "&amp;").replaceAll(">", "&gt;")
                .replaceAll("<", "&lt;")
                .toString();
    }

    /**
     * Old implementation. Kept only for testing backward compatibility!
     */
    public String toStringOld() {
        FastStringBuffer buffer = new FastStringBuffer();
        if (this.name != null) {
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
        if (forcedLibs.size() > 0) {
            for (Iterator<String> iter = forcedLibs.iterator(); iter.hasNext();) {
                buffer.append("|");
                buffer.append(iter.next().toString());
            }
        }

        if (this.envVariables != null) {
            buffer.append("^");
            for (String s : envVariables) {
                buffer.append(s);
                buffer.append("|");
            }
        }

        if (this.stringSubstitutionVariables != null && this.stringSubstitutionVariables.size() > 0) {
            buffer.append("@PYDEV_STRING_SUBST_VARS@");
            buffer.append(PropertiesHelper.createStringFromProperties(this.stringSubstitutionVariables));
        }

        if (this.predefinedCompletionsPath.size() > 0) {
            buffer.append("@PYDEV_PREDEF_COMPS_PATHS@");
            for (String s : this.predefinedCompletionsPath) {
                buffer.append("|");
                buffer.append(s);
            }
        }

        return buffer.toString();
    }

    /**
     * Adds the compiled libs (dlls)
     */
    public void restoreCompiledLibs(IProgressMonitor monitor) {
        //the compiled with the interpreter should be already gotten.

        for (String lib : this.libs) {
            addForcedLibsFor(lib);
        }

        //we have it in source, but want to interpret it, source info (ast) does not give us much
        forcedLibs.add("os");

        //we also need to add this submodule (because even though it's documented as such, it's not really
        //implemented that way with a separate file -- there's black magic to put it there)
        forcedLibs.add("os.path");

        //as it is a set, there is no problem to add it twice
        if (this.version.startsWith("2") || this.version.startsWith("1")) {
            //don't add it for 3.0 onwards.
            forcedLibs.add("__builtin__"); //jython bug: __builtin__ is not added
        }
        forcedLibs.add("sys"); //jython bug: sys is not added
        forcedLibs.add("email"); //email has some lazy imports that pydev cannot handle through the source
        forcedLibs.add("hashlib"); //depending on the Python version, hashlib cannot find md5, so, let's always leave it there.
        forcedLibs.add("pytest"); //yeap, pytest does have a structure that's pretty hard to analyze.

        int interpreterType = getInterpreterType();
        switch (interpreterType) {
            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                //by default, we don't want to force anything to python.
                forcedLibs.add("StringIO"); //jython bug: StringIO is not added
                forcedLibs.add("re"); //re is very strange in Jython (while it's OK in Python)
                forcedLibs.add("com.ziclix.python.sql"); //bultin to jython but not reported.
                break;

            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                //those are sources, but we want to get runtime info on them.
                forcedLibs.add("OpenGL");
                forcedLibs.add("wxPython");
                forcedLibs.add("wx");
                forcedLibs.add("numpy");
                forcedLibs.add("scipy");
                forcedLibs.add("Image"); //for PIL

                //these are the builtins -- apparently sys.builtin_module_names is not ok in linux.
                forcedLibs.add("_ast");
                forcedLibs.add("_bisect");
                forcedLibs.add("_bytesio");
                forcedLibs.add("_codecs");
                forcedLibs.add("_codecs_cn");
                forcedLibs.add("_codecs_hk");
                forcedLibs.add("_codecs_iso2022");
                forcedLibs.add("_codecs_jp");
                forcedLibs.add("_codecs_kr");
                forcedLibs.add("_codecs_tw");
                forcedLibs.add("_collections");
                forcedLibs.add("_csv");
                forcedLibs.add("_fileio");
                forcedLibs.add("_functools");
                forcedLibs.add("_heapq");
                forcedLibs.add("_hotshot");
                forcedLibs.add("_json");
                forcedLibs.add("_locale");
                forcedLibs.add("_lsprof");
                forcedLibs.add("_md5");
                forcedLibs.add("_multibytecodec");
                forcedLibs.add("_random");
                forcedLibs.add("_sha");
                forcedLibs.add("_sha256");
                forcedLibs.add("_sha512");
                forcedLibs.add("_sre");
                forcedLibs.add("_struct");
                forcedLibs.add("_subprocess");
                forcedLibs.add("_symtable");
                forcedLibs.add("_warnings");
                forcedLibs.add("_weakref");
                forcedLibs.add("_winreg");
                forcedLibs.add("array");
                forcedLibs.add("audioop");
                forcedLibs.add("binascii");
                forcedLibs.add("cPickle");
                forcedLibs.add("cStringIO");
                forcedLibs.add("cmath");
                forcedLibs.add("datetime");
                forcedLibs.add("errno");
                forcedLibs.add("exceptions");
                forcedLibs.add("future_builtins");
                forcedLibs.add("gc");
                forcedLibs.add("imageop");
                forcedLibs.add("imp");
                forcedLibs.add("itertools");
                forcedLibs.add("marshal");
                forcedLibs.add("math");
                forcedLibs.add("mmap");
                forcedLibs.add("msvcrt");
                forcedLibs.add("nt");
                forcedLibs.add("operator");
                forcedLibs.add("parser");
                forcedLibs.add("signal");
                forcedLibs.add("socket"); //socket seems to have issues on linux
                forcedLibs.add("strop");
                forcedLibs.add("sys");
                forcedLibs.add("thread");
                forcedLibs.add("time");
                forcedLibs.add("xxsubtype");
                forcedLibs.add("zipimport");
                forcedLibs.add("zlib");

                break;

            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                //base namespaces
                forcedLibs.add("System");
                forcedLibs.add("Microsoft");
                forcedLibs.add("clr");

                //other namespaces (from http://msdn.microsoft.com/en-us/library/ms229335.aspx)
                forcedLibs.add("IEHost.Execute");
                forcedLibs.add("Microsoft.Aspnet.Snapin");
                forcedLibs.add("Microsoft.Build.BuildEngine");
                forcedLibs.add("Microsoft.Build.Conversion");
                forcedLibs.add("Microsoft.Build.Framework");
                forcedLibs.add("Microsoft.Build.Tasks");
                forcedLibs.add("Microsoft.Build.Tasks.Deployment.Bootstrapper");
                forcedLibs.add("Microsoft.Build.Tasks.Deployment.ManifestUtilities");
                forcedLibs.add("Microsoft.Build.Tasks.Hosting");
                forcedLibs.add("Microsoft.Build.Tasks.Windows");
                forcedLibs.add("Microsoft.Build.Utilities");
                forcedLibs.add("Microsoft.CLRAdmin");
                forcedLibs.add("Microsoft.CSharp");
                forcedLibs.add("Microsoft.Data.Entity.Build.Tasks");
                forcedLibs.add("Microsoft.IE");
                forcedLibs.add("Microsoft.Ink");
                forcedLibs.add("Microsoft.Ink.TextInput");
                forcedLibs.add("Microsoft.JScript");
                forcedLibs.add("Microsoft.JScript.Vsa");
                forcedLibs.add("Microsoft.ManagementConsole");
                forcedLibs.add("Microsoft.ManagementConsole.Advanced");
                forcedLibs.add("Microsoft.ManagementConsole.Internal");
                forcedLibs.add("Microsoft.ServiceModel.Channels.Mail");
                forcedLibs.add("Microsoft.ServiceModel.Channels.Mail.ExchangeWebService");
                forcedLibs.add("Microsoft.ServiceModel.Channels.Mail.ExchangeWebService.Exchange2007");
                forcedLibs.add("Microsoft.ServiceModel.Channels.Mail.WindowsMobile");
                forcedLibs.add("Microsoft.SqlServer.Server");
                forcedLibs.add("Microsoft.StylusInput");
                forcedLibs.add("Microsoft.StylusInput.PluginData");
                forcedLibs.add("Microsoft.VisualBasic");
                forcedLibs.add("Microsoft.VisualBasic.ApplicationServices");
                forcedLibs.add("Microsoft.VisualBasic.Compatibility.VB6");
                forcedLibs.add("Microsoft.VisualBasic.CompilerServices");
                forcedLibs.add("Microsoft.VisualBasic.Devices");
                forcedLibs.add("Microsoft.VisualBasic.FileIO");
                forcedLibs.add("Microsoft.VisualBasic.Logging");
                forcedLibs.add("Microsoft.VisualBasic.MyServices");
                forcedLibs.add("Microsoft.VisualBasic.MyServices.Internal");
                forcedLibs.add("Microsoft.VisualBasic.Vsa");
                forcedLibs.add("Microsoft.VisualC");
                forcedLibs.add("Microsoft.VisualC.StlClr");
                forcedLibs.add("Microsoft.VisualC.StlClr.Generic");
                forcedLibs.add("Microsoft.Vsa");
                forcedLibs.add("Microsoft.Vsa.Vb.CodeDOM");
                forcedLibs.add("Microsoft.Win32");
                forcedLibs.add("Microsoft.Win32.SafeHandles");
                forcedLibs.add("Microsoft.Windows.Themes");
                forcedLibs.add("Microsoft.WindowsCE.Forms");
                forcedLibs.add("Microsoft.WindowsMobile.DirectX");
                forcedLibs.add("Microsoft.WindowsMobile.DirectX.Direct3D");
                forcedLibs.add("Microsoft_VsaVb");
                forcedLibs.add("RegCode");
                forcedLibs.add("System");
                forcedLibs.add("System.AddIn");
                forcedLibs.add("System.AddIn.Contract");
                forcedLibs.add("System.AddIn.Contract.Automation");
                forcedLibs.add("System.AddIn.Contract.Collections");
                forcedLibs.add("System.AddIn.Hosting");
                forcedLibs.add("System.AddIn.Pipeline");
                forcedLibs.add("System.CodeDom");
                forcedLibs.add("System.CodeDom.Compiler");
                forcedLibs.add("System.Collections");
                forcedLibs.add("System.Collections.Generic");
                forcedLibs.add("System.Collections.ObjectModel");
                forcedLibs.add("System.Collections.Specialized");
                forcedLibs.add("System.ComponentModel");
                forcedLibs.add("System.ComponentModel.DataAnnotations");
                forcedLibs.add("System.ComponentModel.Design");
                forcedLibs.add("System.ComponentModel.Design.Data");
                forcedLibs.add("System.ComponentModel.Design.Serialization");
                forcedLibs.add("System.Configuration");
                forcedLibs.add("System.Configuration.Assemblies");
                forcedLibs.add("System.Configuration.Install");
                forcedLibs.add("System.Configuration.Internal");
                forcedLibs.add("System.Configuration.Provider");
                forcedLibs.add("System.Data");
                forcedLibs.add("System.Data.Common");
                forcedLibs.add("System.Data.Common.CommandTrees");
                forcedLibs.add("System.Data.Design");
                forcedLibs.add("System.Data.Entity.Design");
                forcedLibs.add("System.Data.Entity.Design.AspNet");
                forcedLibs.add("System.Data.EntityClient");
                forcedLibs.add("System.Data.Linq");
                forcedLibs.add("System.Data.Linq.Mapping");
                forcedLibs.add("System.Data.Linq.SqlClient");
                forcedLibs.add("System.Data.Linq.SqlClient.Implementation");
                forcedLibs.add("System.Data.Mapping");
                forcedLibs.add("System.Data.Metadata.Edm");
                forcedLibs.add("System.Data.Objects");
                forcedLibs.add("System.Data.Objects.DataClasses");
                forcedLibs.add("System.Data.Odbc");
                forcedLibs.add("System.Data.OleDb");
                forcedLibs.add("System.Data.OracleClient");
                forcedLibs.add("System.Data.Services");
                forcedLibs.add("System.Data.Services.Client");
                forcedLibs.add("System.Data.Services.Common");
                forcedLibs.add("System.Data.Services.Design");
                forcedLibs.add("System.Data.Services.Internal");
                forcedLibs.add("System.Data.Sql");
                forcedLibs.add("System.Data.SqlClient");
                forcedLibs.add("System.Data.SqlTypes");
                forcedLibs.add("System.Deployment.Application");
                forcedLibs.add("System.Deployment.Internal");
                forcedLibs.add("System.Diagnostics");
                forcedLibs.add("System.Diagnostics.CodeAnalysis");
                forcedLibs.add("System.Diagnostics.Design");
                forcedLibs.add("System.Diagnostics.Eventing");
                forcedLibs.add("System.Diagnostics.Eventing.Reader");
                forcedLibs.add("System.Diagnostics.PerformanceData");
                forcedLibs.add("System.Diagnostics.SymbolStore");
                forcedLibs.add("System.DirectoryServices");
                forcedLibs.add("System.DirectoryServices.AccountManagement");
                forcedLibs.add("System.DirectoryServices.ActiveDirectory");
                forcedLibs.add("System.DirectoryServices.Protocols");
                forcedLibs.add("System.Drawing");
                forcedLibs.add("System.Drawing.Design");
                forcedLibs.add("System.Drawing.Drawing2D");
                forcedLibs.add("System.Drawing.Imaging");
                forcedLibs.add("System.Drawing.Printing");
                forcedLibs.add("System.Drawing.Text");
                forcedLibs.add("System.EnterpriseServices");
                forcedLibs.add("System.EnterpriseServices.CompensatingResourceManager");
                forcedLibs.add("System.EnterpriseServices.Internal");
                forcedLibs.add("System.Globalization");
                forcedLibs.add("System.IdentityModel.Claims");
                forcedLibs.add("System.IdentityModel.Policy");
                forcedLibs.add("System.IdentityModel.Selectors");
                forcedLibs.add("System.IdentityModel.Tokens");
                forcedLibs.add("System.IO");
                forcedLibs.add("System.IO.Compression");
                forcedLibs.add("System.IO.IsolatedStorage");
                forcedLibs.add("System.IO.Log");
                forcedLibs.add("System.IO.Packaging");
                forcedLibs.add("System.IO.Pipes");
                forcedLibs.add("System.IO.Ports");
                forcedLibs.add("System.Linq");
                forcedLibs.add("System.Linq.Expressions");
                forcedLibs.add("System.Management");
                forcedLibs.add("System.Management.Instrumentation");
                forcedLibs.add("System.Media");
                forcedLibs.add("System.Messaging");
                forcedLibs.add("System.Messaging.Design");
                forcedLibs.add("System.Net");
                forcedLibs.add("System.Net.Cache");
                forcedLibs.add("System.Net.Configuration");
                forcedLibs.add("System.Net.Mail");
                forcedLibs.add("System.Net.Mime");
                forcedLibs.add("System.Net.NetworkInformation");
                forcedLibs.add("System.Net.PeerToPeer");
                forcedLibs.add("System.Net.PeerToPeer.Collaboration");
                forcedLibs.add("System.Net.Security");
                forcedLibs.add("System.Net.Sockets");
                forcedLibs.add("System.Printing");
                forcedLibs.add("System.Printing.IndexedProperties");
                forcedLibs.add("System.Printing.Interop");
                forcedLibs.add("System.Reflection");
                forcedLibs.add("System.Reflection.Emit");
                forcedLibs.add("System.Resources");
                forcedLibs.add("System.Resources.Tools");
                forcedLibs.add("System.Runtime");
                forcedLibs.add("System.Runtime.CompilerServices");
                forcedLibs.add("System.Runtime.ConstrainedExecution");
                forcedLibs.add("System.Runtime.Hosting");
                forcedLibs.add("System.Runtime.InteropServices");
                forcedLibs.add("System.Runtime.InteropServices.ComTypes");
                forcedLibs.add("System.Runtime.InteropServices.CustomMarshalers");
                forcedLibs.add("System.Runtime.InteropServices.Expando");
                forcedLibs.add("System.Runtime.Remoting");
                forcedLibs.add("System.Runtime.Remoting.Activation");
                forcedLibs.add("System.Runtime.Remoting.Channels");
                forcedLibs.add("System.Runtime.Remoting.Channels.Http");
                forcedLibs.add("System.Runtime.Remoting.Channels.Ipc");
                forcedLibs.add("System.Runtime.Remoting.Channels.Tcp");
                forcedLibs.add("System.Runtime.Remoting.Contexts");
                forcedLibs.add("System.Runtime.Remoting.Lifetime");
                forcedLibs.add("System.Runtime.Remoting.Messaging");
                forcedLibs.add("System.Runtime.Remoting.Metadata");
                forcedLibs.add("System.Runtime.Remoting.MetadataServices");
                forcedLibs.add("System.Runtime.Remoting.Proxies");
                forcedLibs.add("System.Runtime.Remoting.Services");
                forcedLibs.add("System.Runtime.Serialization");
                forcedLibs.add("System.Runtime.Serialization.Configuration");
                forcedLibs.add("System.Runtime.Serialization.Formatters");
                forcedLibs.add("System.Runtime.Serialization.Formatters.Binary");
                forcedLibs.add("System.Runtime.Serialization.Formatters.Soap");
                forcedLibs.add("System.Runtime.Serialization.Json");
                forcedLibs.add("System.Runtime.Versioning");
                forcedLibs.add("System.Security");
                forcedLibs.add("System.Security.AccessControl");
                forcedLibs.add("System.Security.Authentication");
                forcedLibs.add("System.Security.Authentication.ExtendedProtection");
                forcedLibs.add("System.Security.Authentication.ExtendedProtection.Configuration");
                forcedLibs.add("System.Security.Cryptography");
                forcedLibs.add("System.Security.Cryptography.Pkcs");
                forcedLibs.add("System.Security.Cryptography.X509Certificates");
                forcedLibs.add("System.Security.Cryptography.Xml");
                forcedLibs.add("System.Security.Permissions");
                forcedLibs.add("System.Security.Policy");
                forcedLibs.add("System.Security.Principal");
                forcedLibs.add("System.Security.RightsManagement");
                forcedLibs.add("System.ServiceModel");
                forcedLibs.add("System.ServiceModel.Activation");
                forcedLibs.add("System.ServiceModel.Activation.Configuration");
                forcedLibs.add("System.ServiceModel.Channels");
                forcedLibs.add("System.ServiceModel.ComIntegration");
                forcedLibs.add("System.ServiceModel.Configuration");
                forcedLibs.add("System.ServiceModel.Description");
                forcedLibs.add("System.ServiceModel.Diagnostics");
                forcedLibs.add("System.ServiceModel.Dispatcher");
                forcedLibs.add("System.ServiceModel.Install.Configuration");
                forcedLibs.add("System.ServiceModel.Internal");
                forcedLibs.add("System.ServiceModel.MsmqIntegration");
                forcedLibs.add("System.ServiceModel.PeerResolvers");
                forcedLibs.add("System.ServiceModel.Persistence");
                forcedLibs.add("System.ServiceModel.Security");
                forcedLibs.add("System.ServiceModel.Security.Tokens");
                forcedLibs.add("System.ServiceModel.Syndication");
                forcedLibs.add("System.ServiceModel.Web");
                forcedLibs.add("System.ServiceProcess");
                forcedLibs.add("System.ServiceProcess.Design");
                forcedLibs.add("System.Speech.AudioFormat");
                forcedLibs.add("System.Speech.Recognition");
                forcedLibs.add("System.Speech.Recognition.SrgsGrammar");
                forcedLibs.add("System.Speech.Synthesis");
                forcedLibs.add("System.Speech.Synthesis.TtsEngine");
                forcedLibs.add("System.Text");
                forcedLibs.add("System.Text.RegularExpressions");
                forcedLibs.add("System.Threading");
                forcedLibs.add("System.Timers");
                forcedLibs.add("System.Transactions");
                forcedLibs.add("System.Transactions.Configuration");
                forcedLibs.add("System.Web");
                forcedLibs.add("System.Web.ApplicationServices");
                forcedLibs.add("System.Web.Caching");
                forcedLibs.add("System.Web.ClientServices");
                forcedLibs.add("System.Web.ClientServices.Providers");
                forcedLibs.add("System.Web.Compilation");
                forcedLibs.add("System.Web.Configuration");
                forcedLibs.add("System.Web.Configuration.Internal");
                forcedLibs.add("System.Web.DynamicData");
                forcedLibs.add("System.Web.DynamicData.Design");
                forcedLibs.add("System.Web.DynamicData.ModelProviders");
                forcedLibs.add("System.Web.Handlers");
                forcedLibs.add("System.Web.Hosting");
                forcedLibs.add("System.Web.Mail");
                forcedLibs.add("System.Web.Management");
                forcedLibs.add("System.Web.Mobile");
                forcedLibs.add("System.Web.Profile");
                forcedLibs.add("System.Web.Query.Dynamic");
                forcedLibs.add("System.Web.RegularExpressions");
                forcedLibs.add("System.Web.Routing");
                forcedLibs.add("System.Web.Script.Serialization");
                forcedLibs.add("System.Web.Script.Services");
                forcedLibs.add("System.Web.Security");
                forcedLibs.add("System.Web.Services");
                forcedLibs.add("System.Web.Services.Configuration");
                forcedLibs.add("System.Web.Services.Description");
                forcedLibs.add("System.Web.Services.Discovery");
                forcedLibs.add("System.Web.Services.Protocols");
                forcedLibs.add("System.Web.SessionState");
                forcedLibs.add("System.Web.UI");
                forcedLibs.add("System.Web.UI.Adapters");
                forcedLibs.add("System.Web.UI.Design");
                forcedLibs.add("System.Web.UI.Design.MobileControls");
                forcedLibs.add("System.Web.UI.Design.MobileControls.Converters");
                forcedLibs.add("System.Web.UI.Design.WebControls");
                forcedLibs.add("System.Web.UI.Design.WebControls.WebParts");
                forcedLibs.add("System.Web.UI.MobileControls");
                forcedLibs.add("System.Web.UI.MobileControls.Adapters");
                forcedLibs.add("System.Web.UI.MobileControls.Adapters.XhtmlAdapters");
                forcedLibs.add("System.Web.UI.WebControls");
                forcedLibs.add("System.Web.UI.WebControls.Adapters");
                forcedLibs.add("System.Web.UI.WebControls.WebParts");
                forcedLibs.add("System.Web.Util");
                forcedLibs.add("System.Windows");
                forcedLibs.add("System.Windows.Annotations");
                forcedLibs.add("System.Windows.Annotations.Storage");
                forcedLibs.add("System.Windows.Automation");
                forcedLibs.add("System.Windows.Automation.Peers");
                forcedLibs.add("System.Windows.Automation.Provider");
                forcedLibs.add("System.Windows.Automation.Text");
                forcedLibs.add("System.Windows.Controls");
                forcedLibs.add("System.Windows.Controls.Primitives");
                forcedLibs.add("System.Windows.Converters");
                forcedLibs.add("System.Windows.Data");
                forcedLibs.add("System.Windows.Documents");
                forcedLibs.add("System.Windows.Documents.Serialization");
                forcedLibs.add("System.Windows.Forms");
                forcedLibs.add("System.Windows.Forms.ComponentModel.Com2Interop");
                forcedLibs.add("System.Windows.Forms.Design");
                forcedLibs.add("System.Windows.Forms.Design.Behavior");
                forcedLibs.add("System.Windows.Forms.Integration");
                forcedLibs.add("System.Windows.Forms.Layout");
                forcedLibs.add("System.Windows.Forms.PropertyGridInternal");
                forcedLibs.add("System.Windows.Forms.VisualStyles");
                forcedLibs.add("System.Windows.Ink");
                forcedLibs.add("System.Windows.Ink.AnalysisCore");
                forcedLibs.add("System.Windows.Input");
                forcedLibs.add("System.Windows.Input.StylusPlugIns");
                forcedLibs.add("System.Windows.Interop");
                forcedLibs.add("System.Windows.Markup");
                forcedLibs.add("System.Windows.Markup.Localizer");
                forcedLibs.add("System.Windows.Markup.Primitives");
                forcedLibs.add("System.Windows.Media");
                forcedLibs.add("System.Windows.Media.Animation");
                forcedLibs.add("System.Windows.Media.Converters");
                forcedLibs.add("System.Windows.Media.Effects");
                forcedLibs.add("System.Windows.Media.Imaging");
                forcedLibs.add("System.Windows.Media.Media3D");
                forcedLibs.add("System.Windows.Media.Media3D.Converters");
                forcedLibs.add("System.Windows.Media.TextFormatting");
                forcedLibs.add("System.Windows.Navigation");
                forcedLibs.add("System.Windows.Resources");
                forcedLibs.add("System.Windows.Shapes");
                forcedLibs.add("System.Windows.Threading");
                forcedLibs.add("System.Windows.Xps");
                forcedLibs.add("System.Windows.Xps.Packaging");
                forcedLibs.add("System.Windows.Xps.Serialization");
                forcedLibs.add("System.Workflow.Activities");
                forcedLibs.add("System.Workflow.Activities.Configuration");
                forcedLibs.add("System.Workflow.Activities.Rules");
                forcedLibs.add("System.Workflow.Activities.Rules.Design");
                forcedLibs.add("System.Workflow.ComponentModel");
                forcedLibs.add("System.Workflow.ComponentModel.Compiler");
                forcedLibs.add("System.Workflow.ComponentModel.Design");
                forcedLibs.add("System.Workflow.ComponentModel.Serialization");
                forcedLibs.add("System.Workflow.Runtime");
                forcedLibs.add("System.Workflow.Runtime.Configuration");
                forcedLibs.add("System.Workflow.Runtime.DebugEngine");
                forcedLibs.add("System.Workflow.Runtime.Hosting");
                forcedLibs.add("System.Workflow.Runtime.Tracking");
                forcedLibs.add("System.Xml");
                forcedLibs.add("System.Xml.Linq");
                forcedLibs.add("System.Xml.Schema");
                forcedLibs.add("System.Xml.Serialization");
                forcedLibs.add("System.Xml.Serialization.Advanced");
                forcedLibs.add("System.Xml.Serialization.Configuration");
                forcedLibs.add("System.Xml.XPath");
                forcedLibs.add("System.Xml.Xsl");
                forcedLibs.add("System.Xml.Xsl.Runtime");
                forcedLibs.add("UIAutomationClientsideProviders");

                break;

            default:
                throw new RuntimeException("Don't know how to treat: " + interpreterType);
        }
        this.clearBuiltinsCache(); //force cache recreation
    }

    private void addForcedLibsFor(String lib) {
        //For now only adds "werkzeug", but this is meant as an extension place.
        File file = new File(lib);
        if (file.exists()) {
            addToForcedBuiltinsIfItExists(file, "werkzeug", "werkzeug");
            addToForcedBuiltinsIfItExists(file, "nose", "nose", "nose.tools");
            addToForcedBuiltinsIfItExists(file, "astropy", "astropy", "astropy.units");
        }
    }

    private void addToForcedBuiltinsIfItExists(File file, String libraryToAdd, String... addToForcedBuiltins) {
        if (file.isDirectory()) {
            //check as dir (if it has a werkzeug folder)
            File werkzeug = new File(file, libraryToAdd);
            if (werkzeug.isDirectory()) {
                for (String s : addToForcedBuiltins) {
                    forcedLibs.add(s);
                }
            }
        } else {
            //check as zip (if it has a werkzeug entry -- note that we have to check the __init__
            //because an entry just with the folder doesn't really exist)
            try {
                try (ZipFile zipFile = new ZipFile(file)) {
                    if (zipFile.getEntry(libraryToAdd + "/__init__.py") != null) {
                        for (String s : addToForcedBuiltins) {
                            forcedLibs.add(s);
                        }
                    }
                }
            } catch (Exception e) {
                //ignore (not zip file)
            }
        }
    }

    //  Initially I thought werkzeug would need to add all the contents, so, this was a prototype to
    //  analyze it and add what's needed (but it turns out that just adding werkzeug is ok.
    //	protected void handleWerkzeug(File initWerkzeug) {
    //		String fileContents = FileUtils.getFileContents(initWerkzeug);
    //		Tuple<SimpleNode, Throwable> parse = PyParser.reparseDocument(
    //				new PyParser.ParserInfo(new Document(fileContents), false, this.getGrammarVersion()));
    //		Module o1 = (Module) parse.o1;
    //		forcedLibs.add("werkzeug");
    //		for(stmtType stmt:o1.body){
    //			if(stmt instanceof Assign){
    //				Assign assign = (Assign) stmt;
    //				if(assign.targets.length == 1){
    //					if(assign.value instanceof Dict){
    //						String rep = NodeUtils.getRepresentationString(assign.targets[0]);
    //						if("all_by_module".equals(rep)){
    //							Dict dict = (Dict) assign.value;
    //							for(exprType key:dict.keys){
    //								if(key instanceof Str){
    //									Str str = (Str) key;
    //									forcedLibs.add(str.s);
    //								}
    //							}
    //						}
    //					}else if(assign.value instanceof Call){
    //						String rep = NodeUtils.getRepresentationString(assign.targets[0]);
    //						if("attribute_modules".equals(rep)){
    //							Call call = (Call) assign.value;
    //							rep = NodeUtils.getRepresentationString(call.func);
    //							if("fromkeys".equals(rep)){
    //								if(call.args.length == 1){
    //									if(call.args[0] instanceof org.python.pydev.parser.jython.ast.List){
    //										org.python.pydev.parser.jython.ast.List list = (org.python.pydev.parser.jython.ast.List) call.args[0];
    //										for(exprType elt:list.elts){
    //											if(elt instanceof Str){
    //												Str str = (Str) elt;
    //												forcedLibs.add("werkzeug."+str.s);
    //											}
    //										}
    //
    //									}
    //								}
    //							}
    //						}
    //					}
    //				}
    //			}
    //		}
    //	}

    private void clearBuiltinsCache() {
        this.builtinsCache = null; //force cache recreation
        this.predefinedBuiltinsCache = null;
    }

    /**
     * Restores the path given non-standard libraries
     * @param path
     */
    private void restorePythonpath(String path, IProgressMonitor monitor) {
        //no managers involved here...
        getModulesManager().changePythonPath(path, null, monitor);
    }

    /**
     * Restores the path with the discovered libs
     * @param path
     */
    public void restorePythonpath(IProgressMonitor monitor) {
        FastStringBuffer buffer = new FastStringBuffer();
        for (Iterator<String> iter = libs.iterator(); iter.hasNext();) {
            String folder = iter.next();
            buffer.append(folder);
            buffer.append("|");
        }
        restorePythonpath(buffer.toString(), monitor);
    }

    public int getInterpreterType() {
        if (isJythonExecutable(executableOrJar)) {
            return IInterpreterManager.INTERPRETER_TYPE_JYTHON;

        } else if (isIronpythonExecutable(executableOrJar)) {
            return IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON;
        }
        //neither one: it's python.
        return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
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

    /**
     * @param executable the executable we want to know about
     * @return if the executable is the ironpython executable.
     */
    public static boolean isIronpythonExecutable(String executable) {
        File file = new File(executable);
        return file.getName().startsWith("ipy");
    }

    public static String getExeAsFileSystemValidPath(String executableOrJar) {
        return PyStringUtils.getExeAsFileSystemValidPath(executableOrJar);
    }

    public String getExeAsFileSystemValidPath() {
        return getExeAsFileSystemValidPath(executableOrJar);
    }

    public String getVersion() {
        return version;
    }

    public int getGrammarVersion() {
        return PythonNature.getGrammarVersionFromStr(version);
    }

    //START: Things related to the builtins (forcedLibs) ---------------------------------------------------------------
    public String[] getBuiltins() {
        if (this.builtinsCache == null) {
            Set<String> set = new HashSet<String>(forcedLibs);
            this.builtinsCache = set.toArray(new String[0]);
        }
        return this.builtinsCache;
    }

    public void addForcedLib(String forcedLib) {
        if (isForcedLibToIgnore(forcedLib)) {
            return;
        }
        this.forcedLibs.add(forcedLib);
        this.clearBuiltinsCache();
    }

    /**
     * @return true if the passed forced lib should not be added to the forced builtins.
     */
    private boolean isForcedLibToIgnore(String forcedLib) {
        if (forcedLib == null) {
            return true;
        }
        //We want django to be always analyzed as source
        for (String s : LIBRARIES_TO_IGNORE_AS_FORCED_BUILTINS) {
            if (forcedLib.equals(s) || forcedLib.startsWith(s + ".")) {
                return true;
            }
        }
        return false;
    }

    public void removeForcedLib(String forcedLib) {
        this.forcedLibs.remove(forcedLib);
        this.clearBuiltinsCache();
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

        if (env != null) {
            ArrayList<String> lst = new ArrayList<String>();
            //We must make sure that the PYTHONPATH is not in the env. variables.
            for (String s : env) {
                Tuple<String, String> sp = StringUtils.splitOnFirst(s, '=');
                if (sp.o1.length() != 0 && sp.o2.length() != 0) {
                    if (!checkIfPythonPathEnvVarAndWarnIfIs(sp.o1)) {
                        lst.add(s);
                    }
                }
            }
            Collections.sort(lst);
            env = lst.toArray(new String[lst.size()]);
        }

        if (env != null && env.length == 0) {
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
        if (this.envVariables == null || this.envVariables.length == 0) {
            return env; //nothing to change
        }
        //Ok, it's not null...
        //let's merge them (env may be null/zero-length but we need to apply variable resolver to envVariables anyway)
        HashMap<String, String> hashMap = new HashMap<String, String>();

        fillMapWithEnv(env, hashMap, null, null);
        fillMapWithEnv(envVariables, hashMap, keysThatShouldNotBeUpdated, getStringVariableManager()); //will override the keys already there unless they're in keysThatShouldNotBeUpdated

        String[] ret = createEnvWithMap(hashMap);

        return ret;
    }

    public static String[] createEnvWithMap(Map<String, String> hashMap) {
        Set<Entry<String, String>> entrySet = hashMap.entrySet();
        String[] ret = new String[entrySet.size()];
        int i = 0;
        for (Entry<String, String> entry : entrySet) {
            ret[i] = entry.getKey() + "=" + entry.getValue();
            i++;
        }
        return ret;
    }

    public static void fillMapWithEnv(String[] env, HashMap<String, String> hashMap,
            Set<String> keysThatShouldNotBeUpdated, IStringVariableManager manager) {
        if (env == null || env.length == 0) {
            // nothing to do
            return;
        }

        if (keysThatShouldNotBeUpdated == null) {
            keysThatShouldNotBeUpdated = Collections.emptySet();
        }

        for (String s : env) {
            Tuple<String, String> sp = StringUtils.splitOnFirst(s, '=');
            if (sp.o1.length() != 0 && sp.o2.length() != 0 && !keysThatShouldNotBeUpdated.contains(sp.o1)) {
                String value = sp.o2;
                if (manager != null) {
                    try {
                        value = manager.performStringSubstitution(value, false);
                    } catch (CoreException e) {
                        // Unreachable as false passed to reportUndefinedVariables above
                    }
                }
                hashMap.put(sp.o1, value);
            }
        }
    }

    /**
     * This function will remove any PYTHONPATH entry from the given map (considering the case based on the system)
     * and will give a warning to the user if that's actually done.
     */
    public static void removePythonPathFromEnvMapWithWarning(HashMap<String, String> map) {
        if (map == null) {
            return;
        }

        for (Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> next = it.next();

            String key = next.getKey();

            if (checkIfPythonPathEnvVarAndWarnIfIs(key)) {
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
        boolean win32 = PlatformUtils.isWindowsPlatform();
        if (win32) {
            key = key.toUpperCase();
        }
        final String keyPlatformDependent = key;
        if (keyPlatformDependent.equals("PYTHONPATH") || keyPlatformDependent.equals("CLASSPATH")
                || keyPlatformDependent.equals("JYTHONPATH") || keyPlatformDependent.equals("IRONPYTHONPATH")) {
            final String msg = "Ignoring "
                    + keyPlatformDependent
                    + " specified in the interpreter info.\n"
                    + "It's managed depending on the project and other configurations and cannot be directly specified in the interpreter.";
            try {
                RunInUiThread.async(new Runnable() {
                    public void run() {
                        MessageBox message = new MessageBox(EditorUtils.getShell(), SWT.OK | SWT.ICON_INFORMATION);
                        message.setText("Ignoring " + keyPlatformDependent);
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
        InterpreterInfo ret = fromString(toString(), false);
        ret.setModificationStamp(modificationStamp);
        return ret;
    }

    private int modificationStamp = 0;

    @Override
    public void setModificationStamp(int modificationStamp) {
        this.modificationStamp = modificationStamp;
    }

    @Override
    public int getModificationStamp() {
        return this.modificationStamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (this.name != null) {
            return this.name;
        }
        return this.executableOrJar;
    }

    public String getNameForUI() {
        if (this.name != null && !this.name.equals(this.executableOrJar)) {
            return this.name + "  (" + this.executableOrJar + ")";
        } else {
            return this.executableOrJar;
        }
    }

    public boolean matchNameBackwardCompatible(String interpreter) {
        if (this.name != null) {
            if (interpreter.equals(this.name)) {
                return true;
            }
        }
        if (PlatformUtils.isWindowsPlatform()) {
            return interpreter.equalsIgnoreCase(executableOrJar);
        }
        return interpreter.equals(executableOrJar);
    }

    public void setStringSubstitutionVariables(Properties stringSubstitutionOriginal) {
        if (stringSubstitutionOriginal == null) {
            this.stringSubstitutionVariables = null;
        } else {
            this.stringSubstitutionVariables = stringSubstitutionOriginal;
        }
    }

    public Properties getStringSubstitutionVariables() {
        return this.stringSubstitutionVariables;
    }

    public void addPredefinedCompletionsPath(String path) {
        this.predefinedCompletionsPath.add(path);
        this.clearBuiltinsCache();
    }

    public List<String> getPredefinedCompletionsPath() {
        return new ArrayList<String>(predefinedCompletionsPath); //Return a copy.
    }

    /**
     * May return null if it doesn't exist.
     * @return the file that matches the passed module name with the predefined builtins.
     */
    public File getPredefinedModule(String moduleName) {
        if (this.predefinedBuiltinsCache == null) {
            this.predefinedBuiltinsCache = new HashMap<String, File>();
            for (String s : this.getPredefinedCompletionsPath()) {
                File f = new File(s);
                if (f.exists()) {
                    File[] predefs = f.listFiles(new FilenameFilter() {

                        //Only accept names ending with .pypredef in the passed dirs
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".pypredef");
                        }
                    });

                    if (predefs != null) {
                        for (File file : predefs) {
                            String n = file.getName();
                            String modName = n.substring(0, n.length() - (".pypredef".length()));
                            this.predefinedBuiltinsCache.put(modName, file);
                        }
                    }
                }
            }
        }

        return this.predefinedBuiltinsCache.get(moduleName);
    }

    public void removePredefinedCompletionPath(String item) {
        this.predefinedCompletionsPath.remove(item);
        this.clearBuiltinsCache();
    }

    private volatile boolean loadFinished = true;

    public void setLoadFinished(boolean b) {
        this.loadFinished = b;
    }

    public boolean getLoadFinished() {
        return this.loadFinished;
    }

    public File getIoDirectory() {
        final File workspaceMetadataFile = PydevPlugin.getWorkspaceMetadataFile(this.getExeAsFileSystemValidPath());
        return workspaceMetadataFile;
    }
}
