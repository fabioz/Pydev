/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.io.File;

import org.eclipse.jface.util.Assert;
import org.python.parser.SimpleNode;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

/**
 * @author Fabio Zadrozny
 */
public class Definition implements IDefinition {

    /**
     * Line of the definition.
     */
    public int line;
    
    /**
     * Column of the definition.
     */
    public int col;


    /**
     * Name of the token.
     * 
     * e.g.
     * tok = ClassA()
     * 
     * the value equals ClassA
     */
    public String value;

    /**
     * This is the module where the definition is.
     */
    public IModule module;

    /**
     * Assign ast.
     */
    public SimpleNode ast;

    /**
     * Node with the path of classes / funcs to get to an assign.
     */
    public Scope scope;

    /**
     * Determines whether this definition was found as a local.
     */
    private boolean foundAsLocal;
    
    public Definition(int line, int col, String value, SimpleNode ast, Scope scope, IModule module){
    	this(line, col, value, ast, scope, module, false);
    }
    /**
     * The ast and scope may be null if the definition points to the module (and not some token defined
     * within it).
     * 
     * The line and col are defined starting at 1 (and not 0)
     */
    public Definition(int line, int col, String value, SimpleNode ast, Scope scope, IModule module, boolean foundAsLocal){
    	Assert.isNotNull(value, "Invalid value.");
    	Assert.isNotNull(module, "Invalid Module.");

        this.line = line;
        this.col = col;
        this.value = value;
        this.ast = ast;
        this.scope = scope;
        this.module = module;
        this.foundAsLocal = foundAsLocal;
    }
    
    
    public Definition(org.python.pydev.core.IToken tok, Scope scope, IModule module){
    	this(tok, scope, module, false);
    }
    
    public Definition(org.python.pydev.core.IToken tok, Scope scope, IModule module, boolean foundAsLocal){
    	Assert.isNotNull(tok, "Invalid value.");
    	Assert.isNotNull(module, "Invalid Module.");
    	
    	this.line = tok.getLineDefinition();
    	this.col = tok.getColDefinition();
    	this.value = tok.getRepresentation();
    	if(tok instanceof SourceToken){
    		this.ast = ((SourceToken)tok).getAst();
    	}
    	this.scope = scope;
    	this.module = module;
    }
    
	/** 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer("Definition=");
        buffer.append(value);
        buffer.append(" line=");
        buffer.append(line);
        buffer.append(" col=");
        buffer.append(col);
        return buffer.toString();
    }

    /** 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Definition)){
            return false;
        }
        
        Definition d = (Definition) obj;

        if(!value.equals(d.value)){
            return false;
        }
        
        if(col != d.col){
            return false;
        }
        
        if(line != d.line){
            return false;
        }

        if(!scope.equals(d.scope)){
            return false;
        }
        
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return value.hashCode() + col + line;
    }
}
