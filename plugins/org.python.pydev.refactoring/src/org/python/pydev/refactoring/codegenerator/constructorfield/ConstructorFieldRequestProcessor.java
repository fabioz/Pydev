/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.constructorfield;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.codegenerator.constructorfield.request.ConstructorFieldRequest;
import org.python.pydev.refactoring.core.model.constructorfield.TreeNodeClassField;
import org.python.pydev.refactoring.core.model.constructorfield.TreeNodeField;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class ConstructorFieldRequestProcessor implements IRequestProcessor<ConstructorFieldRequest> {

    private Object[] checked;

    private int offsetStrategy;

    private AdapterPrefs adapterPrefs;

    public ConstructorFieldRequestProcessor(AdapterPrefs adapterPrefs) {
        checked = new Object[0];
        offsetStrategy = IOffsetStrategy.AFTERINIT;
        this.adapterPrefs = adapterPrefs;
    }

    public void setCheckedElements(Object[] checked) {
        this.checked = checked;
    }

    @Override
    public List<ConstructorFieldRequest> getRefactoringRequests() {
        return generateRequests();
    }

    private List<ConstructorFieldRequest> generateRequests() {
        List<ConstructorFieldRequest> requests = new ArrayList<ConstructorFieldRequest>();
        List<ITreeNode> nodes = new ArrayList<ITreeNode>();
        for (Object o : checked) {
            nodes.add((ITreeNode) o);
        }

        Iterator<ITreeNode> iter = nodes.iterator();
        while (iter.hasNext()) {
            ITreeNode node = iter.next();
            if (node instanceof TreeNodeClassField) {
                List<INodeAdapter> fields = getFields(iter);
                if (!fields.isEmpty()) {
                    ClassDefAdapter clazz = (ClassDefAdapter) node.getAdapter();
                    ConstructorFieldRequest request = new ConstructorFieldRequest(clazz, fields, offsetStrategy,
                            adapterPrefs);
                    requests.add(request);
                }
            }
        }

        return requests;
    }

    private List<INodeAdapter> getFields(Iterator<ITreeNode> iter) {
        List<INodeAdapter> fields = new ArrayList<INodeAdapter>();
        ITreeNode field = iter.next();
        while (field instanceof TreeNodeField) {
            fields.add(field.getAdapter());
            if (iter.hasNext()) {
                field = iter.next();
            } else {
                break;
            }
        }
        return fields;
    }

    public void setMethodDestination(int strat) {
        this.offsetStrategy = strat;
    }

}
