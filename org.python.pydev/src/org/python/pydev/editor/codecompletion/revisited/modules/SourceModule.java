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
import java.util.StringTokenizer;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;
import org.python.pydev.editor.codecompletion.revisited.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindDefinitionModelVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.GlobalModelVisitor;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

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

    
    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    public IToken[] getGlobalTokens(CompletionState state, ICodeCompletionASTManager manager) {
        IToken[] t = getTokens(GlobalModelVisitor.GLOBAL_TOKENS);
        
        if(t instanceof SourceToken[]){
	        SourceToken[] tokens = (SourceToken[]) t;
	        for (int i = 0; i < tokens.length; i++) {
	            if(tokens[i].getRepresentation().equals(state.activationToken)){
	                
	                SimpleNode a = tokens[i].getAst();
	                    
                    //COMPLETION: get the completions for the whole hierarchy if this is a class!!
                    List modToks = new ArrayList(Arrays.asList(GlobalModelVisitor.getTokens(a, GlobalModelVisitor.INNER_DEFS, name)));
                    
                    try {
                        if (a instanceof ClassDef) {
                            ClassDef c = (ClassDef) a;
                            for (int j = 0; j < c.bases.length; j++) {
                                if (c.bases[j] instanceof Name) {
                                    Name n = (Name) c.bases[j];
                                    String base = n.id;
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
                                } else if (c.bases[j] instanceof Attribute) {
                                    Attribute attr = (Attribute) c.bases[j];
                                    String s = NodeUtils.getFullRepresentationString(attr);

                                    state = state.getCopy();
                                    state.activationToken = s;
                                    final IToken[] comps = manager.getCompletionsForModule(this, state);
                                    modToks.addAll(Arrays.asList(comps));
                                }
                            }

                        }
                    } catch (CompletionRecursionException e) {
                        // let's return what we have so far...
                    }
                    
                    return (IToken[]) modToks.toArray(new IToken[0]);
	            }
	        }
        }else{
            System.err.println("Expecting SourceToken, got: "+t.getClass().getName());
        }
        return new IToken[0];
    }

    public Definition[] findDefinition(String token, int line, int col, PythonNature nature) throws Exception{
        //the line passed in starts at 1 and the lines for the visitor start at 0
        ArrayList<Definition> toRet = new ArrayList<Definition>();
        
        FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(token, line, col+1, this);
        if (ast != null){
            ast.accept(visitor);
        }
        
        if(visitor.definitions.size() == 0){
            //ok, it is not an assign, so, let's search the global tokens (and imports)
            Tuple<AbstractModule, String> o = nature.getAstManager().findOnImportedMods(nature, token, this);
            String tok = token;
            SourceModule mod = this;
            
            if(o != null && o.o1 instanceof SourceModule){
	            mod =  (SourceModule) o.o1;
	            tok = o.o2;
            }
            
            if(tok != null){
            	if(tok.length() > 0){
		            //mod == this if we are now checking the globals (or maybe not)...heheheh
		            Definition d = mod.findGlobalTokDef(tok, nature);
		            if(d != null){
		                toRet.add(d);
		                
		            }else if(visitor.moduleImported != null){
		            	//if it was found as some import (and is already stored as a dotted name), we must check for
		            	//multiple representations in the absolute form:
		            	//as a relative import
		            	//as absolute import
		            	String rel = AbstractToken.makeRelative(mod.getName(), visitor.moduleImported);
		            	AbstractModule modFound = nature.getAstManager().getModule(rel, nature, false);
		            	if(modFound == null){
		            		modFound = nature.getAstManager().getModule(visitor.moduleImported, nature, false);
		            	}
		            	if(modFound != null){
		            		//ok, found it
		            		toRet.add(new Definition(1,1,"", null, null, modFound));
		            	}
		            }else{
		            	//ok, we can still have it in wild imports
		            	
		            }

            	}else{
            		//we found it, but it is an empty tok (which means that what we found is the actual module).
            		toRet.add(new Definition(1,1,"",null,null,mod)); 
            	}
            }
            
        }else{
            FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col);
            if (ast != null){
                ast.accept(scopeVisitor);
            }

            for (Iterator iter = visitor.definitions.iterator(); iter.hasNext();) {
	            AssignDefinition element = (AssignDefinition) iter.next();
	            if(element.target.startsWith("self") == false){
		            if(element.scope.isOuterOrSameScope(scopeVisitor.scope)){
		                toRet.add(element);
		            }
	            }else{
		            toRet.add(element);
	            }
	        }
        }
        return (Definition[]) toRet.toArray(new Definition[0]);
    }


    /**
     * @param tok
     * @param nature 
     * @return
     */
    public Definition findGlobalTokDef(String tok, PythonNature nature) {
    	String[] headAndTail = FullRepIterable.headAndTail(tok);
    	String firstPart = headAndTail[0];
    	String rep = headAndTail[1];
    	
    	IToken[] tokens = null;
    	if(nature != null){
    		tokens = nature.getAstManager().getCompletionsForModule(this, CompletionState.getEmptyCompletionState(firstPart, nature));
    	}else{
    		tokens = getGlobalTokens();
    	}

        for (int i = 0; i < tokens.length; i++) {
            if(tokens[i].getRepresentation().equals(rep)){
                SimpleNode a = ((SourceToken)tokens[i]).getAst();
                
                FindScopeVisitor scopeVisitor = new FindScopeVisitor(a.beginLine, a.beginColumn);
                if (ast != null){
                    try {
                        ast.accept(scopeVisitor);
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                }
                Tuple<Integer, Integer> def = getLineColForDefinition(a);
                
                //line, col
                return new Definition(def.o1, def.o2, tok, a, scopeVisitor.scope, this);
            }
        }
        return null;
    }
    
    public Tuple<Integer, Integer> getLineColForDefinition(SimpleNode a){
    	int line = a.beginLine;
    	int col = a.beginColumn;
    	
    	if(a instanceof ClassDef){
    		ClassDef c = (ClassDef)a;
    		line = c.name.beginLine;
    		col = c.name.beginColumn;
    		
    	} else if(a instanceof FunctionDef){
    		FunctionDef c = (FunctionDef)a;
    		line = c.name.beginLine;
    		col = c.name.beginColumn;
    	}
    	
    	return new Tuple<Integer, Integer>(line,col);
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

    @Override
    public List<IToken> getLocalImportedModules(int line, int col) {
        try {
            FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col);
            if (ast != null){
                ast.accept(scopeVisitor);
            }
            
            return scopeVisitor.scope.getLocalImportedModules(line, col, name);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<IToken>();
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

    
    /**
     * 
     */
    public int findAstEnd(SimpleNode node) {
        try {
            int line = node.beginLine;
            int col = node.beginColumn;
	        FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col);
	        if (ast != null){
                ast.accept(scopeVisitor);
	        }
	        
	        return scopeVisitor.scope.scopeEndLine;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * @return
     */
    public int findIfMain() {
        try {
	        FindScopeVisitor scopeVisitor = new FindScopeVisitor(-1,-1);
	        if (ast != null){
                ast.accept(scopeVisitor);
	        }
	        
	        return scopeVisitor.scope.ifMainLine;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SourceModule)) {
            return false;
        }
        SourceModule m = (SourceModule) obj;
        if(file == null || m.file == null){
            if(file != null){
                return false;
            }
            if(m.file != null){
                return false;
            }
            return this.name.equals(m.name);
        }
        
        return REF.getFileAbsolutePath(file).equals(REF.getFileAbsolutePath(m.file)) && this.name.equals(m.name); 
    }
}