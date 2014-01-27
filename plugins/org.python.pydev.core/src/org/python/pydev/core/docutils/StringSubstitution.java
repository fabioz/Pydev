/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Implements a part of IStringVariableManager (just the performStringSubstitution methods).
 */
public class StringSubstitution {

    private Map<String, String> variableSubstitution = null;

    public StringSubstitution(IPythonNature nature) {
        if (nature != null) {
            try {
                IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                IProject project = nature.getProject(); //note: project can be null when creating a new project and receiving a system nature.
                variableSubstitution = pythonPathNature.getVariableSubstitution();

                try {
                    IPathVariableManager projectPathVarManager = null;
                    try {
                        if (project != null) {
                            projectPathVarManager = project.getPathVariableManager();
                        }
                    } catch (Throwable e1) {
                        //Ignore: getPathVariableManager not available on earlier Eclipse versions.
                    }
                    String[] pathVarNames = null;
                    if (projectPathVarManager != null) {
                        pathVarNames = projectPathVarManager.getPathVariableNames();
                    }
                    //The usual path var names are:

                    //ECLIPSE_HOME, PARENT_LOC, WORKSPACE_LOC, PROJECT_LOC
                    //Other possible variables may be defined in General > Workspace > Linked Resources.

                    //We also add PROJECT_DIR_NAME (so, we can define a source folder with /${PROJECT_DIR_NAME}
                    if (project != null && !variableSubstitution.containsKey("PROJECT_DIR_NAME")) {
                        IPath location = project.getFullPath();
                        if (location != null) {
                            variableSubstitution.put("PROJECT_DIR_NAME", location.lastSegment());
                        }
                    }

                    if (pathVarNames != null) {
                        URI uri = null;
                        String var = null;
                        String path = null;
                        for (int i = 0; i < pathVarNames.length; i++) {
                            try {
                                var = pathVarNames[i];
                                uri = projectPathVarManager.getURIValue(var);
                                if (uri != null) {
                                    String scheme = uri.getScheme();
                                    if (scheme != null && scheme.equalsIgnoreCase("file")) {
                                        path = uri.getPath();
                                        if (path != null && !variableSubstitution.containsKey(var)) {
                                            variableSubstitution.put(var, new File(uri).toString());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.log(e);
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * Replaces with all variables (the ones for this class and the ones in the VariablesPlugin)
     *
     *
     * Recursively resolves and replaces all variable references in the given
     * expression with their corresponding values. Allows the client to control
     * whether references to undefined variables are reported as an error (i.e.
     * an exception is thrown).
     *
     * @param expression expression referencing variables
     * @param reportUndefinedVariables whether a reference to an undefined variable
     *  is to be considered an error (i.e. throw an exception)
     * @return expression with variable references replaced with variable values
     * @throws CoreException if unable to resolve the value of one or more variables
     */
    public String performStringSubstitution(String expression, boolean reportUndefinedVariables) throws CoreException {
        VariablesPlugin plugin = VariablesPlugin.getDefault();
        expression = performPythonpathStringSubstitution(expression);
        expression = plugin.getStringVariableManager().performStringSubstitution(expression, reportUndefinedVariables);
        return expression;
    }

    /**
     * String substitution for the pythonpath does not use the default eclipse string substitution (only variables
     * defined explicitly in this class)
     */
    public String performPythonpathStringSubstitution(String expression) throws CoreException {
        if (variableSubstitution != null && variableSubstitution.size() > 0 && expression != null
                && expression.length() > 0) {
            //Only throw exception here if the
            expression = new StringSubstitutionEngine().performStringSubstitution(expression, true,
                    variableSubstitution);
        }
        return expression;

    }

    /**
     * Recursively resolves and replaces all variable references in the given
     * expression with their corresponding values. Reports errors for references
     * to undefined variables (equivalent to calling
     * <code>performStringSubstitution(expression, true)</code>).
     *
     * @param expression expression referencing variables
     * @return expression with variable references replaced with variable values
     * @throws CoreException if unable to resolve the value of one or more variables
     */
    public String performStringSubstitution(String expression) throws CoreException {
        return performStringSubstitution(expression, true);
    }

    /**
     * Performs string substitution for context and value variables.
     */
    @SuppressWarnings("unchecked")
    class StringSubstitutionEngine {

        // delimiters
        private static final String VARIABLE_START = "${"; //$NON-NLS-1$
        private static final char VARIABLE_END = '}';
        private static final char VARIABLE_ARG = ':';
        // parsing states
        private static final int SCAN_FOR_START = 0;
        private static final int SCAN_FOR_END = 1;

        /**
         * Resulting string
         */
        private StringBuffer fResult;

        /**
         * Whether substitutions were performed
         */
        private boolean fSubs;

        /**
         * Stack of variables to resolve
         */
        private Stack<VariableReference> fStack;

        class VariableReference {

            // the text inside the variable reference
            private StringBuffer fText;

            public VariableReference() {
                fText = new StringBuffer();
            }

            public void append(String text) {
                fText.append(text);
            }

            public String getText() {
                return fText.toString();
            }

        }

        /**
         * Performs recursive string substitution and returns the resulting string.
         *
         * @param expression expression to resolve
         * @param reportUndefinedVariables whether to report undefined variables as an error
         * @param variableSubstitution registry of variables
         * @return the resulting string with all variables recursively
         *  substituted
         * @exception CoreException if unable to resolve a referenced variable or if a cycle exists
         *  in referenced variables
         */

        public String performStringSubstitution(String expression, boolean resolveVariables,
                Map<String, String> variableSubstitution) throws CoreException {
            substitute(expression, resolveVariables, variableSubstitution);
            List resolvedVariableSets = new ArrayList();
            while (fSubs) {
                HashSet resolved = substitute(fResult.toString(), true, variableSubstitution);

                for (int i = resolvedVariableSets.size() - 1; i >= 0; i--) {

                    HashSet prevSet = (HashSet) resolvedVariableSets.get(i);

                    if (prevSet.equals(resolved)) {
                        HashSet conflictingSet = new HashSet();
                        for (; i < resolvedVariableSets.size(); i++) {
                            conflictingSet.addAll((HashSet) resolvedVariableSets.get(i));
                        }

                        StringBuffer problemVariableList = new StringBuffer();
                        for (Iterator it = conflictingSet.iterator(); it.hasNext();) {
                            problemVariableList.append(it.next().toString());
                            problemVariableList.append(", "); //$NON-NLS-1$
                        }
                        problemVariableList.setLength(problemVariableList.length() - 2); //truncate the last ", "
                        throw new CoreException(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(),
                                VariablesPlugin.REFERENCE_CYCLE_ERROR,
                                StringUtils.format("Cycle error on:",
                                        problemVariableList.toString()), null));
                    }
                }

                resolvedVariableSets.add(resolved);
            }
            return fResult.toString();
        }

        /**
         * Makes a substitution pass of the given expression returns a Set of the variables that were resolved in this
         *  pass
         *
         * @param expression source expression
         * @param resolveVariables whether to resolve the value of any variables
         * @exception CoreException if unable to resolve a variable
         */
        private HashSet<String> substitute(String expression, boolean resolveVariables,
                Map<String, String> variableSubstitution)
                throws CoreException {
            fResult = new StringBuffer(expression.length());
            fStack = new Stack<VariableReference>();
            fSubs = false;

            HashSet<String> resolvedVariables = new HashSet<String>();

            int pos = 0;
            int state = SCAN_FOR_START;
            while (pos < expression.length()) {
                switch (state) {
                    case SCAN_FOR_START:
                        int start = expression.indexOf(VARIABLE_START, pos);
                        if (start >= 0) {
                            int length = start - pos;
                            // copy non-variable text to the result
                            if (length > 0) {
                                fResult.append(expression.substring(pos, start));
                            }
                            pos = start + 2;
                            state = SCAN_FOR_END;

                            fStack.push(new VariableReference());
                        } else {
                            // done - no more variables
                            fResult.append(expression.substring(pos));
                            pos = expression.length();
                        }
                        break;
                    case SCAN_FOR_END:
                        // be careful of nested variables
                        start = expression.indexOf(VARIABLE_START, pos);
                        int end = expression.indexOf(VARIABLE_END, pos);
                        if (end < 0) {
                            // variables are not completed
                            VariableReference tos = fStack.peek();
                            tos.append(expression.substring(pos));
                            pos = expression.length();
                        } else {
                            if (start >= 0 && start < end) {
                                // start of a nested variable
                                int length = start - pos;
                                if (length > 0) {
                                    VariableReference tos = fStack.peek();
                                    tos.append(expression.substring(pos, start));
                                }
                                pos = start + 2;
                                fStack.push(new VariableReference());
                            } else {
                                // end of variable reference
                                VariableReference tos = fStack.pop();
                                String substring = expression.substring(pos, end);
                                tos.append(substring);
                                resolvedVariables.add(substring);

                                pos = end + 1;
                                String value = resolve(tos, resolveVariables, variableSubstitution);
                                if (value == null) {
                                    value = ""; //$NON-NLS-1$
                                }
                                if (fStack.isEmpty()) {
                                    // append to result
                                    fResult.append(value);
                                    state = SCAN_FOR_START;
                                } else {
                                    // append to previous variable
                                    tos = fStack.peek();
                                    tos.append(value);
                                }
                            }
                        }
                        break;
                }
            }
            // process incomplete variable references
            while (!fStack.isEmpty()) {
                VariableReference tos = fStack.pop();
                if (fStack.isEmpty()) {
                    fResult.append(VARIABLE_START);
                    fResult.append(tos.getText());
                } else {
                    VariableReference var = fStack.peek();
                    var.append(VARIABLE_START);
                    var.append(tos.getText());
                }
            }

            return resolvedVariables;
        }

        /**
         * Resolve and return the value of the given variable reference,
         * possibly <code>null</code>.
         *
         * @param var
         * @param resolveVariables whether to resolve the variables value or just to validate that this variable is valid
         * @param variableSubstitution variable registry
         * @return variable value, possibly <code>null</code>
         * @exception CoreException if unable to resolve a value
         */
        private String resolve(VariableReference var, boolean resolveVariables, Map<String, String> variableSubstitution)
                throws CoreException {
            String text = var.getText();
            int pos = text.indexOf(VARIABLE_ARG);
            String name = null;
            String arg = null;
            if (pos > 0) {
                name = text.substring(0, pos);
                pos++;
                if (pos < text.length()) {
                    arg = text.substring(pos);
                }
            } else {
                name = text;
            }
            String valueVariable = variableSubstitution.get(name);
            if (valueVariable == null) {
                //leave as is
                return getOriginalVarText(var);
            }

            if (arg == null) {
                if (resolveVariables) {
                    fSubs = true;
                    return valueVariable;
                }
                //leave as is
                return getOriginalVarText(var);
            }
            // error - an argument specified for a value variable
            throw new CoreException(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(),
                    VariablesPlugin.INTERNAL_ERROR, "Error substituting: " + name + " var: " + valueVariable, null));
        }

        private String getOriginalVarText(VariableReference var) {
            StringBuffer res = new StringBuffer(var.getText());
            res.insert(0, VARIABLE_START);
            res.append(VARIABLE_END);
            return res.toString();
        }
    }

}
