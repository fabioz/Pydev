/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on May 6, 2004
 */
package org.python.pydev.debug.model;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editorinput.EditorInputFactory;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.ImageCache;

/**
 * Provides decoration for model elements in the debugger interface.
 */
@SuppressWarnings("restriction")
public class PyDebugModelPresentation implements IDebugModelPresentation {

    static public String PY_DEBUG_MODEL_ID = "org.python.pydev.debug";

    /**
     * Listeners compared by identity
     */
    protected ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);

    protected boolean displayVariableTypeNames = false; // variables display attribute

    private boolean returnNullForDefaultHandling;

    private DefaultLabelProvider defaultDebugLabelProvider;

    public PyDebugModelPresentation() {
        this(true);
    }

    public PyDebugModelPresentation(boolean returnNullForDefaultHandling) {
        this.returnNullForDefaultHandling = returnNullForDefaultHandling;
        if (!returnNullForDefaultHandling) {
            try {
                defaultDebugLabelProvider = new DefaultLabelProvider();
            } catch (Throwable e) {
                //As it's discouraged access, let's prevent from having an error if it disappears in the future.
                Log.log(e);
            }
        }
    }

    /**
     * @return the image for some debug element
     */
    public Image getImage(Object element) {
        ImageCache imageCache = PydevDebugPlugin.getImageCache();

        if (element instanceof PyBreakpoint) {
            try {
                PyBreakpoint pyBreakpoint = (PyBreakpoint) element;

                if (pyBreakpoint.isEnabled()) {
                    if (pyBreakpoint.getType().equals(PyBreakpoint.PY_BREAK_TYPE_DJANGO)) {
                        return imageCache.get("icons/breakmarker_django.png");

                    } else {
                        if (pyBreakpoint.isConditionEnabled()) {
                            return imageCache.get("icons/breakmarker_conditional.gif");
                        } else {
                            return imageCache.get("icons/breakmarker.gif");
                        }
                    }
                } else {
                    if (pyBreakpoint.getType().equals(PyBreakpoint.PY_BREAK_TYPE_DJANGO)) {
                        return imageCache.get("icons/breakmarker_django_gray.png");

                    } else {
                        if (pyBreakpoint.isConditionEnabled()) {
                            return imageCache.get("icons/breakmarker_gray_conditional.gif");
                        } else {
                            return imageCache.get("icons/breakmarker_gray.gif");
                        }
                    }
                }

            } catch (CoreException e) {
                PydevDebugPlugin.log(IStatus.ERROR, "getImage error", e);
            }

        } else if (element instanceof PyVariableCollection) {
            return imageCache.get("icons/greendot_big.gif");

        } else if (element instanceof PyVariable) {
            return imageCache.get("icons/greendot.gif");

        } else if (element instanceof CaughtException) {
            return imageCache.get("icons/python_exception_breakpoint.png");

        } else if (element instanceof PyDebugTarget || element instanceof PyThread || element instanceof PyStackFrame) {
            if (element instanceof PyThread) {
                if (((PyThread) element).isCustomFrame) {
                    return imageCache.get("icons/tasklet.png");
                }
            }

            if (returnNullForDefaultHandling) {
                return null;
            }
            if (defaultDebugLabelProvider != null) {
                return defaultDebugLabelProvider.getImage(element);
            }
        }

        return null;
    }

    /**
     * @return the text for some debug element
     */
    public String getText(Object element) {
        if (element instanceof PyBreakpoint) {
            PyBreakpoint pyBreakpoint = (PyBreakpoint) element;
            IMarker marker = ((PyBreakpoint) element).getMarker();
            try {
                Map<String, Object> attrs = marker.getAttributes();

                //get the filename
                String ioFile = pyBreakpoint.getFile();
                String fileName = "unknown";
                if (ioFile != null) {
                    File file = new File(ioFile);
                    fileName = file.getName();
                }

                //get the line number
                Object lineNumber = attrs.get(IMarker.LINE_NUMBER);
                String functionName = pyBreakpoint.getFunctionName();

                if (lineNumber == null) {
                    lineNumber = "unknown";
                }

                //get the location
                String location = fileName + ":" + lineNumber.toString();
                if (functionName == null) {
                    return location;
                } else {
                    return functionName + " [" + location + "]";
                }

            } catch (CoreException e) {
                PydevDebugPlugin.log(IStatus.ERROR, "error retreiving marker attributes", e);
                return "error";
            }
        } else if (element instanceof AbstractDebugTarget || element instanceof PyStackFrame
                || element instanceof PyThread) {
            if (returnNullForDefaultHandling) {
                return null;
            }
            if (element instanceof AbstractDebugTarget) {
                AbstractDebugTarget abstractDebugTarget = (AbstractDebugTarget) element;
                try {
                    return abstractDebugTarget.getName();
                } catch (DebugException e) {
                    Log.log(e);
                }
            }
            if (element instanceof PyStackFrame) {
                PyStackFrame pyStackFrame = (PyStackFrame) element;
                try {
                    return pyStackFrame.getName();
                } catch (DebugException e) {
                    Log.log(e);
                }
            }
            if (element instanceof PyThread) {
                PyThread pyThread = (PyThread) element;
                try {
                    return pyThread.getName();
                } catch (DebugException e) {
                    Log.log(e);
                }
            }
            return null; // defaults work

        } else if (element instanceof CaughtException) {
            CaughtException caughtException = (CaughtException) element;
            String text = this.getText(caughtException.threadNstack.thread);
            return StringUtils.join("",
                    new String[] { caughtException.excType, ": ", caughtException.msg, " - ", text });

        } else if (element instanceof IVariable) {
            if (returnNullForDefaultHandling) {
                return null;
            }
            IVariable iVariable = (IVariable) element;
            return getVariableText(iVariable); // defaults are fine

        } else if (element instanceof IWatchExpression) {
            try {
                IWatchExpression watch_expression = (IWatchExpression) element;
                IValue value = watch_expression.getValue();
                if (value != null) {
                    return "\"" + watch_expression.getExpressionText() + "\"= " + value.getValueString();
                } else {
                    return null;
                }
            } catch (DebugException e) {
                return null;
            }

        } else if (element == null) {
            PydevDebugPlugin.log(IStatus.ERROR, "PyDebugModelPresentation: element == null", null);
            return null;

        } else {
            PydevDebugPlugin.log(IStatus.ERROR, "PyDebugModelPresentation:\nclass not expected for presentation:"
                    + element.getClass() + "\n(returning default presentation).", null);
            return null;
        }
    }

    protected String getVariableText(IVariable variable) {
        try {
            return StringUtils.join(" = ", variable.getName(), variable.getValue().getValueString());
        } catch (DebugException e) {
            Log.log(e);
        }
        try {
            return variable.getName();
        } catch (DebugException e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * We've got some work to do to replicate here, because we can't return null, and have LazyModel presentation do the
     * default
     */
    public void computeDetail(IValue value, IValueDetailListener listener) {
        if (value instanceof PyVariable) {
            try {
                ((PyVariable) value).getVariables();
                listener.detailComputed(value, ((PyVariable) value).getDetailText());
            } catch (DebugException e) {
                PydevDebugPlugin.errorDialog("Unexpected error fetching variable", e);
            }
        }
    }

    /**
     * Returns editor to be displayed
     */
    public IEditorInput getEditorInput(Object element) {
        if (element instanceof PyBreakpoint) {
            String file = ((PyBreakpoint) element).getFile();
            if (file != null) {
                return EditorInputFactory.create(new File(file), false);

                //We should not open the editor here, just create the input... the debug framework opens it later on.
                //IPath path = new Path(file);
                //IEditorPart part = PyOpenEditor.doOpenEditor(path);
                //return part.getEditorInput();
            }
        }
        return null;
    }

    /**
     * @see org.eclipse.debug.ui.ISourcePresentation#getEditorInput
     */
    public String getEditorId(IEditorInput input, Object element) {
        return PyEdit.EDITOR_ID;
    }

    public void setAttribute(String attribute, Object value) {
        if (attribute.equals(IDebugModelPresentation.DISPLAY_VARIABLE_TYPE_NAMES)) {
            displayVariableTypeNames = ((Boolean) value).booleanValue();
        } else {
            Log.log("setattribute");
        }
    }

    public void addListener(ILabelProviderListener listener) {
        fListeners.add(listener);
    }

    public void removeListener(ILabelProviderListener listener) {
        fListeners.remove(listener);
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
        // Not really sure what this does. see IBaseLabelProvider:isLabelProperty
        return false;
    }
}
