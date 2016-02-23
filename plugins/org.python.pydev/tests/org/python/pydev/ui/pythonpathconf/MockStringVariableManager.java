/******************************************************************************
* Copyright (C) 2012  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.ui.pythonpathconf;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IStringVariable;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.IValueVariableListener;

/**
 * A mock implementation of IStringVariableManager suitable to test {@link InterpreterInfo#updateEnv(String[])}
 */
final class MockStringVariableManager implements IStringVariableManager {

    private Map<String, String> mockVariables = new HashMap<String, String>();

    public void addMockVariable(String variable, String value) {
        mockVariables.put(variable, value);
    }

    @Override
    public String performStringSubstitution(String expression, boolean reportUndefinedVariables) throws CoreException {
        if (reportUndefinedVariables) {
            throw new AssertionFailedError("reportUndefinedVariables only supports false");
        }
        for (Entry<String, String> entry : mockVariables.entrySet()) {
            expression = expression.replaceAll(Pattern.quote("${" + entry.getKey() + "}"),
                    Matcher.quoteReplacement(entry.getValue()));
        }
        return expression;
    }

    @Override
    public String performStringSubstitution(String expression) throws CoreException {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public void validateStringVariables(String expression) throws CoreException {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public void removeVariables(IValueVariable[] variables) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public void removeValueVariableListener(IValueVariableListener listener) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public IValueVariable newValueVariable(String name, String description, boolean readOnly, String value) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public IValueVariable newValueVariable(String name, String description) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public IStringVariable[] getVariables() {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public IValueVariable[] getValueVariables() {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public IValueVariable getValueVariable(String name) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public IDynamicVariable[] getDynamicVariables() {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public IDynamicVariable getDynamicVariable(String name) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public String getContributingPluginId(IStringVariable variable) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public String generateVariableExpression(String varName, String arg) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public void addVariables(IValueVariable[] variables) throws CoreException {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

    @Override
    public void addValueVariableListener(IValueVariableListener listener) {
        throw new AssertionFailedError("Unexpected method call in MockStringVariableManager");
    }

}