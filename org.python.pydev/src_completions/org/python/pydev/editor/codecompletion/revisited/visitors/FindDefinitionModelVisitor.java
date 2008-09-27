/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.Subscript;
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
     * This is a stack that will keep the globals for each stack
     */
    private FastStack<Set<String>> globalDeclarationsStack = new FastStack<Set<String>>();
    
    /**
     * This is the module we are visiting: just a weak reference so that we don't create a cycle (let's
     * leave things easy for the garbage collector).
     */
    private WeakReference<IModule> module;
    
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
     * Call is stored for the context for a keyword parameter
     */
    private Stack<Call> call = new Stack<Call>();
    
    /**
     * Constructor
     * @param line: starts at 1
     * @param col: starts at 1
     */
    public FindDefinitionModelVisitor(String token, int line, int col, IModule module){
        this.tokenToFind = token;
        this.module = new WeakReference<IModule>(module);
        this.line = line;
        this.col = col;
        this.moduleName = module.getName();
        //we may have a global declared in the global scope
        globalDeclarationsStack.push(new HashSet<String>());
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
                    moduleImported = modRep + "." + NodeUtils.getRepresentationString(alias.name);
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
        globalDeclarationsStack.push(new HashSet<String>());
        defsStack.push(node);
        
        node.traverse(this);
        
        defsStack.pop();
        globalDeclarationsStack.pop();
        
        checkDeclaration(node, (NameTok) node.name);
        return null;
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        globalDeclarationsStack.push(new HashSet<String>());
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
        globalDeclarationsStack.pop();
        
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
            
            
            definitionFound = new Definition(line, name.beginColumn, rep, name, scope, module.get());
            definitions.add(definitionFound);
        }
    }
    
    @Override
    public Object visitCall(Call node) throws Exception {
        this.call.push(node);
        Object r = super.visitCall(node);
        this.call.pop();
        return r;
    }
    
    
    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        if(node.ctx == NameTok.KeywordName){
            if(this.line == node.beginLine){
                String rep = NodeUtils.getRepresentationString(node);

                if(PySelection.isInside(col, node.beginColumn, rep.length())){
                    foundAsDefinition = true;
                    // if it is found as a definition it is an 'exact' match, so, erase all the others.
                    ILocalScope scope = new LocalScope(this.defsStack);
                    for (Iterator<Definition> it = definitions.iterator(); it.hasNext();) {
                        Definition d = it.next();
                        if(!d.scope.equals(scope)){
                            it.remove();
                        }
                    }
                    
                    definitions.clear();
                    
                    definitionFound = new KeywordParameterDefinition(line, node.beginColumn, rep, node, scope, module.get(), this.call.peek());
                    definitions.add(definitionFound);
                    throw new StopVisitingException();
                }
            }
        }
        return null;
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
            
            
            definitionFound = new Definition(line, name.beginColumn, rep, node, scope, module.get());
            definitions.add(definitionFound);
        }
    }
    
    @Override
    public Object visitGlobal(Global node) throws Exception {
        for(NameTokType n:node.names){
            globalDeclarationsStack.peek().add(NodeUtils.getFullRepresentationString(n));
        }
        return null;
    }
    
    @Override
    public Object visitModule(Module node) throws Exception {
        this.defsStack.push(node);
        return super.visitModule(node);
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
            exprType target = node.targets[i];
            if(target instanceof Subscript){
                continue; //assigning to an element and not the variable itself. E.g.: mydict[1] = 10 (instead of mydict = 10)
            }
            String rep = NodeUtils.getFullRepresentationString(target);
            
            if(tokenToFind.equals(rep)){ //note, order of equals is important (because one side may be null).
                exprType nodeValue = node.value;
                String value = NodeUtils.getFullRepresentationString(nodeValue);
                if(value == null){
                    value = "";
                }
                
                //get the line and column correspondent to the target
                int line = NodeUtils.getLineDefinition(target);
                int col = NodeUtils.getColDefinition(target);

                AssignDefinition definition = new AssignDefinition(value, rep, i, node, line, col, scope, module.get());
                
                //mark it as global (if it was found as global in some of the previous contexts).
                for(Set<String> globals: globalDeclarationsStack){
                    if(globals.contains(rep)){
                        definition.foundAsGlobal = true;
                    }
                }
                
                definitions.add(definition);
            }
        }
        
        return null;
    }
}
