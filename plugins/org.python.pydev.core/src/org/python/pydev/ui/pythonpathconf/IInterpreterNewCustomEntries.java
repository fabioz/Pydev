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

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of this class can provide new settings that get
 * added to every new interpreter configured in the preference 
 * pages.
 * <p>
 * Instances of this class should be registered with the
 * org.python.pydev.pydev_interpreter_new_custom_entries extension point.
 */
public interface IInterpreterNewCustomEntries {

    /**
     * Return additional entries to add to the Python Path.
     * 
     * @return a collection of paths. Must not return <code>null</code>
     */
    Collection<String> getAdditionalLibraries();

    /**
     * Return additional entries to add to the list of environment variables.
     * 
     * Each entry should be a string with the variable name, and = (equals) and
     * the value. 
     * 
     * @return a collection of variables. Must not return <code>null</code>
     */
    Collection<String> getAdditionalEnvVariables();

    /**
     * Return additional entries to add to the list of builtins.
     * 
     * @return a collection of builtins. Must not return <code>null</code>
     */
    Collection<String> getAdditionalBuiltins();

    /**
     * Return additional entries to add to the list of string substitutions variables.
     * 
     * @return a collection of string substitutions variables. Must not return <code>null</code>
     */
    Map<String, String> getAdditionalStringSubstitutionVariables();

}
