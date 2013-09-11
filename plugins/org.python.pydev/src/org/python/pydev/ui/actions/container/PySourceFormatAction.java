/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.parser.prettyprinterv2.IFormatter;

/**
 * Action used to apply source code formatting to all the available python files.
 *  
 * @author Fabio
 */
public class PySourceFormatAction extends PyContainerFormatterAction {

    public PySourceFormatAction() {
        super("format", "format", "formatted");
    }

    @Override
    IFormatter createFormatter() {
        return new PyFormatStd().getFormatter();
    }

}
