/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.pythonpathconf;

/**
 * @author fabioz
 *
 * This interface is provided to provide a builder for the interpreter info.
 * 
 * Such a builder should take care of keeping the interpreter info up to date -- i.e.: the SystemModulesManager
 * and the AdditionalSystemInterpreterInfo)
 */
public interface IInterpreterInfoBuilder {

    /**
     * @param interpreterInfo the information which the builder should use to get the pythonpath (and thus, the 
     * paths that should be listened).
     * 
     * The info can be set only once, after that, an error may be raised if someone tries to set it again.
     * 
     * On dispose, the info reference should be disposed (but this builder should not be reused again).
     */
    void setInfo(InterpreterInfo interpreterInfo);

    /**
     * Stops hearing any changes.
     */
    void dispose();

}
