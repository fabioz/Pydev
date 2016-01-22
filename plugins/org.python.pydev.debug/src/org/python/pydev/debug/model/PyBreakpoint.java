/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 28, 2004
 */
package org.python.pydev.debug.model;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Represents python breakpoint.
 *
 */
public class PyBreakpoint extends LineBreakpoint {
    /**
     * Marker attribute storing the path id of some external file
     */
    public static final String PY_BREAK_EXTERNAL_PATH_ID = "org.python.pydev.debug.PYDEV_EXTERNAL_PATH_ID";

    /**
     * Can be null/not set (which signals python-line) or django-line
     */
    public static final String PY_BREAK_TYPE = "org.python.pydev.debug.PY_BREAK_TYPE";
    public static final String PY_BREAK_TYPE_PYTHON = "python-line";
    public static final String PY_BREAK_TYPE_DJANGO = "django-line";

    static public final String PY_BREAK_MARKER = "org.python.pydev.debug.pyStopBreakpointMarker";

    static public final String DJANGO_BREAK_MARKER = "org.python.pydev.debug.djangoStopBreakpointMarker";

    static public final String PY_CONDITIONAL_BREAK_MARKER = "org.python.pydev.debug.pyConditionalStopBreakpointMarker";

    /**
     * Breakpoint attribute storing a breakpoint's conditional expression
     * (value <code>"org.eclipse.jdt.debug.core.condition"</code>). This attribute is stored as a
     * <code>String</code>.
     */
    protected static final String CONDITION = "org.python.pydev.debug.condition"; //$NON-NLS-1$
    /**
     * Breakpoint attribute storing a breakpoint's condition enablement
     * (value <code>"org.eclipse.jdt.debug.core.conditionEnabled"</code>). This attribute is stored as an
     * <code>boolean</code>.
     */
    protected static final String CONDITION_ENABLED = "org.python.pydev.debug.conditionEnabled";

    public static int nextId = 0;
    public static final Object lock = new Object();

    public final int breakpointId;

    public PyBreakpoint() {
        synchronized (lock) {
            this.breakpointId = nextId++;
        }
    }

    public String getModelIdentifier() {
        return PyDebugModelPresentation.PY_DEBUG_MODEL_ID;
    }

