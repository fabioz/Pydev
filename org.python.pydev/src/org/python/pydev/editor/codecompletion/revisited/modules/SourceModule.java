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

import org.python.pydev.core.FindInfo;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;
import org.python.pydev.editor.codecompletion.revisited.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ConcreteToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindDefinitionModelVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.GlobalModelVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.LocalScope;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;

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

    public static boolean TESTING = false;
    
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
     * Searches for the following import tokens:
     *   import xxx 
     *   import xxx as ... 
     *   from xxx import xxx
     *   from xxx import xxx as ....
     * Note, that imports with wildcards are not collected.
     * @return an array of references to the modules that are imported from this one in the global context.
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
     * @see org.python.pydev.core.IModule#getGlobalTokens(org.python.pydev.core.ICompletionState, org.python.pydev.core.ICodeCompletionASTManager)
     */
    public IToken[] getGlobalTokens(ICompletionState initialState, ICodeCompletionASTManager manager) {
        IToken[] t = getTokens(GlobalModelVisitor.GLOBAL_TOKENS);
        
        if(t instanceof SourceToken[]){
	        SourceToken[] tokens = (SourceToken[]) t;
	        for (int i = 0; i < tokens.length; i++) {
	            String activationToken = initialState.getActivationToken();
                SourceToken token = tokens[i];
                String rep = token.getRepresentation();
                
                SimpleNode ast = token.getAst();
                
                if(activationToken.length() > rep.length() && activationToken.startsWith(rep)){
                    //we need this thing to work correctly for nested modules...
                    //some tests are available at: PythonCompletionTestWithoutBuiltins.testDeepNestedXXX
                    
                    int iActTok = 0;
                    String[] actToks = FullRepIterable.dotSplit(activationToken);
                    if(actToks[iActTok].equals(rep)){
                        //System.out.println("Now we have to find act..."+activationToken+"(which is a definition of:"+rep+")");
                        try {
                            Definition[] definitions;
                            String value = activationToken;
                            String initialValue=null;
                            while(true){
                                if(value.equals(initialValue)){
                                    break;
                                }
                                initialValue = value;
                                if(iActTok > actToks.length){
                                    break; //unable to find it
                                }
                                definitions = findDefinition(initialState.getCopyWithActTok(value), token.getLineDefinition(), token.getColDefinition(), manager.getNature(), new ArrayList<FindInfo>());
                                if(definitions.length == 1){
                                    Definition d = definitions[0];
                                    if(d.ast instanceof Assign){
                                        Assign assign = (Assign) d.ast;
                                        value = NodeUtils.getRepresentationString(assign.value);
                                        definitions = findDefinition(initialState.getCopyWithActTok(value), d.line, d.col, manager.getNature(), new ArrayList<FindInfo>());
                                    }else if(d.ast instanceof ClassDef){
                                        IToken[] toks = (IToken[]) getToks(initialState, manager, d.ast).toArray(new IToken[0]);
                                        if(iActTok == actToks.length-1){
                                            return toks;
                                        }
                                        value = d.value;
                                        
                                    }else if (d.ast instanceof Name){
                                        ClassDef classDef = ((LocalScope)d.scope).getClassDef();
                                        if(classDef != null){
                                        	FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(actToks[actToks.length-1], d.line, d.col, d.module);
	                                        classDef.accept(visitor);
	                                        if(visitor.definitions.size() == 0){
	                                        	return new IToken[0];
	                                        }
	                                        d = visitor.definitions.get(0);
	                                        value = d.value;
	                                        if(d instanceof AssignDefinition){
	                                            return getValueCompletions(initialState, manager, value, d.module);
	                                        }
                                        }else{
                                        	if(d.module instanceof SourceModule){
                                        		SourceModule m = (SourceModule) d.module;
                                        		String joined = FullRepIterable.joinFirstParts(actToks);
                                                Definition[] definitions2 = m.findDefinition(initialState.getCopyWithActTok(joined), d.line, d.col,manager.getNature(), null);
                                        		if(definitions2.length == 0){
                                        			return new IToken[0];
                                        		}
                                        		d = definitions2[0];
                                        		value = d.value+"."+actToks[actToks.length-1];
                                        		if(d instanceof AssignDefinition){
                                        			return ((SourceModule)d.module).getValueCompletions(initialState, manager, value, d.module);
                                        		}
                                        	}
                                        }
                                        
                                    }else if ((d.ast == null && d.module != null) || d.ast instanceof ImportFrom){
                                        return getValueCompletions(initialState, manager, value, d.module);
                                        
                                    }else{
                                        break;
                                    }
                                }else{
                                    return getValueCompletions(initialState, manager, value, this);
                                }
                                iActTok++;
                            }
                        } catch (CompletionRecursionException e) {
                        } catch (Exception e) {
                            PydevPlugin.log(e);
                        }
                    }
                } else if(rep.equals(activationToken)){
                    if(ast instanceof ClassDef){
                        initialState.setLookingForInstance(false);
                    }
                    return (IToken[]) getToks(initialState, manager, ast).toArray(new IToken[0]);
	            }
	        }
        }else{
            System.err.println("Expecting SourceToken, got: "+t.getClass().getName());
        }
        return new IToken[0];
    }

    /**
     * @param initialState
     * @param manager
     * @param value
     * @return
     */
    private IToken[] getValueCompletions(ICompletionState initialState, ICodeCompletionASTManager manager, String value, IModule module) {
        initialState.checkFindMemory(this, value);
        ICompletionState copy = initialState.getCopy();
        copy.setActivationToken(value);
        IToken[] completionsForModule = manager.getCompletionsForModule(module, copy);
        return completionsForModule;
    }

    /**
     * @param initialState
     * @param manager
     * @param ast
     * @return
     */
    private List<IToken> getToks(ICompletionState initialState, ICodeCompletionASTManager manager, SimpleNode ast) {
        List<IToken> modToks = new ArrayList<IToken>(Arrays.asList(GlobalModelVisitor.getTokens(ast, GlobalModelVisitor.INNER_DEFS, name)));//name = moduleName
        
        try {
            //COMPLETION: get the completions for the whole hierarchy if this is a class!!
            ICompletionState state;
            if (ast instanceof ClassDef) {
                ClassDef c = (ClassDef) ast;
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
                        state = initialState.getCopy();
                        state.setActivationToken(base);

                        state.checkMemory(this, base);

                        final IToken[] comps = manager.getCompletionsForModule(this, state);
                        modToks.addAll(Arrays.asList(comps));
                    } else if (c.bases[j] instanceof Attribute) {
                        Attribute attr = (Attribute) c.bases[j];
                        String s = NodeUtils.getFullRepresentationString(attr);

                        state = initialState.getCopy();
                        state.setActivationToken(s);
                        final IToken[] comps = manager.getCompletionsForModule(this, state);
                        modToks.addAll(Arrays.asList(comps));
                    }
                }

            }
        } catch (CompletionRecursionException e) {
            // let's return what we have so far...
        }
        return modToks;
    }

    @SuppressWarnings("unchecked")
	public Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature, List<FindInfo> lFindInfo) throws Exception{
        String rep = state.getActivationToken();
    	if(lFindInfo == null){
    		lFindInfo = new ArrayList<FindInfo>();
    	}
        //the line passed in starts at 1 and the lines for the visitor start at 0
        ArrayList<Definition> toRet = new ArrayList<Definition>();
        FindInfo info = new FindInfo();
        lFindInfo.add(info);
        
        //first thing is finding its scope
        FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col);
        if (ast != null){
        	ast.accept(scopeVisitor);
        }
        
        //this visitor checks for assigns for the token
        FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(rep, line, col, this);
        if (ast != null){
            ast.accept(visitor);
        }
        
        if(visitor.definitions.size() > 0){
        	//ok, it is an assign, so, let's get it

            for (Iterator iter = visitor.definitions.iterator(); iter.hasNext();) {
	            Object next = iter.next();
                if(next instanceof AssignDefinition){
                    AssignDefinition element = (AssignDefinition) next;
    	            if(element.target.startsWith("self") == false){
    		            if(element.scope.isOuterOrSameScope(scopeVisitor.scope)){
    		                toRet.add(element);
    		            }
    	            }else{
    		            toRet.add(element);
    	            }
                }else{
                    toRet.add((Definition) next);
                }
	        }
            return (Definition[]) toRet.toArray(new Definition[0]);
        }
        
        
        
        //now, check for locals
        IToken[] localTokens = scopeVisitor.scope.getAllLocalTokens();
        info.localTokens = localTokens;
        for (IToken tok : localTokens) {
        	if(tok.getRepresentation().equals(rep)){
        		return new Definition[]{new Definition(tok, scopeVisitor.scope, this, true)};
        	}
        }
        
        //not found... check as local imports
        List<IToken> localImportedModules = scopeVisitor.scope.getLocalImportedModules(line, col, this.name);
        for (IToken tok : localImportedModules) {
        	if(tok.getRepresentation().equals(rep)){
                Tuple3<IModule, String, IToken> o = nature.getAstManager().findOnImportedMods(new IToken[]{tok}, nature, rep, this.getName());
                if(o != null && o.o1 instanceof SourceModule){
                    ICompletionState copy = state.getCopy();
                    copy.setActivationToken(o.o2);
                    
                    findDefinitionsFromModAndTok(nature, toRet, null, (SourceModule) o.o1, copy);
                }
                if(toRet.size() > 0){
                	return (Definition[]) toRet.toArray(new Definition[0]);
                }
        	}
        }
        	
        
        //ok, not assign nor import, let's check if it is some self (we do not check for only 'self' because that would map to a
        //local (which has already been covered).
        if (rep.startsWith("self.")){
        	//ok, it is some self, now, that is only valid if we are in some class definition
        	ClassDef classDef = scopeVisitor.scope.getClassDef();
        	if(classDef != null){
        		//ok, we are in a class, so, let's get the self completions
        		String classRep = NodeUtils.getRepresentationString(classDef);
				IToken[] globalTokens = getGlobalTokens(
        				new CompletionState(line, col, classRep, nature,""), 
        				nature.getAstManager());
				
        		String withoutSelf = rep.substring(5);
        		for (IToken token : globalTokens) {
					if(token.getRepresentation().equals(withoutSelf)){
						String parentPackage = token.getParentPackage();
						IModule module = nature.getAstManager().getModule(parentPackage, nature, true);
                        
						if(token instanceof SourceToken && (module != null || this.name.equals(parentPackage))){
                            if(module == null){
                                module = this;
                            }
                            
			                SimpleNode ast2 = ((SourceToken)token).getAst();
							Tuple<Integer, Integer> def = getLineColForDefinition(ast2);
							FastStack<SimpleNode> stack = new FastStack<SimpleNode>();
							stack.add(classDef);
							ILocalScope scope = new LocalScope(stack);
							return new Definition[]{new Definition(def.o1, def.o2, token.getRepresentation(), ast2, scope, module)};
                            
						}else{
							return new Definition[0];
						}
					}
				}
        	}
        }
        
        	
    	//ok, it is not an assign, so, let's search the global tokens (and imports)
        String tok = rep;
        SourceModule mod = this;

        Tuple3<IModule, String, IToken> o = nature.getAstManager().findOnImportedMods(nature, rep, this);
        
        if(o != null && o.o1 instanceof SourceModule){
            mod =  (SourceModule) o.o1;
            tok = o.o2;
            
        }else if(o != null && o.o1 instanceof CompiledModule){
            //ok, we have to check the compiled module
            tok = o.o2;
            if (tok == null || tok.length() == 0 ){
                return new Definition[]{new Definition(1,1,"",null,null,o.o1)};
            }else{
                return (Definition[]) o.o1.findDefinition(state.getCopyWithActTok(tok), 0, 0, nature, lFindInfo);
            }
        }
        
        //mod == this if we are now checking the globals (or maybe not)...heheheh
        ICompletionState copy = state.getCopy();
        copy.setActivationToken(tok);
        try{
        	state.checkFindDefinitionMemory(mod, tok);
        	findDefinitionsFromModAndTok(nature, toRet, visitor.moduleImported, mod, copy);
        }catch(CompletionRecursionException e){
        	//ignore (will return what we've got so far)
        }
            
        return toRet.toArray(new Definition[0]);
    }

    /**
     * Finds the definitions for some module and a token from that module
     */
	private void findDefinitionsFromModAndTok(IPythonNature nature, ArrayList<Definition> toRet, String moduleImported, SourceModule mod, ICompletionState state) {
        String tok = state.getActivationToken();
		if(tok != null){
        	if(tok.length() > 0){
	            Definition d = mod.findGlobalTokDef(state.getCopyWithActTok(tok), nature);
				if(d != null){
	                toRet.add(d);
	                
	            }else if(moduleImported != null){
	            	//if it was found as some import (and is already stored as a dotted name), we must check for
	            	//multiple representations in the absolute form:
	            	//as a relative import
	            	//as absolute import
	            	getModuleDefinition(nature, toRet, mod, moduleImported);
	            }

        	}else{
        		//we found it, but it is an empty tok (which means that what we found is the actual module).
        		toRet.add(new Definition(1,1,"",null,null,mod)); 
        	}
        }
	}

	private IDefinition getModuleDefinition(IPythonNature nature, ArrayList<Definition> toRet, SourceModule mod, String moduleImported) {
		String rel = AbstractToken.makeRelative(mod.getName(), moduleImported);
		IModule modFound = nature.getAstManager().getModule(rel, nature, false);
		if(modFound == null){
			modFound = nature.getAstManager().getModule(moduleImported, nature, false);
		}
		if(modFound != null){
			//ok, found it
			Definition definition = new Definition(1,1,"", null, null, modFound);
			if(toRet != null){
				toRet.add(definition);
			}
			return definition;
		}
		return null;
	}


    /**
     * @param tok
     * @param nature 
     * @return
     */
    public Definition findGlobalTokDef(ICompletionState state, IPythonNature nature) {
        String tok = state.getActivationToken();
    	String[] headAndTail = FullRepIterable.headAndTail(tok);
    	String firstPart = headAndTail[0];
    	String rep = headAndTail[1];
    	
    	IToken[] tokens = null;
    	if(nature != null){
    		tokens = nature.getAstManager().getCompletionsForModule(this, state.getCopyWithActTok(firstPart), true);
    	}else{
    		tokens = getGlobalTokens();
    	}
        for (IToken token : tokens) {
            boolean sameRep = token.getRepresentation().equals(rep);
            if(sameRep){
                if(token instanceof SourceToken){
                	//ok, we found it
                    SimpleNode a = ((SourceToken)token).getAst();
                    Tuple<Integer, Integer> def = getLineColForDefinition(a);
                    
                    String parentPackage = token.getParentPackage();
                    IModule module = this;
                    if(nature != null){
                    	IModule mod = nature.getAstManager().getModule(parentPackage, nature, true);
                    	if(mod != null){
                    		module = mod;
                    	}
                    }
                    
                    
                    if(module instanceof SourceModule){
                        //this is just to get its scope...
                        SourceModule m = (SourceModule) module;
                        FindScopeVisitor scopeVisitor = new FindScopeVisitor(a.beginLine, a.beginColumn);
                        if (m.ast != null){
                            try {
                                m.ast.accept(scopeVisitor);
                            } catch (Exception e) {
                                PydevPlugin.log(e);
                            }
                        }
                        return new Definition(def.o1, def.o2, rep, a, scopeVisitor.scope, module);
                    }else{
                        //line, col
                        return new Definition(def.o1, def.o2, rep, a, new LocalScope(new FastStack<SimpleNode>()), module);
                    }
                }else if(token instanceof ConcreteToken){
                	//a contrete token represents a module
                	String modName = token.getParentPackage();
                	if(modName.length() > 0){
                		modName+= ".";
                	}
                	modName += token.getRepresentation();
                	IModule module = nature.getAstManager().getModule(modName, nature, true);
                	if(module == null){
                		return null;
                	}else{
                		return new Definition(0+1, 0+1, "", null, null, module); // it is the module itself
                	}
                	
                }else if(token instanceof CompiledToken){
                    String parentPackage = token.getParentPackage();
                    FullRepIterable iterable = new FullRepIterable(parentPackage, true);
                    
                    IModule module = null;
                    for(String modName: iterable){
                        module = nature.getAstManager().getModule(modName, nature, true);
                        if(module != null){
                            break;
                        }
                    }
                    if(module == null){
                        return null;
                    }
                    
                    int length = module.getName().length();
                    String finalRep = "";
                    if(parentPackage.length() > length){
                        finalRep = parentPackage.substring(length + 1)+'.';
                    }
                    finalRep += token.getRepresentation();
                    
                    try {
                        IDefinition[] definitions = module.findDefinition(state.getCopyWithActTok(finalRep), -1, -1, nature, new ArrayList<FindInfo>());
                        if(definitions.length > 0){
                            return (Definition) definitions[0];
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }else{
                	throw new RuntimeException("Unexpected token:"+token.getClass());
                }
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
	        
	        return scopeVisitor.scope.getLocalTokens(line, col, false);
        } catch (Exception e) {
            e.printStackTrace();
            return new IToken[0];
        }
    }

    public ILocalScope getLocalScope(int line, int col) {
        try {
            FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col);
            if (ast != null){
                ast.accept(scopeVisitor);
            }
            
            return scopeVisitor.scope;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * @return if the file we have is the same file in the cache.
     */
    public boolean isSynched() {
        if(this.file == null && TESTING){
            return true; //when testing we can have a source module without a file
        }
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

	public void setName(String n) {
		this.name = n;
	}
}