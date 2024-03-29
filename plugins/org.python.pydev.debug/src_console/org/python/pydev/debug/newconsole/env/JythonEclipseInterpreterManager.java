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
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IInterpreterManagerListener;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModuleRequestState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TokensList;

/**
 * Interpreter manager created so that we can use the eclipse process to access the internal jython.
 */
public class JythonEclipseInterpreterManager implements IInterpreterManager {

    @Override
    public int getInterpreterType() {

        return IInterpreterManager.INTERPRETER_TYPE_JYTHON_ECLIPSE;
    }

    public IInterpreterInfo getDefaultInterpreterInfo() throws MisconfigurationException {

        return getInterpreterInfos()[0];
    }

    @Override
    public IInterpreterInfo[] getInterpreterInfos() {
        InterpreterInfo interpreterInfo = new InterpreterInfo("2.1", "Jython Eclipse", new ArrayList<String>());
        return new IInterpreterInfo[] { interpreterInfo };
    }

    @Override
    public void setInfos(IInterpreterInfo[] infos, Set<String> interpreterNamesToRestore, IProgressMonitor monitor) {
        //do nothing
    }

    @Override
    public IInterpreterInfo getInterpreterInfo(String nameOrExecutableOrJar, IProgressMonitor monitor)
            throws MisconfigurationException {

        return null;
    }

    @Override
    public void addListener(IInterpreterManagerListener listener) {

    }

    public IInterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor) throws MisconfigurationException {

        return null;
    }

    @Override
    public IInterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor, boolean askUser) {

        return null;
    }

    public void addInterpreterInfo(IInterpreterInfo info) {

    }

    @Override
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

    @Override
    public String getManagerRelatedName() {

        return null;
    }

    @Override
    public String getPersistedString() {

        return null;
    }

    public void setPersistedString(String s) {

    }

    @Override
    public boolean isConfigured() {

        return true;
    }

    public boolean hasInfoOnInterpreter(String interpreter) {

        return false;
    }

    @Override
    public void clearBuiltinCompletions(String projectInterpreterName) {

    }

    @Override
    public TokensList getBuiltinCompletions(String projectInterpreterName, IModuleRequestState moduleRequest) {

        return null;
    }

    @Override
    public IModule getBuiltinMod(String projectInterpreterName, IModuleRequestState moduleRequest) {

        return null;
    }

    @Override
    public void clearBuiltinMod(String projectInterpreterName) {

    }

    @Override
    public void clearCaches() {

    }

    public void saveInterpretersInfoModulesManager() {

    }

    @Override
    public IInterpreterInfo getDefaultInterpreterInfo(boolean autoConfigure) throws MisconfigurationException {
        return getDefaultInterpreterInfo();
    }

}
