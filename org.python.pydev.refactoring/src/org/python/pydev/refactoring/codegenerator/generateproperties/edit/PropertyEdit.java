/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties.edit;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.str_typeType;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.SelectionState;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class PropertyEdit extends AbstractInsertEdit {

	private static final String SET = "set";

	private static final String DEL = "del";

	private static final String NONE = "None";

	private static final String GET = "get";

	private static final String PROPERTY = "property";

	private String attributeName;

	private SelectionState state;

	private int offsetStrategy;

	private int accessModifier;

	public PropertyEdit(GeneratePropertiesRequest req) {
		super(req);
		this.attributeName = nodeHelper.getPublicAttr(req.getAttributeName());
		state = req.getSelectionState();
		this.offsetStrategy = req.getPropertyOffsetStrategy();
		this.accessModifier = req.getAccessModifier();
	}

	@Override
	protected SimpleNode getEditNode() {
		exprType[] targets = initProperty();
		List<exprType> args = getPropertyArgs();
		Call property = new Call(new Name(PROPERTY, Name.Load), args.toArray(new exprType[0]), null, null, null);

		return new Assign(targets, property);
	}

	private exprType[] initProperty() {
		exprType[] targets = new exprType[1];
		String propertyName = nodeHelper.getAccessName(attributeName, accessModifier);
		targets[0] = new Name(propertyName, Name.Store);
		return targets;
	}

	private List<exprType> getPropertyArgs() {
		List<exprType> args = new ArrayList<exprType>();

		String propertyName = getCapitalString(attributeName);

		if (state.isGetter()) {
			args.add(new Name(GET + propertyName, Name.Load));
		} else {
			args.add(noneName());
		}
		if (state.isSetter()) {
			args.add(new Name(SET + propertyName, Name.Load));
		} else {
			args.add(noneName());
		}
		if (state.isDelete()) {
			args.add(new Name(DEL + propertyName, Name.Load));
		} else {
			args.add(noneName());
		}
		if (state.isDocstring()) {
			args.add(new Str(propertyName + "'s Docstring", str_typeType.SingleDouble, false, false));
		} else {
			args.add(noneName());
		}
		return args;
	}

	private Name noneName() {
		return new Name(NONE, Name.Load);
	}

	@Override
	public int getOffsetStrategy() {
		return offsetStrategy;
	}

}
