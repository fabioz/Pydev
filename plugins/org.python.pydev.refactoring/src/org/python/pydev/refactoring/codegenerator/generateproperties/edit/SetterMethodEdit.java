/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties.edit;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

/**
 * Creates the setter function:
 * 
 * <pre>
 *    def set_attribute(self, value):
 *        self._attribute = value
 * </pre>
 */
public class SetterMethodEdit extends AbstractInsertEdit {

    private String attributeName;
    private String accessorName;

    private int offsetStrategy;

    public SetterMethodEdit(GeneratePropertiesRequest req) {
        super(req);
        this.attributeName = req.getAttributeName();
        this.accessorName = req.getAccessorName("set", attributeName);

        this.offsetStrategy = req.offsetMethodStrategy;
    }

    @Override
    protected SimpleNode getEditNode() {
        return astFactory.createSetterFunctionDef(accessorName, attributeName);
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
