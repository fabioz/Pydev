/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.codefolding;

import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codefolding.PyProjectionAnnotation;


/**
 * @author Fabio Zadrozny
 */
public class PyCollapse extends PyAction {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        PySelection ps = new PySelection(getTextEditor());

        ProjectionAnnotationModel model = (ProjectionAnnotationModel) getTextEditor().getAdapter(
                ProjectionAnnotationModel.class);
        try {
            if (model != null) {
                //put annotations in array list.
                Iterator iter = model.getAnnotationIterator();
                while (iter != null && iter.hasNext()) {
                    PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();
                    Position position = model.getPosition(element);

                    int line = ps.getDoc().getLineOfOffset(position.offset);

                    int start = ps.getStartLineIndex();
                    int end = ps.getEndLineIndex();

                    for (int i = start; i <= end; i++) {
                        if (i == line) {
                            model.collapse(element);
                            break;
                        }
                    }
                }

            }
        } catch (BadLocationException e) {
            Log.log(IStatus.ERROR, "Unexpected error collapsing", e);
        }
    }
}