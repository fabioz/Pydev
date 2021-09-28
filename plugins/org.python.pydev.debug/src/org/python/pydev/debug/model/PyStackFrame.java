/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 22, 2004
 */
package org.python.pydev.debug.model;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IPyStackFrame;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.GetFileContentsCommand;
import org.python.pydev.debug.model.remote.GetFrameCommand;
import org.python.pydev.debug.model.remote.GetSmartStepIntoVariants;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editorinput.EditorInputFactory;
import org.python.pydev.editorinput.PySourceLocatorPrefs;
import org.python.pydev.shared_core.cache.LRUMap;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Represents a stack entry.
 *
 * Needs to integrate with the source locator
 */
public class PyStackFrame extends PlatformObject
        implements IStackFrame, IVariableLocator, IPyStackFrame, IVariablesContainerParent {

    private String name;
    private PyThread thread;
    private String id;
    private IPath path;
    private int line;
    private final ContainerOfVariables variableContainer = new ContainerOfVariables(this, true);
    private IVariableLocator localsLocator;
    private IVariableLocator globalsLocator;
    private IVariableLocator frameLocator;
    private IVariableLocator expressionLocator;
    private AbstractDebugTarget target;

    public PyStackFrame(PyThread in_thread, String in_id, String name, IPath file, int line,
            AbstractDebugTarget target) {
        this.id = in_id;
        this.name = name;
        this.path = file;
        this.line = line;
        this.thread = in_thread;

        localsLocator = new IVariableLocator() {
            @Override
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tLOCAL";
            }

            @Override
            public String getThreadId() {
                return thread.getId();
            }
        };
        frameLocator = new IVariableLocator() {
            @Override
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tFRAME";
            }

            @Override
            public String getThreadId() {
                return thread.getId();
            }
        };
        globalsLocator = new IVariableLocator() {
            @Override
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tGLOBAL";
            }

            @Override
            public String getThreadId() {
                return thread.getId();
            }
        };
        expressionLocator = new IVariableLocator() {
            @Override
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tEXPRESSION";
            }

            @Override
            public String getThreadId() {
                return thread.getId();
            }
        };
        this.target = target;
    }

    @Override
    public AbstractDebugTarget getTarget() {
        return target;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getThreadId() {
        return this.thread.getId();
    }

    public IVariableLocator getLocalsLocator() {
        return localsLocator;
    }

    public IVariableLocator getFrameLocator() {
        return frameLocator;
    }

    @Override
    public IVariableLocator getGlobalLocator() {
        return globalsLocator;
    }

    public IVariableLocator getExpressionLocator() {
        return expressionLocator;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(IPath path) {
        this.path = path;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public IPath getPath() {
        return path;
    }

    @Override
    public IThread getThread() {
        return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return variableContainer.getVariables();
    }

    public void forceGetNewVariables() {
        variableContainer.forceGetNewVariables();
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return true;
    }

    /**
     * Note: line 1-based.
     */
    @Override
    public int getLineNumber() throws DebugException {
        return line;
    }

    @Override
    public int getCharStart() throws DebugException {
        return -1;
    }

    @Override
    public int getCharEnd() throws DebugException {
        return -1;
    }

    private boolean currentStackFrame = false;

    public void setCurrentStackFrame() {
        this.currentStackFrame = true;
    }

    @Override
    public String getName() throws DebugException {
        String ret = StringUtils.join("", name, " [", path.lastSegment(), ":", Integer.toString(line), "]");
        if (currentStackFrame) {
            ret += "   <-- Current frame";
        }
        return ret;
    }

    @Override
    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return new IRegisterGroup[0];
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

    @Override
    public String getModelIdentifier() {
        return thread.getModelIdentifier();
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return thread.getDebugTarget();
    }

    @Override
    public ILaunch getLaunch() {
        return thread.getLaunch();
    }

    @Override
    public boolean canStepInto() {
        return thread.canStepInto();
    }

    @Override
    public boolean canStepOver() {
        return thread.canStepOver();
    }

    @Override
    public boolean canStepReturn() {
        return thread.canStepReturn();
    }

    @Override
    public boolean isStepping() {
        return thread.isStepping();
    }

    @Override
    public void stepInto() throws DebugException {
        thread.stepInto();
    }

    public void stepIntoTarget(PyEdit pyEdit, int line, String selectedWord, SmartStepIntoVariant target)
            throws DebugException {
        thread.stepIntoTarget(pyEdit, line, selectedWord, target);
    }

    @Override
    public void stepOver() throws DebugException {
        thread.stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        thread.stepReturn();
    }

    @Override
    public boolean canResume() {
        return thread.canResume();
    }

    @Override
    public boolean canSuspend() {
        return thread.canSuspend();
    }

    @Override
    public boolean isSuspended() {
        return thread.isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        thread.resume();
    }

    @Override
    public void suspend() throws DebugException {
        thread.suspend();
    }

    @Override
    public boolean canTerminate() {
        return thread.canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return thread.isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        thread.terminate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        AdapterDebug.print(this, adapter);

        if (adapter.equals(ILaunch.class) || adapter.equals(IResource.class)) {
            return thread.getAdapter(adapter);
        }

        if (adapter.equals(ITaskListResourceAdapter.class)) {
            return null;
        }

        if (adapter.equals(IDebugTarget.class)) {
            return (T) thread.getDebugTarget();
        }

        if (adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)) {
            return (T) this.target.getRunToLineTarget();
        }

        if (adapter.equals(IPropertySource.class) || adapter.equals(ITaskListResourceAdapter.class)
                || adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)) {
            return super.getAdapter(adapter);
        }

        AdapterDebug.printDontKnow(this, adapter);
        // ongoing, I do not fully understand all the interfaces they'd like me to support
        return super.getAdapter(adapter);
    }

    /**
     * fixed - this was bug http://sourceforge.net/tracker/index.php?func=detail&aid=1174821&group_id=85796&atid=577329
     * in the forum (unable to get stack correctly when recursing)
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * fixed - this was bug http://sourceforge.net/tracker/index.php?func=detail&aid=1174821&group_id=85796&atid=577329
     * in the forum (unable to get stack correctly when recursing)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PyStackFrame) {
            PyStackFrame sf = (PyStackFrame) obj;
            return this.id.equals(sf.id) && this.path.toString().equals(sf.path.toString()) && this.line == sf.line
                    && this.getThreadId().equals(sf.getThreadId());
        }
        return false;
    }

    public GetVariableCommand getFrameCommand(AbstractDebugTarget dbg) {
        return new GetFrameCommand(dbg, frameLocator.getPyDBLocation());
    }

    @Override
    public GetVariableCommand getVariableCommand(AbstractDebugTarget target) {
        return getFrameCommand(target);
    }

    @Override
    public String getPyDBLocation() {
        return this.frameLocator.getPyDBLocation();
    }

    public AbstractRemoteDebugger getDebugger() {
        return target.getDebugger();
    }

    @Override
    public String toString() {
        return "PyStackFrame: " + this.id;
    }

    @Override
    public boolean isFileLoadedFromDebugger(IPath path) {
        if (path.lastSegment().startsWith("<") || path.lastSegment().startsWith("&lt;")) {
            return true;
        }

        if (target == null) {
            return false;
        }
        return target.isFileLoadedFromDebugger(path);
    }

    @Override
    public void setFileLoadedFromDebugger(IPath path) {
        if (target != null) {
            target.setFileLoadedFromDebugger(path);
        }
    }

    private String fileContents = null;

    @Override
    public String getFileContents() {
        if (fileContents == null) {
            // send the command, and then busy-wait
            GetFileContentsCommand cmd = new GetFileContentsCommand(target, this.id);

            final Object lock = new Object();
            final String[] response = new String[1];

            cmd.setCompletionListener(new ICommandResponseListener() {

                @Override
                public void commandComplete(AbstractDebuggerCommand cmd) {
                    try {
                        response[0] = ((GetFileContentsCommand) cmd).getResponse();
                    } catch (CoreException e) {
                        response[0] = "";
                    }
                    try {
                        synchronized (lock) {
                            lock.notify();
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            });

            target.postCommand(cmd);
            int timeout = PySourceLocatorPrefs.getFileContentsTimeout();
            long initialTimeMillis = System.currentTimeMillis();
            while (response[0] == null) {
                synchronized (lock) {
                    try {
                        lock.wait(50);
                    } catch (Exception e) {
                        //ignore
                    }
                }
                if (System.currentTimeMillis() - initialTimeMillis > timeout) {
                    break;
                }
            }
            fileContents = response[0];
        }
        return fileContents;
    }

    private IEditorInput editorInput = null;
    private final static LRUMap<File, File> createdInCurrentSession = new LRUMap<File, File>(200);
    private final static Object lock = new Object();

    @Override
    public IEditorInput getEditorInputFromLoadedSource(IPath initialPath) {
        if (editorInput != null) {
            return editorInput;
        }
        try {
            String fileContents = this.getFileContents();
            if (fileContents != null && fileContents.length() > 0) {
                synchronized (lock) {
                    this.setFileLoadedFromDebugger(initialPath);
                    String lastSegment = path.lastSegment();
                    if (lastSegment.length() > 255) {
                        lastSegment = lastSegment.substring(0, 255);
                    }
                    lastSegment = lastSegment.replaceAll("[\\\\/:*?\"<>|]", "_"); // make it a valid filename.
                    File workspaceMetadataFile = CorePlugin.getWorkspaceMetadataFile("temporary_files");
                    if (!workspaceMetadataFile.exists()) {
                        workspaceMetadataFile.mkdirs();
                    }
                    File file = new File(workspaceMetadataFile, lastSegment);
                    for (int i = 0; i < 1000; i++) {
                        if (file.exists()) {
                            if (Arrays.equals(FileUtils.getFileContentsBytes(file), fileContents.getBytes())) {
                                createdInCurrentSession.put(file, file);
                                editorInput = EditorInputFactory.create(file, true);
                                return editorInput;
                            }

                            if (!createdInCurrentSession.containsKey(file)) {
                                createdInCurrentSession.put(file, file);
                                file.delete();
                                FileUtils.writeStrToFile(fileContents, file);
                                try {
                                    file.setReadOnly();
                                } catch (Exception e) {
                                    Log.log(e);
                                }
                                editorInput = EditorInputFactory.create(file, true);
                                return editorInput;
                            } else {
                                // It was created in this session and it doesn't match, so, generate a new name.
                                Tuple<String, String> splitted = StringUtils.splitExt(lastSegment);
                                if (splitted.o2.isEmpty()) {
                                    file = new File(workspaceMetadataFile,
                                            StringUtils.format("%s-%s", splitted.o1, i));

                                } else {
                                    file = new File(workspaceMetadataFile,
                                            StringUtils.format("%s-%s.%s", splitted.o1, i, splitted.o2));

                                }
                            }

                        } else {
                            // File does not exist...
                            createdInCurrentSession.put(file, file);
                            FileUtils.writeStrToFile(fileContents, file);
                            try {
                                file.setReadOnly();
                            } catch (Exception e) {
                                Log.log(e);
                            }
                            editorInput = EditorInputFactory.create(file, true);
                            return editorInput;
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    public SmartStepIntoVariant[] getStepIntoTargets() {
        GetSmartStepIntoVariants cmd = new GetSmartStepIntoVariants(target, this.thread.getId(), this.id, 0, 99999999);

        target.postCommand(cmd);
        try {
            cmd.waitUntilDone(PySourceLocatorPrefs.getFileContentsTimeout());
        } catch (InterruptedException e) {
            Log.log(e); // I.e.: it wasn't able to get it.
        }
        return cmd.getResponse();
    }
}
