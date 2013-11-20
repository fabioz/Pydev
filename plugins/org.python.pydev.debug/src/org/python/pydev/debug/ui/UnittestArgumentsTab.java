/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.python.pydev.debug.ui.blocks.OverrideUnittestArgumentsBlock;

/**
 * Adds the possibility of overriding unittest arguments for a given launch configuration.
 */
public class UnittestArgumentsTab extends ArgumentsTab {

    public UnittestArgumentsTab(MainModuleTab mainModuleTab) {
        super(mainModuleTab);
    }

    /**
     * Overridden because we don't create the default arguments, but provide a way to override the settings in the
     * preferences for a given test run.
     */
    @Override
    protected AbstractLaunchConfigurationTab createProgramArgumentsBlock(MainModuleTab mainModuleTab) {
        return new OverrideUnittestArgumentsBlock();
    }
}
