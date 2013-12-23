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

package org.python.pydev.refactoring.codegenerator.generateproperties.request;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.plugin.preferences.PyCodeStylePreferencesPage;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;
import org.python.pydev.shared_core.string.StringUtils;

public class GeneratePropertiesRequest implements IRefactoringRequest {

    public final INodeAdapter attributeAdapter;
    public final int offsetMethodStrategy;
    public final int offsetPropertyStrategy;
    public final int accessModifier;

    private IClassDefAdapter classAdapter;
    private SelectionState state;
    private NodeHelper nodeHelper;
    private final AdapterPrefs adapterPrefs;

    public GeneratePropertiesRequest(IClassDefAdapter classAdapter, INodeAdapter attributeAdapter,
            List<PropertyTextAdapter> properties, int offsetMethodStrategy, int offsetPropertyStrategy,
            int accessModifier, AdapterPrefs adapterPrefs) {
        this.state = new SelectionState();
        this.classAdapter = classAdapter;
        this.attributeAdapter = attributeAdapter;
        this.offsetMethodStrategy = offsetMethodStrategy;
        this.offsetPropertyStrategy = offsetPropertyStrategy;
        this.accessModifier = accessModifier;
        this.adapterPrefs = adapterPrefs;
        this.nodeHelper = new NodeHelper(adapterPrefs);
        initialize(properties);
    }

    private void initialize(List<PropertyTextAdapter> properties) {
        for (PropertyTextAdapter propertyAdapter : properties) {
            switch (propertyAdapter.getType()) {
                case (PropertyTextAdapter.GETTER):
                    state.addSelection(SelectionState.GETTER);
                    break;
                case (PropertyTextAdapter.SETTER):
                    state.addSelection(SelectionState.SETTER);
                    break;
                case (PropertyTextAdapter.DELETE):
                    state.addSelection(SelectionState.DELETE);
                    break;
                case (PropertyTextAdapter.DOCSTRING):
                    state.addSelection(SelectionState.DOCSTRING);
                    break;
                default:
                    break;
            }
        }
    }

    public AdapterPrefs getAdapterPrefs() {
        return adapterPrefs;
    }

    public SelectionState getSelectionState() {
        return state;
    }

    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        return classAdapter;
    }

    /**
     * @return the attribute name, for example "an_attribute"
     */
    public String getAttributeName() {
        return nodeHelper.getPublicAttr(attributeAdapter.getName());
    }

    /**
     * @return the property name, for example "_an_attribute"
     */
    public String getPropertyName() {
        return nodeHelper.getAccessName(getAttributeName(), accessModifier);
    }

    /**
     * Joins the two strings to get the accessor function name, adhering to the
     * user's preferred style (camelCase or snake_case).
     * 
     * @param accessType "get", "set" or "del"
     * @param attributeName for example "an_attribute"
     * @return for example "set_an_attribute" or "setAnAttribute"
     */
    public static String getAccessorName(String accessType, String attributeName) {
        accessType += "_" + attributeName;
        int useMethodsCamelCase = PyCodeStylePreferencesPage.useMethodsCamelCase();
        if (useMethodsCamelCase == PyCodeStylePreferencesPage.METHODS_FORMAT_CAMELCASE_FIRST_LOWER) {
            return StringUtils.asStyleCamelCaseFirstLower(accessType);
        }
        if (useMethodsCamelCase == PyCodeStylePreferencesPage.METHODS_FORMAT_CAMELCASE_FIRST_UPPER) {
            return StringUtils.asStyleCamelCaseFirstUpper(accessType);
        }
        //default is underscore
        return StringUtils.asStyleLowercaseUnderscores(accessType);

    }

}