    /**
     * @return the file to be debugged or null if it cannot be determined.
     */
    public String getFile() {
        IMarker marker = getMarker();
        IResource r = marker.getResource();
        if (r instanceof IFile) {
            IPath location = r.getLocation();
            if (location == null) {
                return null;
            }
            return location.toOSString();
        } else {
            //it's an external file...
            try {
                return (String) marker.getAttribute(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private IDocument getDocument() {
        IMarker marker = getMarker();
        IResource r = marker.getResource();
        if (r instanceof IFile) {
            return FileUtilsFileBuffer.getDocFromResource(r);
        } else {
            //it's an external file...
            try {
                return FileUtilsFileBuffer
                        .getDocFromFile(new File((String) marker.getAttribute(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return The nature to be used for this breakpoint or null if it cannot be determined.
     */
    private IPythonNature getPythonNature() {
        IMarker marker = getMarker();
        IPythonNature nature = PythonNature.getPythonNature(marker.getResource());
        if (nature == null) {
            try {
                String externalPath = (String) marker.getAttribute(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID);
                if (externalPath != null) {
                    Tuple<IPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(new File(externalPath));
                    if (infoForFile != null) {
                        nature = infoForFile.o1;
                    }
                }
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
        return nature;
    }

    public String getType() {
        IMarker marker = getMarker();
        Object attribute = null;
        if (marker != null) {
            try {
                attribute = marker.getAttribute(PyBreakpoint.PY_BREAK_TYPE);
            } catch (CoreException e) {
                Log.log(e);
            }
        }
        if (attribute != null
                && (attribute.equals(PyBreakpoint.PY_BREAK_TYPE_DJANGO) || attribute
                        .equals(PyBreakpoint.PY_BREAK_TYPE_PYTHON))) {
            return (String) attribute;
        }
        //default
        return PyBreakpoint.PY_BREAK_TYPE_PYTHON;
    }

    public Object getLine() {
        try {
            return getMarker().getAttribute(IMarker.LINE_NUMBER);
        } catch (CoreException e) {
            return "";
        }
    }

    public boolean supportsCondition() {
        return true;
    }

    public String getCondition() throws DebugException {
        return ensureMarker().getAttribute(CONDITION, null);
    }

    public boolean isConditionEnabled() throws DebugException {
        return ensureMarker().getAttribute(CONDITION_ENABLED, false);
    }

    public void setConditionEnabled(boolean conditionEnabled) throws CoreException {
        setAttributes(new String[] { CONDITION_ENABLED }, new Object[] { new Boolean(conditionEnabled) });
    }

    public void setCondition(String condition) throws CoreException {
        if (condition != null && condition.trim().length() == 0) {
            condition = null;
        }
        setAttributes(new String[] { CONDITION }, new Object[] { condition });
    }

    /**
     * Returns the marker associated with this breakpoint.
     *
     * @return breakpoint marker
     * @exception DebugException if no marker is associated with
     *  this breakpoint or the associated marker does not exist
     */
    @Override
    protected IMarker ensureMarker() throws DebugException {
        IMarker m = getMarker();
        if (m == null || !m.exists()) {
            throw new DebugException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
                    DebugException.REQUEST_FAILED, "Breakpoint_no_associated_marker", null));
        }
        return m;
    }

    private String functionName;
    private long lastModifiedTimeCached;

    /**
     * @return the function name for this breakpoint.
     *
     * A return of "None" signals that we couldn't discover the function name (so, we should try to match things in the whole
     * file, and not only in the given context, as we don't know which context it is)
     */
    public String getFunctionName() {
        String fileStr = getFile();
        File file = fileStr != null ? new File(fileStr) : null;
        if (file == null || !file.exists()) {
            return "None";
        }

        if (FileUtils.lastModified(file) == lastModifiedTimeCached) {
            return functionName;
        }

        try {
            IPythonNature nature = getPythonNature();
            if (nature == null) {
                lastModifiedTimeCached = 0;
                return "None";
            }
            ICodeCompletionASTManager astManager = nature.getAstManager();
            if (astManager == null) {
                lastModifiedTimeCached = 0;
                return "None";
            }
            //Only mark it as found if we were able to get the python nature (otherwise, this could change later
            //if requesting during a setup)
            if (nature.startRequests()) { //start requests, as we'll ask for resolve and get module.
                SourceModule sourceModule = null;
                try {
                    String modName = nature.resolveModule(fileStr);
                    if (modName != null) {
                        //when all is set up, this is the most likely path we're going to use
                        //so, we shouldn't have delays when the module is changed, as it's already
                        //ok for use.
                        IModule module = astManager.getModule(modName, nature, true);
                        if (module instanceof SourceModule) {
                            sourceModule = (SourceModule) module;
                        }
                    }
                } finally {
                    nature.endRequests();
                }
                lastModifiedTimeCached = FileUtils.lastModified(file);

                if (sourceModule == null) {
                    //the text for the breakpoint requires the function name, and it may be requested before
                    //the ast manager is actually restored (so, modName is None, and we have little alternative
                    //but making a parse to get the function name)
                    IDocument doc = getDocument();
                    sourceModule = AbstractModule.createModuleFromDoc("", null, doc, nature, true);
                }

                int lineToUse = getLineNumber() - 1;

                if (sourceModule == null || sourceModule.getAst() == null || lineToUse < 0) {
                    functionName = "None";
                    return functionName;
                }

                SimpleNode ast = sourceModule.getAst();

                functionName = NodeUtils.getContextName(lineToUse, ast);
                if (functionName == null) {
                    functionName = ""; //global context
                }
                return functionName;

            }
            //If it was found, it would've already returned. So, match anything as we couldn't determine it.
            functionName = "None";

        } catch (Exception e) {
            //Some error happened determining it. Match anything.
            Log.log("Error determining breakpoint context. Breakpoint at: " + file + " will match any context.", e);
            functionName = "None";
        }
        return functionName;
    }

}
