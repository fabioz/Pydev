package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

public class ObjectAdapter extends ClassDefAdapter {

	public ObjectAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, String endLineDelim) {
		super(module, parent, null, endLineDelim);
	}

	@Override
	public ClassDef getASTNode() {

		NameTokType name = new NameTok("object", NameTok.ClassName);

		List<stmtType> body = initMethods();

		ClassDef object = new ClassDef(name, null, body.toArray(new stmtType[0]));

		return object;
	}

	private List<stmtType> initMethods() {
		List<stmtType> body = new ArrayList<stmtType>();
		body.add(create__class__());
		body.add(create__delattr__());
		body.add(create__getattribute__());
		body.add(create__hash__());
		body.add(create__init__());
		body.add(create__reduce__());
		body.add(create__reduce_ex__());
		body.add(create__repr__());
		body.add(create__setattr__());
		body.add(create__str__());
		return body;
	}

	private FunctionDef create__class__() {
		NameTokType name = new NameTok("__class__", NameTok.FunctionName);

		exprType[] args = new exprType[1];
		args[0] = new Name("cls", Name.Param);

		NameTokType vararg = new NameTok("args", NameTok.VarArg);

		argumentsType arguments = new argumentsType(args, vararg, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__delattr__() {
		NameTokType name = new NameTok("__delattr__", NameTok.FunctionName);

		exprType[] args = new exprType[2];
		args[0] = new Name("self", Name.Param);
		args[1] = new Name("name", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__getattribute__() {
		NameTokType name = new NameTok("__getattribute__", NameTok.FunctionName);

		exprType[] args = new exprType[2];
		args[0] = new Name("self", Name.Param);
		args[1] = new Name("name", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__hash__() {
		NameTokType name = new NameTok("__hash__", NameTok.FunctionName);

		exprType[] args = new exprType[1];
		args[0] = new Name("self", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__init__() {
		NameTokType name = new NameTok("__init__", NameTok.FunctionName);

		exprType[] args = new exprType[1];
		args[0] = new Name("self", Name.Param);

		NameTokType vararg = new NameTok("args", NameTok.VarArg);

		argumentsType arguments = new argumentsType(args, vararg, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__reduce__() {
		NameTokType name = new NameTok("__reduce__", NameTok.FunctionName);

		exprType[] args = new exprType[1];
		args[0] = new Name("self", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__reduce_ex__() {
		NameTokType name = new NameTok("__reduce_ex__", NameTok.FunctionName);

		exprType[] args = new exprType[1];
		args[0] = new Name("self", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__repr__() {
		NameTokType name = new NameTok("__repr__", NameTok.FunctionName);

		exprType[] args = new exprType[1];
		args[0] = new Name("self", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__setattr__() {
		NameTokType name = new NameTok("__setattr__", NameTok.FunctionName);

		exprType[] args = new exprType[3];
		args[0] = new Name("self", Name.Param);
		args[1] = new Name("name", Name.Param);
		args[2] = new Name("value", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}

	private FunctionDef create__str__() {
		NameTokType name = new NameTok("__str__", NameTok.FunctionName);

		exprType[] args = new exprType[1];
		args[0] = new Name("self", Name.Param);

		argumentsType arguments = new argumentsType(args, null, null, null);

		return new FunctionDef(name, arguments, null, null);
	}
}
