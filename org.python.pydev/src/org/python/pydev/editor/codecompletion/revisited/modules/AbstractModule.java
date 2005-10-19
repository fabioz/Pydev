/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractModule {

    /**
     * @return tokens for the wild imports.
     */
    public abstract IToken[] getWildImportedModules();
    
    /**
     * @return tokens for the imports in the format from xxx import yyy
     * or import xxx 
     */
    public abstract IToken[] getTokenImportedModules();
    
    /**
     * This function should get all that is present in the file as global tokens.
     * Note that imports should not be treated by this function (imports have their own functions).
     * 
     * @return
     */
    public abstract IToken[] getGlobalTokens();
    
    /**
     * This function returns the local completions 
     * @param line
     * @param col
     * @return
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
    protected boolean isInDirectGlobalTokens(String tok, PythonNature nature){
    	IToken[] tokens = getGlobalTokens();
    	
    	for (int i = 0; i < tokens.length; i++) {
    		if(tokens[i].getRepresentation().equals(tok)){
    			return true;
    		}
    	}
    	return false;
    	
    }
    /**
     * @param tok the token we are looking for
     * @return whether the passed token is part of the global tokens of this module (including imported tokens).
     */
    public boolean isInGlobalTokens(String tok, PythonNature nature){
    	if(isInDirectGlobalTokens(tok, nature)){
    		return true;
    	}
    	//if still not found, we have to get all the tokens, including regular and wild imports
        CompletionState state = CompletionState.getEmptyCompletionState(nature);
        ICodeCompletionASTManager astManager = nature.getAstManager();
        IToken[] globalTokens = astManager.getCompletionsForModule(this, state);
        for (IToken token : globalTokens) {
            if(token.getRepresentation().equals(tok)){
                return true;
            }
        }
        //if not found until now, it is not defined
        return false;
    }
    
    /**
     * This function can be called to find possible definitions of a token, based on its name, line and
     * column.
     * 
     * @param token name
     * @param line 
     * @param col
     * @return array of definitions.
     * @throws Exception
     */
    public abstract Definition[] findDefinition(String token, int line, int col, PythonNature nature) throws Exception;

    /**
     * This function should return all tokens that are global for a given token.
     * E.g. if we have a class declared in the module, we return all tokens that are 'global'
     * for the class (methods and attributes).
     * 
     * @param token
     * @param manager
     * @return
     */
    public abstract IToken[] getGlobalTokens(CompletionState state, ICodeCompletionASTManager manager);
    
    /**
     * 
     * @return the docstring for a module.
     */
    public abstract String getDocString();
    
    
    /**
     * Name of the module
     */
    protected String name;
   
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
    public static AbstractModule createModule(String name, File f, PythonNature nature, int currLine) throws FileNotFoundException {
        String path = REF.getFileAbsolutePath(f);
        if(PythonPathHelper.isValidFileMod(path)){
	        if(isValidSourceFile(path)){
	            FileInputStream stream = new FileInputStream(f);
	            
	            InputStreamReader in = null;
                
	            try {
	                //I wish we had an IFile here, but as that's not possible...
	                //This is way too decoupled from the workbench itself so that we
	                //can have this kind of thing... 
	                String encoding = PythonPathHelper.getPythonFileEncoding(f);
	                
	                if(encoding != null){
	                    in = new InputStreamReader(stream, encoding);
	                }else{
	                    in = new InputStreamReader(stream);
	                }
                } catch (UnsupportedEncodingException e) {
                    PydevPlugin.log(e);
                    in = new InputStreamReader(stream);
                }
                
                BufferedReader reader = new BufferedReader(in);
                StringBuffer buffer = new StringBuffer();
	            try{
	                String line = "";
	                while( (line = reader.readLine() ) != null){
	                    buffer.append(line);
	                    buffer.append('\n');
	                }

	            }catch (Exception e2) {
                    PydevPlugin.log(e2);
                }finally{
	                try {reader.close();} catch (IOException e1) {}
	            }
	            
                Document doc = new Document(buffer.toString());
                return createModuleFromDoc(name, f, doc, nature, currLine);
	
	        }else{ //this should be a compiled extension... we have to get completions from the python shell.
	            return new CompiledModule(name, nature.getAstManager());
	        }
        }
        
        //if we are here, return null...
        return null;
    }

    
    /**
     * @param path
     * @return
     */
    private static boolean isValidSourceFile(String path) {
        return path.endsWith(".py") || path.endsWith(".pyw");
    }
    
    /**
     * @param name
     * @param f
     * @param doc
     * @return
     */
    public static AbstractModule createModuleFromDoc(String name, File f, IDocument doc, PythonNature nature, int currLine) {
        //for doc, we are only interested in python files.
        
        if(f != null){
	        String absolutePath = REF.getFileAbsolutePath(f);
	        if(isValidSourceFile(absolutePath)){
		        Object[] obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, nature, currLine));
		        SimpleNode n = (SimpleNode) obj[0];
		        return new SourceModule(name, f, n);
	        }
        } else {
	        Object[] obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, nature, currLine));
	        SimpleNode n = (SimpleNode) obj[0];
	        return new SourceModule(name, f, n);
        }
        return null;
    }

    /**
     * Creates a source file generated only from an ast.
     * @param n the ast root
     * @return the module
     */
    public static AbstractModule createModule(SimpleNode n) {
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
    public static AbstractModule createModule(SimpleNode n, File file, String moduleName) {
        return new SourceModule(moduleName, file, n);
    }
    /**
     * @param m
     * @param f
     * @return
     */
    public static AbstractModule createEmptyModule(String m, File f) {
        return new EmptyModule(m, f);
    }

    public List <IToken> getLocalImportedModules(int line, int col) {
        return new ArrayList<IToken>();
    }

}
