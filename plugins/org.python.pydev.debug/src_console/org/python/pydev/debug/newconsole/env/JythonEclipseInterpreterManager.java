/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IInterpreterManagerListener;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * Interpreter manager created so that we can use the eclipse process to access the internal jython.
 */
public class JythonEclipseInterpreterManager implements IInterpreterManager {

    public int getInterpreterType() {

        return IInterpreterManager.INTERPRETER_TYPE_JYTHON_ECLIPSE;
    }

    public IInterpreterInfo getDefaultInterpreterInfo() throws MisconfigurationException {

        return getInterpreterInfos()[0];
    }

    public IInterpreterInfo[] getInterpreterInfos() {
        InterpreterInfo interpreterInfo = new InterpreterInfo("2.1", "Jython Eclipse", new ArrayList<String>());
        return new IInterpreterInfo[] { interpreterInfo };
    }

    public void setInfos(IInterpreterInfo[] infos, Set<String> interpreterNamesToRestore, IProgressMonitor monitor) {
        //do nothing
    }

    public IInterpreterInfo getInterpreterInfo(String nameOrExecutableOrJar, IProgressMonitor monitor)
            throws MisconfigurationException {

        return null;
    }

    public void addListener(IInterpreterManagerListener listener) {

    }

    public IInterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor) throws MisconfigurationException {

        return null;
    }

    public IInterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor, boolean askUser) {

        return null;
    }

    public void addInterpreterInfo(IInterpreterInfo info) {

    }

    public IInterpreterInfo[] getInterpretersFromPersistedString(String persisted) {

        return null;
    }

    public String getStringToPersist(IInterpreterInfo[] executables) {

        return null;
    }

    public boolean hasInfoOnDefaultInterpreter(IPythonNature nature) {

        return false;
    }

    public void setInfos(List<IInterpreterInfo> allButTheseInterpreters) {

    }

    public void restorePythopathForInterpreters(IProgressMonitor monitor, Set<String> interpreterNamesToRestore) {

    }

    public String getManagerRelatedName() {

        return null;
    }

    public String getPersistedString() {

        return null;
    }

    public void setPersistedString(String s) {

    }

    public boolean isConfigured() {

        return true;
    }

    public boolean hasInfoOnInterpreter(String interpreter) {

        return false;
    }

    public void clearBuiltinCompletions(String projectInterpreterName) {

    }

    public IToken[] getBuiltinCompletions(String projectInterpreterName) {

        return null;
    }

    public IModule getBuiltinMod(String projectInterpreterName) {

        return null;
    }

    public void clearBuiltinMod(String projectInterpreterName) {

    }

    public void clearCaches() {

    }

    public void saveInterpretersInfoModulesManager() {

    }

    public IInterpreterInfo getDefaultInterpreterInfo(boolean autoConfigure) throws MisconfigurationException {
        return getDefaultInterpreterInfo();
    }

}
