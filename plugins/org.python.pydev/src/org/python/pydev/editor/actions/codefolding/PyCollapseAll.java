/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 22, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.codefolding;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.python.pydev.editor.codefolding.PyProjectionAnnotation;

/**
 * @author Fabio Zadrozny
 */
public class PyCollapseAll extends PyFoldingAction {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        final ProjectionAnnotationModel model = getModel();

        if (model != null) {

            Iterator iter = getAnnotationsIterator(model, false);

            if (iter != null) {
                //we just want to collapse the leafs, and we are working only with the not collapsed sorted by offset.

                List<PyProjectionAnnotation> elements = new ArrayList<PyProjectionAnnotation>(); //used to know the context
                while (iter.hasNext()) {
                    PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();

                    //special case, we have none in our context
                    if (elements.size() == 0) {
                        elements.add(element);

                    } else {
                        if (isInsideLast(element, elements, model)) {
                            elements.add(element);

                        } else {
                            //ok, the one in the top has to be collapsed ( and this one added )
                            PyProjectionAnnotation top = elements.remove(elements.size() - 1);
                            model.collapse(top);
                            elements.add(element);
                        }
                    }
                }
                if (elements.size() > 0) {
                    PyProjectionAnnotation top = elements.remove(elements.size() - 1);
                    model.collapse(top);
                }
            }
        }
    }
}
