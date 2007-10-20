/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;


public class ClassDefAdapterFromTokens implements IClassDefAdapter{

    private List<IToken> tokens;
	private String parentName;
    private String endLineDelim;


	public ClassDefAdapterFromTokens(String parentName, List<IToken> tokens, String endLineDelim) {
		this.parentName = parentName;
		this.tokens = tokens;
        this.endLineDelim = endLineDelim;
	}


	public List<SimpleAdapter> getAssignedVariables() {
		throw new RuntimeException("Not implemented");
	}


	public List<SimpleAdapter> getAttributes() {
		throw new RuntimeException("Not implemented");
	}


	public List<String> getBaseClassNames() {
		return new ArrayList<String>();
	}


	public List<IClassDefAdapter> getBaseClasses() {
		return new ArrayList<IClassDefAdapter>();
	}


	public FunctionDefAdapter getFirstInit() {
		throw new RuntimeException("Not implemented");
	}


	public List<FunctionDefAdapter> getFunctions() {
		throw new RuntimeException("Not implemented");
	}


	public List<FunctionDefAdapter> getFunctionsInitFiltered() {
		ArrayList<FunctionDefAdapter> ret = new ArrayList<FunctionDefAdapter>();
		for(IToken tok:this.tokens){
			if(tok.getType() == IToken.TYPE_FUNCTION || tok.getType() == IToken.TYPE_BUILTIN || tok.getType() == IToken.TYPE_UNKNOWN){
				ret.add(new FunctionDefAdapter(null, null, new FunctionDef(new NameTok(tok.getRepresentation(), NameTok.FunctionName), null, null, null), endLineDelim));
			}
		}
		return ret;
	}


	public int getNodeBodyIndent() {
		throw new RuntimeException("Not implemented");
	}


	public List<PropertyAdapter> getProperties() {
		throw new RuntimeException("Not implemented");
	}


	public boolean hasAttributes() {
		throw new RuntimeException("Not implemented");
	}


	public boolean hasBaseClass() {
		return false;
	}


	public boolean hasFunctions() {
		throw new RuntimeException("Not implemented");
	}


	public boolean hasFunctionsInitFiltered() {
		return this.tokens.size() > 0;
	}


	public boolean hasInit() {
		throw new RuntimeException("Not implemented");
	}


	public boolean isNested() {
		throw new RuntimeException("Not implemented");
	}


	public boolean isNewStyleClass() {
		throw new RuntimeException("Not implemented");
	}


	public String getName() {
		return parentName;
	}


	public String getParentName() {
		throw new RuntimeException("Not implemented");
	}


	public ClassDef getASTNode() {
		throw new RuntimeException("Not implemented");
	}


	public SimpleNode getASTParent() {
		throw new RuntimeException("Not implemented");
	}


	public ModuleAdapter getModule() {
		throw new RuntimeException("Not implemented");
	}


	public int getNodeFirstLine() {
		return 0;
	}


	public int getNodeIndent() {
		return 0;
	}


	public int getNodeLastLine() {
		return 0;
	}


	public AbstractNodeAdapter<? extends SimpleNode> getParent() {
		return null;
	}


	public SimpleNode getParentNode() {
		return null;
	}


	public boolean isModule() {
		return false;
	}

}
