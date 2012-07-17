/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.PyEdit;

/**
 * Deselects the scope based on a previous scope selection.
 * 
 * @author fabioz
 */
public class PyScopeDeselection extends PyAction {

    public void run(IAction action) {
        try {
            PyEdit pyEdit = getPyEdit();
            FastStack<IRegion> stack = PyScopeSelection.getCache(pyEdit);

            ITextSelection selection = (ITextSelection) pyEdit.getSelectionProvider().getSelection();
            Region region = new Region(selection.getOffset(), selection.getLength());
            Iterator<IRegion> it = stack.topDownIterator();
            while (it.hasNext()) {
                IRegion iRegion = it.next();
                stack.pop(); //After getting the latest, pop it.

                if (iRegion.equals(region)) {
                    if (stack.size() > 0) {
                        IRegion peek = stack.peek();
                        pyEdit.setSelection(peek.getOffset(), peek.getLength());
                    }
                    break;
                }
            }

        } catch (Exception e) {
            Log.log(e);
        }
    }

}
