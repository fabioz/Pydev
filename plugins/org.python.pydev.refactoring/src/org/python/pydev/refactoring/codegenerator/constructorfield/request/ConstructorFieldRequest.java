/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.constructorfield.request;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class ConstructorFieldRequest implements IRefactoringRequest {

    public final List<INodeAdapter> attributeAdapters;
    public final int offsetStrategy;
    public final IClassDefAdapter classAdapter;
    private final AdapterPrefs adapterPrefs;

    public ConstructorFieldRequest(IClassDefAdapter classAdapter, List<INodeAdapter> attributeAdapters,
            int offsetStrategy, AdapterPrefs adapterPrefs) {
        this.classAdapter = classAdapter;
        this.attributeAdapters = attributeAdapters;
        this.offsetStrategy = offsetStrategy;
        this.adapterPrefs = adapterPrefs;
    }

    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        return classAdapter;
    }

    public AdapterPrefs getAdapterPrefs() {
        return adapterPrefs;
    }

}
