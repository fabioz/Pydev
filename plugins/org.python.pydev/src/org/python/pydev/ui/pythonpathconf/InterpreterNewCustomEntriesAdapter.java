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
import java.util.Collections;
import java.util.Map;

/**
 * Adapter for {@link IInterpreterNewCustomEntries} that provides no additional
 * entries for any item.
 * 
 * This class can be subclassed in preference to implementing
 * IInterpreterNewCustomEntries directly if not all additions need to be made.
 */
public class InterpreterNewCustomEntriesAdapter implements IInterpreterNewCustomEntries {

    @Override
    public Collection<String> getAdditionalLibraries() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getAdditionalEnvVariables() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getAdditionalBuiltins() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getAdditionalStringSubstitutionVariables() {
        return Collections.emptyMap();
    }

}
