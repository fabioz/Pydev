/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Copyright (c) 2013 by Syapse, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;


import org.python.pydev.editor.actions.PyOrganizeImports;


/**
 * Action used to organize imports to all the available python files.
 *  
 * @author Jeremy J. Carroll
 */
public class PyOrganizeImportsAction extends PyContainerFormatterAction {

    public PyOrganizeImportsAction() {
        super("organize imports", "organize imports in", "organized");
    }

    @Override
    PyOrganizeImports createFormatter() {
        return new PyOrganizeImports();
    }

}
