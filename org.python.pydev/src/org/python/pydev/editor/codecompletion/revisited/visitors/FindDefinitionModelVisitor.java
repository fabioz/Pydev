/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * @author Fabio Zadrozny
 */
public class FindDefinitionModelVisitor extends AbstractVisitor{

    /**
     * This is the token to find.
     */
    private String tokenToFind;
    
    /**
     * List of definitions.
     */
    public List<Definition> definitions = new ArrayList<Definition>();
    
    /**
     * Stack of classes / methods to get to a definition.
     */
    private FastStack<SimpleNode> defsStack = new FastStack<SimpleNode>();
    
    /**
     * This is the module we are visiting
     */
    private IModule module;
    
    /**
     * It is only available if the cursor position is upon a NameTok in an import (it represents the complete
     * path for finding the module from the current module -- it can be a regular or relative import).
     */
    public String moduleImported;

	private int line;

	private int col;
    
    private boolean foundAsDefinition = false;
    private Definition definitionFound;
    
    /**
     * Constructor
     * @param line: starts at 1
     * @param col: starts at 1
     */
    public FindDefinitionModelVisitor(String token, int line, int col, IModule module){
        this.tokenToFind = token;
        this.module = module;
        this.line = line;
        this.col = col;
        this.moduleName = module.getName();
    }
    
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
    	String modRep = NodeUtils.getRepresentationString(node.module);
		if( NodeUtils.isWithin(line, col, node.module) ){
    		//it is a token in the definition of a module
    		int startingCol = node.module.beginColumn;
			int endingCol = startingCol;
    		while(endingCol < this.col){
    			endingCol++;
    		}
    		int lastChar = endingCol-startingCol;
    		moduleImported = modRep.substring(0, lastChar);
    		int i = lastChar;
    		while(i < modRep.length()){
    			if(Character.isJavaIdentifierPart(modRep.charAt(i))){
    				i++;
    			}else{
    				break;
    			}
    		}
    		moduleImported += modRep.substring(lastChar, i);
    	}else{
    		//it was not the module, so, we have to check for each name alias imported
    		for (aliasType alias: node.names){
    			//we do not check the 'as' because if it is some 'as', it will be gotten as a global in the module
    			if( NodeUtils.isWithin(line, col, alias.name) ){
    				moduleImported = modRep + "." + 
    							     NodeUtils.getRepresentationString(alias.name);
    			}
    		}
    	}
    	return super.visitImportFrom(node);
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitClassDef(org.python.pydev.parser.jython.ast.ClassDef)
     */
    public Object visitClassDef(ClassDef node) throws Exception {
        defsStack.push(node);
        node.traverse(this);
        defsStack.pop();
        checkDeclaration(node, (NameTok) node.name);
        return null;
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        defsStack.push(node);
        if(node.args != null && node.args.args != null){
	        for(exprType arg:node.args.args){
	        	if(arg instanceof Name){
	        		checkParam((Name) arg);
	        	}
	        }
        }
        node.traverse(this);
        defsStack.pop();
        checkDeclaration(node, (NameTok) node.name);
        return null;
    }
    
    /**
     * @param node the declaration node we're interested in (class or function)
     * @param name the token that represents the name of that declaration
     */
    private void checkParam(Name name) {
    	String rep = NodeUtils.getRepresentationString(name);
    	if(rep.equals(tokenToFind) && line == name.beginLine && col >= name.beginColumn && col <= name.beginColumn+rep.length()){
    		foundAsDefinition = true;
    		// if it is found as a definition it is an 'exact' match, so, erase all the others.
    		ILocalScope scope = new LocalScope(this.defsStack);
    		for (Iterator<Definition> it = definitions.iterator(); it.hasNext();) {
    			Definition d = it.next();
    			if(!d.scope.equals(scope)){
    				it.remove();
    			}
    		}
    		
    		
    		definitionFound = new Definition(line, name.beginColumn, rep, name, scope, module);
    		definitions.add(definitionFound);
    	}
    }

    /**
     * @param node the declaration node we're interested in (class or function)
     * @param name the token that represents the name of that declaration
     */
    private void checkDeclaration(SimpleNode node, NameTok name) {
        String rep = NodeUtils.getRepresentationString(node);
        if(rep.equals(tokenToFind) && line == name.beginLine && col >= name.beginColumn && col <= name.beginColumn+rep.length()){
            foundAsDefinition = true;
            // if it is found as a definition it is an 'exact' match, so, erase all the others.
            ILocalScope scope = new LocalScope(this.defsStack);
            for (Iterator<Definition> it = definitions.iterator(); it.hasNext();) {
                Definition d = it.next();
                if(!d.scope.equals(scope)){
                    it.remove();
                }
            }
            
            
            definitionFound = new Definition(line, name.beginColumn, rep, node, scope, module);
            definitions.add(definitionFound);
        }
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        ILocalScope scope = new LocalScope(this.defsStack);
        if(foundAsDefinition && !scope.equals(definitionFound.scope)){ //if it is found as a definition it is an 'exact' match, so, we do not keep checking it
            return null;
        }
        
        for (int i = 0; i < node.targets.length; i++) {
            String rep = NodeUtils.getFullRepresentationString(node.targets[i]);
	        
            if(rep != null && rep.equals(tokenToFind)){
	            String value = NodeUtils.getFullRepresentationString(node.value);
                if(value == null){
                    value = "";
                }
	            AssignDefinition definition;
	            int line = NodeUtils.getLineDefinition(node.value);
	            int col = NodeUtils.getColDefinition(node.value);
	            
	            if (node.targets != null && node.targets.length > 0){
	            	line = NodeUtils.getLineDefinition(node.targets[0]);
	            	col = NodeUtils.getColDefinition(node.targets[0]);
	            }

                definition = new AssignDefinition(value, rep, i, node, line, col, scope, module);
	            definitions.add(definition);
	        }
        }
        
        return null;
    }
}
