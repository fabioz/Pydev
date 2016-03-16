/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.structure.Tuple;

public class PySelectWord extends PyAction {

    @Override
    public void run(IAction action) {
        PyEdit pyEdit = getPyEdit();
        PySelection ps = new PySelection(pyEdit);
        try {
            Tuple<String, Integer> currToken = ps.getCurrToken();
            if (currToken.o1 != null) {
                int len = currToken.o1.length();
                if (len > 0) {
                    pyEdit.selectAndReveal(currToken.o2, len);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

}
