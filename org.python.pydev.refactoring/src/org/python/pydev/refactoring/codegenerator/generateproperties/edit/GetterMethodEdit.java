package org.python.pydev.refactoring.codegenerator.generateproperties.edit;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class GetterMethodEdit extends AbstractInsertEdit {

	private static final String GET = "get";

	private String attributeName;

	private int offsetStrategy;

	public GetterMethodEdit(GeneratePropertiesRequest req) {
		super(req);
		this.attributeName = nodeHelper.getPublicAttr(req.getAttributeName());
		this.offsetStrategy = req.getMethodOffsetStrategy();
	}

	@Override
	protected SimpleNode getEditNode() {
		argumentsType args = initArguments();
		Attribute returnAttribute = initReturn();
		List<stmtType> body = initBody(returnAttribute);

		return new FunctionDef(new NameTok(GET
				+ getCapitalString(attributeName), NameTok.FunctionName), args,
				body.toArray(new stmtType[0]), null);
	}

	private List<stmtType> initBody(Attribute returnAttribute) {
		List<stmtType> body = new ArrayList<stmtType>();
		body.add(new Return(returnAttribute));
		return body;
	}

	private Attribute initReturn() {
		Attribute returnAttribute = new Attribute(new Name(
				NodeHelper.KEYWORD_SELF, Name.Load), new NameTok(nodeHelper
				.getPrivateAttr(attributeName), NameTok.Attrib), Attribute.Load);
		return returnAttribute;
	}

	private argumentsType initArguments() {
		exprType[] params = new exprType[1];
		params[0] = (new Name(NodeHelper.KEYWORD_SELF, Name.Param));
		argumentsType args = new argumentsType(params, null, null, null);
		return args;
	}

	@Override
	public int getOffsetStrategy() {
		return offsetStrategy;
	}

}
