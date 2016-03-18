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

package org.python.pydev.refactoring.codegenerator.generateproperties;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.model.generateproperties.TreeAttributeNode;
import org.python.pydev.refactoring.core.model.generateproperties.TreeClassNode;
import org.python.pydev.refactoring.core.model.tree.TreeNodeSimple;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class GeneratePropertiesRequestProcessor implements IRequestProcessor<GeneratePropertiesRequest> {

    private Object[] checked;

    private int offsetMethodStrategy;

    private int offsetPropertyStrategy;

    private int accessModifier;

    private AdapterPrefs adapterPrefs;

    public GeneratePropertiesRequestProcessor(AdapterPrefs adapterPrefs) {
        this.adapterPrefs = adapterPrefs;
        checked = new Object[0];
        offsetMethodStrategy = IOffsetStrategy.AFTERINIT;
        offsetPropertyStrategy = IOffsetStrategy.END;
        accessModifier = NodeHelper.ACCESS_PUBLIC;
    }

    public void setCheckedElements(Object[] checked) {
        this.checked = checked;
    }

    private List<TreeAttributeNode> getAttributes() {
        List<TreeAttributeNode> attrs = new ArrayList<TreeAttributeNode>();

        for (Object elem : checked) {
            if (elem instanceof TreeAttributeNode) {
                attrs.add((TreeAttributeNode) elem);
            }
        }

        return attrs;
    }

    private List<PropertyTextAdapter> getProperties(TreeAttributeNode attr) {
        List<PropertyTextAdapter> props = new ArrayList<PropertyTextAdapter>();

        for (Object elem : checked) {
            if (elem instanceof TreeNodeSimple) {
                TreeNodeSimple<? extends INodeAdapter> propertyNode = (TreeNodeSimple<?>) elem;
                if (propertyNode.getParent() == attr) {
                    props.add((PropertyTextAdapter) propertyNode.getAdapter());
                }
            }
        }

        return props;
    }

    @Override
    public List<GeneratePropertiesRequest> getRefactoringRequests() {
        List<GeneratePropertiesRequest> requests = generateRequests();

        return requests;
    }

    private List<GeneratePropertiesRequest> generateRequests() {
        List<GeneratePropertiesRequest> requests = new ArrayList<GeneratePropertiesRequest>();

        for (TreeAttributeNode elem : getAttributes()) {
            GeneratePropertiesRequest request = extractRequest(elem);
            if (request != null) {
                requests.add(request);
            }
        }

        return requests;
    }

    private GeneratePropertiesRequest extractRequest(TreeAttributeNode attr) {
        if (attr.getParent() != null && attr.getParent() instanceof TreeClassNode) {
            TreeClassNode classNode = (TreeClassNode) attr.getParent();

            return new GeneratePropertiesRequest(classNode.getAdapter(), attr.getAdapter(), getProperties(attr),
                    offsetMethodStrategy, offsetPropertyStrategy, accessModifier, adapterPrefs);
        }
        return null;
    }

    public void setMethodDestination(int strat) {
        this.offsetMethodStrategy = strat;
    }

    public void setPropertyDestination(int strat) {
        this.offsetPropertyStrategy = strat;
    }

    public void setAccessModifier(int accessModifier) {
        this.accessModifier = accessModifier;
    }
}
