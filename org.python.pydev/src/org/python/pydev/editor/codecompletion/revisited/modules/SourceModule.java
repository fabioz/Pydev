/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindDefinitionModelVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.GlobalModelVisitor;

/**
 * The module should have all the information we need for code completion, find definition, and refactoring on a module.
 * 
 * Note: A module may be represented by a folder if it has an __init__.py file that represents the module or a python file.
 * 
 * Any of those must be a valid python token to be recognized (from the PYTHONPATH).
 * 
 * We don't reuse the ModelUtils already created as we still have to transport a lot of logic to it to make it workable, so, the attempt
 * here is to use a thin tier.
 * 
 * NOTE: When using it, don't forget to use the superclass abstraction.
 *  
 * @author Fabio Zadrozny
 */
public class SourceModule extends AbstractModule {

    /**
     * This is the abstract syntax tree based on the jython parser output.
     */
    private SimpleNode ast;

    /**
     * File that originated the syntax tree.
     */
    private File file;

    /**
     * This is the time when the file was last modified.
     */
    private long lastModified;

    /**
     * @return a reference to all the modules that are imported from this one in the global context as a from xxx import *
     * 
     * This modules are treated specially, as we don't care which tokens were imported. When this is requested, the module is prompted for
     * its tokens.
     */
    public IToken[] getWildImportedModules() {
        return getTokens(GlobalModelVisitor.WILD_MODULES);
    }

    /**
     * @return a reference to all the modules that are imported from this one in the global context in the following constructions:
     * 
     * from xxx import xxx import xxx import xxx as ... from xxx import xxx as ....
     */
    public IToken[] getTokenImportedModules() {
        return getTokens(GlobalModelVisitor.ALIAS_MODULES);
    }

    /**
     * 
     * @return the file this module corresponds to.
     */
    public File getFile(){
        return this.file;
    }
    
    /**
     * @return the tokens that are present in the global scope.
     * 
     * The tokens can be class definitions, method definitions and attributes.
     */
    public IToken[] getGlobalTokens() {
        return getTokens(GlobalModelVisitor.GLOBAL_TOKENS);
    }

    /**
     * @return a string representing the module docstring.
     */
    public String getDocString() {
        IToken[] l = getTokens(GlobalModelVisitor.MODULE_DOCSTRING);
        if (l.length > 0) {
            SimpleNode a = ((SourceToken) l[0]).getAst();

            return ((Str) a).s;
        }
        return "";
    }

    /**
     * @param which
     * @return a list of IToken
     */
    private IToken[] getTokens(int which) {
        try {
            return GlobalModelVisitor.getTokens(ast, which, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new IToken[0];
    }

    /**
     * 
     * @param name
     * @param f
     * @param n
     */
    public SourceModule(String name, File f, SimpleNode n) {
        super(name);
        this.ast = n;
        this.file = f;
        if(f != null)
            this.lastModified = f.lastModified();
    }

//    public IToken[] getGlobalTokens(String token, ASTManager manager, int line, int col, PythonNature nature) {
    
    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    public IToken[] getGlobalTokens(CompletionState state, ASTManager manager) {
        IToken[] t = getTokens(GlobalModelVisitor.GLOBAL_TOKENS);
        
        if(t instanceof SourceToken[]){
	        SourceToken[] tokens = (SourceToken[]) t;
	        for (int i = 0; i < tokens.length; i++) {
	            if(tokens[i].getRepresentation().equals(state.activationToken)){
	                
	                SimpleNode a = tokens[i].getAst();
	                    
                    //COMPLETION: get the completions for the whole hierarchy if this is a class!!
                    List modToks = new ArrayList(Arrays.asList(GlobalModelVisitor.getTokens(a, GlobalModelVisitor.INNER_DEFS, name)));
                    
                    if( a instanceof ClassDef){
                        ClassDef c = (ClassDef) a;
                        for (int j = 0; j < c.bases.length; j++) {
                            if(c.bases[j] instanceof Name){
                                Name n = (Name) c.bases[j];
                                String base = n.id;
                                //TODO: this may enter in a loop, as it is recursive.
                                //An error in the programming might result in an error.
                                //
                                //e.g. The case below results in a loop.
                                //
                                //class A(B):
                                //    
                                //    def a(self):
                                //        pass
                                //        
                                //class B(A):
                                //    
                                //    def b(self):
                                //        pass
                                state = state.getCopy();
                                state.activationToken = base;
                                
                                state.checkMemory(this, base);
                                
                                
                                final IToken[] comps = manager.getCompletionsForModule(this, state);
                                modToks.addAll(Arrays.asList(comps));
                            }else if (c.bases[j] instanceof Attribute){
                                Attribute attr = (Attribute) c.bases[j];
                                String s = AbstractVisitor.getFullRepresentationString(attr);
                                
                                state = state.getCopy();
                                state.activationToken = s;
                                final IToken[] comps = manager.getCompletionsForModule(this, state);
                                modToks.addAll(Arrays.asList(comps));
                            }
                        }
                        
                    }
                    
                    return (IToken[]) modToks.toArray(new IToken[0]);
	            }
	        }
        }else{
            System.err.println("Expecting SourceToken, got: "+t.getClass().getName());
        }
        return new IToken[0];
    }

    public AssignDefinition[] findDefinition(String token, int line, int col, IASTManager manager) throws Exception{
        //the line passed in starts at 1 and the lines for the visitor start at 0
        FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col);
        if (ast != null){
            ast.accept(scopeVisitor);
        }
        
        FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(token, line, col, this);
        if (ast != null){
            ast.accept(visitor);
        }
        
        ArrayList toRet = new ArrayList();
        for (Iterator iter = visitor.definitions.iterator(); iter.hasNext();) {
            AssignDefinition element = (AssignDefinition) iter.next();
            if(element.scope.isOuterOrSameScope(scopeVisitor.scope)){
                toRet.add(element);
            }
        }
        return (AssignDefinition[]) toRet.toArray(new AssignDefinition[0]);
    }


    public IToken[] getLocalTokens(int line, int col){
        try {
	        FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col);
	        if (ast != null){
                ast.accept(scopeVisitor);
	        }
	        
	        return scopeVisitor.scope.getLocalTokens(line, col);
        } catch (Exception e) {
            e.printStackTrace();
            return new IToken[0];
        }
    }

    /**
     * @return if the file we have is the same file in the cache.
     */
    public boolean isSynched() {
        return this.file.lastModified() == this.lastModified;
    }
    
    public SimpleNode getAst(){
        return ast;
    }
}