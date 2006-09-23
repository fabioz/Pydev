/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractModule implements IModule {

	/**
	 * May be changed for tests
	 */
    public static String MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "";

	/** 
     * @see org.python.pydev.core.IModule#getWildImportedModules()
     */
    public abstract IToken[] getWildImportedModules();
    
    /** 
     * @see org.python.pydev.core.IModule#getFile()
     */
    public abstract File getFile();
    
    /** 
     * @see org.python.pydev.core.IModule#getTokenImportedModules()
     */
    public abstract IToken[] getTokenImportedModules();
    
    /** 
     * @see org.python.pydev.core.IModule#getGlobalTokens()
     */
    public abstract IToken[] getGlobalTokens();
    
    /** 
     * @see org.python.pydev.core.IModule#getLocalTokens(int, int)
     */
    public IToken[] getLocalTokens(int line, int col){
        return new IToken[0];
    }

    /**
     * Checks if it is in the global tokens that were created in this module
     * @param tok the token we are looking for
     * @param nature the nature
     * @return true if it was found and false otherwise
     */
    protected boolean isInDirectGlobalTokens(String tok, IPythonNature nature){
    	IToken[] tokens = getGlobalTokens();
    	
    	for (int i = 0; i < tokens.length; i++) {
    		if(tokens[i].getRepresentation().equals(tok)){
    			return true;
    		}
    	}
    	return false;
    	
    }

    /** 
     * @see org.python.pydev.core.IModule#isInGlobalTokens(java.lang.String, org.python.pydev.plugin.nature.PythonNature)
     */
    public boolean isInGlobalTokens(String tok, IPythonNature nature){
    	return isInGlobalTokens(tok, nature, true);
    }
    
    /** 
     * @see org.python.pydev.core.IModule#isInGlobalTokens(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean)
     */
    public boolean isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods){
        return isInGlobalTokens(tok, nature, true, false);
    }
    
    public boolean isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods, boolean ifHasGetAttributeConsiderInTokens){
        //it's just worth checking it if it is not dotted...
        if(tok.indexOf(".") == -1){
        	if(isInDirectGlobalTokens(tok, nature)){
        		return true;
        	}
        }
        
    	//if still not found, we have to get all the tokens, including regular and wild imports
        ICompletionState state = CompletionState.getEmptyCompletionState(nature);
        ICodeCompletionASTManager astManager = nature.getAstManager();
        String[] headAndTail = FullRepIterable.headAndTail(tok);
        state.setActivationToken (headAndTail[0]);
        String head = headAndTail[1];
        IToken[] globalTokens = astManager.getCompletionsForModule(this, state, searchSameLevelMods);
        for (IToken token : globalTokens) {
            String rep = token.getRepresentation();
            
            if(ifHasGetAttributeConsiderInTokens && 
                    //getattribute
                    (rep.equals("__getattribute__") || rep.equals("__getattr__")) && 
                    //but not defined in the builtins (it must be overriden)
                   token.getParentPackage().startsWith("__builtin__") == false){
                return true;
            }
            
            if(rep.equals(head)){
                return true;
            }
        }
        //if not found until now, it is not defined
        return false;
    }
    
    
    /**
     * The token we're looking for must be the state activation token
     */
    public abstract Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature, List<FindInfo> findInfo) throws Exception;

    /** 
     * @see org.python.pydev.core.IModule#getGlobalTokens(org.python.pydev.editor.codecompletion.revisited.CompletionState, org.python.pydev.core.ICodeCompletionASTManager)
     */
    public abstract IToken[] getGlobalTokens(ICompletionState state, ICodeCompletionASTManager manager);
    
    /** 
     * @see org.python.pydev.core.IModule#getDocString()
     */
    public abstract String getDocString();
    
    
    /**
     * Name of the module
     */
    protected String name;
   
    /** 
     * @see org.python.pydev.core.IModule#getName()
     */
    public String getName(){
        return name;
    }
    
    /**
     * Constructor
     * 
     * @param name - name of the module
     */
    protected AbstractModule(String name){
        this.name = name;
    }
    
    /**
     * This method creates a source module from a file.
     * 
     * @param f
     * @return
     * @throws FileNotFoundException
     */
    public static AbstractModule createModule(String name, File f, IPythonNature nature, int currLine) throws FileNotFoundException {
        String path = REF.getFileAbsolutePath(f);
        if(PythonPathHelper.isValidFileMod(path)){
	        if(PythonPathHelper.isValidSourceFile(path)){
                return createModuleFromDoc(name, f, REF.getDocFromFile(f), nature, currLine);
	
	        }else{ //this should be a compiled extension... we have to get completions from the python shell.
	            return new CompiledModule(name, nature.getAstManager());
	        }
        }
        
        //if we are here, return null...
        return null;
    }

    
    
    /** 
     * This function creates the module given that you have a document (that will be parsed)
     */
    public static AbstractModule createModuleFromDoc(String name, File f, IDocument doc, IPythonNature nature, int currLine) {
        //for doc, we are only interested in python files.
        
        if(f != null){
	        String absolutePath = REF.getFileAbsolutePath(f);
	        if(PythonPathHelper.isValidSourceFile(absolutePath)){
                Tuple<SimpleNode, Throwable> obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, nature, currLine));
		        SimpleNode n = obj.o1;
		        return new SourceModule(name, f, n);
	        }
        } else {
            Tuple<SimpleNode, Throwable> obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, nature, currLine));
	        SimpleNode n = obj.o1;
	        return new SourceModule(name, f, n);
        }
        return null;
    }
    
    /**
     * This function creates a module and resolves the module name (use this function if only the file is available).
     */
	public static IModule createModuleFromDoc(File file, IDocument doc, IPythonNature pythonNature, int line, IModulesManager projModulesManager) {
		String moduleName = MODULE_NAME_WHEN_FILE_IS_UNDEFINED;
	    if(file != null){
			moduleName = projModulesManager.resolveModule(REF.getFileAbsolutePath(file));
	    }
		IModule module = createModuleFromDoc(moduleName, file, doc, pythonNature, line);
	    return module;
	}


    /**
     * Creates a source file generated only from an ast.
     * @param n the ast root
     * @return the module
     */
    public static IModule createModule(SimpleNode n) {
        return new SourceModule(null, null, n);
    }
    
    /**
     * Creates a source file generated only from an ast.
     * 
     * @param n the ast root
     * @param file the module file
     * @param moduleName the name of the module
     * 
     * @return the module
     */
    public static IModule createModule(SimpleNode n, File file, String moduleName) {
        return new SourceModule(moduleName, file, n);
    }
    
    /**
     * Creates a source file generated only from an ast (also discovers the module name).
     * 
     * @param n the ast root
     * @param file the module file
     * @param projModulesManager this is the manager (used to resolve the name of the module).
     * 
     * @return the module
     */
    public static IModule createModule(SimpleNode n, File file, IModulesManager projModulesManager) {
		String moduleName = MODULE_NAME_WHEN_FILE_IS_UNDEFINED;
	    if(file != null){
			moduleName = projModulesManager.resolveModule(REF.getFileAbsolutePath(file));
	    }
    	return createModule(n, file, moduleName);
    }
    
    /**
     * @param m
     * @param f
     * @return
     */
    public static AbstractModule createEmptyModule(String m, File f) {
        return new EmptyModule(m, f);
    }

    /** 
     * @see org.python.pydev.core.IModule#getLocalImportedModules(int, int)
     */
    public List <IToken> getLocalImportedModules(int line, int col) {
        return new ArrayList<IToken>();
    }

    /** 
     * @see org.python.pydev.core.IModule#toString()
     */
    @Override
    public String toString() {
    	String n2 = this.getClass().getName();
    	String n = n2.substring(n2.lastIndexOf('.')+1);
		return this.getName()+" ("+n+")";
    }

}
