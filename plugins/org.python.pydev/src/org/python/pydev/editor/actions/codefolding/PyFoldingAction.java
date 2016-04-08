/**
 * Copyright (c) 2005-201 6by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 16/06/2005
 */
package org.python.pydev.editor.actions.codefolding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codefolding.PyProjectionAnnotation;

/**
 * @author Fabio
 */
public abstract class PyFoldingAction extends PyAction {

    /**
     * @param model
     * @return
     */
    protected Iterator<Annotation> getAnnotationsIterator(final ProjectionAnnotationModel model,
            boolean useExpanded) {
        //put annotations in array list.
        Iterator<Annotation> iter = model.getAnnotationIterator();
        if (iter != null) {

            //get the not collapsed (expanded) and sort them
            ArrayList<Annotation> expanded = new ArrayList<Annotation>();
            while (iter.hasNext()) {
                PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();
                if (element.isCollapsed() == useExpanded) {
                    expanded.add(element);
                }
            }

            Collections.sort(expanded, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    PyProjectionAnnotation e1 = (PyProjectionAnnotation) o1;
                    PyProjectionAnnotation e2 = (PyProjectionAnnotation) o2;
                    int e1Off = model.getPosition(e1).getOffset();
                    int e2Off = model.getPosition(e2).getOffset();
                    if (e1Off < e2Off) {
                        return -1;
                    }
                    if (e1Off > e2Off) {
                        return 1;
                    }
                    return 0;
                }
            });

            iter = expanded.iterator();
        }
        return iter;
    }

    /**
     * @return
     */
    protected ProjectionAnnotationModel getModel() {
        final ProjectionAnnotationModel model = getTextEditor().getAdapter(
                ProjectionAnnotationModel.class);
        return model;
    }

    /**
     * @param element
     * @param elements
     * @param model
     * @return
     */
    protected boolean isInsideLast(PyProjectionAnnotation element, List elements, ProjectionAnnotationModel model) {
        if (elements.size() == 0) {
            return false;
        }

        PyProjectionAnnotation top = (PyProjectionAnnotation) elements.get(elements.size() - 1);
        Position p1 = model.getPosition(element);
        Position pTop = model.getPosition(top);

        int p1Offset = p1.getOffset();

        int pTopoffset = pTop.getOffset();
        int pTopLen = pTopoffset + pTop.getLength();

        if (p1Offset > pTopoffset && p1Offset < pTopLen) {
            return true;
        }
        return false;
    }

    /**
     * @param position
     * @param elements
     * @return
     */
    protected boolean isInside(Position position, List elements) {
        for (Iterator iter = elements.iterator(); iter.hasNext();) {
            Position element = (Position) iter.next();
            if (position.getOffset() > element.getOffset()
                    && position.getOffset() < element.getOffset() + element.getLength()) {
                return true;
            }
        }
        return false;
    }

}
