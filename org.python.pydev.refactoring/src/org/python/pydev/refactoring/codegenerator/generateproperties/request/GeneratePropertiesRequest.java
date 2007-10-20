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
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class GeneratePropertiesRequest implements IRefactoringRequest {

	private IClassDefAdapter classAdapter;

	private INodeAdapter attributeAdapter;

	private SelectionState state;

	private int offsetMethodStrategy;

	private int offsetPropertyStrategy;

	private int accessModifier;

    private String endLineDelim;

	public GeneratePropertiesRequest(IClassDefAdapter classAdapter, INodeAdapter attributeAdapter, List<PropertyTextAdapter> properties,
			int offsetMethodStrategy, int offsetPropertyStrategy, int accessModifier, String endLineDelim) {
		this.state = new SelectionState();
		this.classAdapter = classAdapter;
		this.attributeAdapter = attributeAdapter;
		this.offsetMethodStrategy = offsetMethodStrategy;
		this.offsetPropertyStrategy = offsetPropertyStrategy;
		this.accessModifier = accessModifier;
        this.endLineDelim = endLineDelim;
		initialize(properties);
	}

	public IClassDefAdapter getClassAdapter() {
		return classAdapter;
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

	public INodeAdapter getAttributeAdapter() {
		return attributeAdapter;
	}

	public String getAttributeName() {
		return getAttributeAdapter().getName();
	}

	public SelectionState getSelectionState() {
		return state;
	}

	public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
		return classAdapter;
	}

	public int getMethodOffsetStrategy() {
		return offsetMethodStrategy;
	}

	public int getPropertyOffsetStrategy() {
		return offsetPropertyStrategy;
	}

	public int getAccessModifier() {
		return accessModifier;
	}

    public String getNewLineDelim() {
        return this.endLineDelim;
    }
}
