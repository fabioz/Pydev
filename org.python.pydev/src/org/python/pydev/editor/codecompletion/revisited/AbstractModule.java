/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;

import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractModule implements Serializable{

    public abstract IToken[] getWildImportedModules();
    public abstract IToken[] getTokenImportedModules();
    
    /**
     * This function should get all that is present in the file as global tokens.
     * Note that imports should not be treated by this function (imports have their own functions).
     * 
     * @return
     */
    public abstract IToken[] getGlobalTokens();
    public abstract String getDocString();
    
    protected String name;
    
    protected AbstractModule(String name){
        this.name = name;
    }
    
    /**
     * This method creates a source module from a file.
     * 
     * @param f
     * @return
     */
    public static AbstractModule createModule(String name, File f) {
        if(f.getAbsolutePath().endsWith(".py")){
	        try {
	            FileInputStream stream = new FileInputStream(f);
	            try {
	                int i = stream.available();
	                byte[] b = new byte[i];
	                stream.read(b);
	
	                Object[] obj = PyParser.reparseDocument(new Document(new String(b)), false);
	                SimpleNode n = (SimpleNode) obj[0];
	                return new SourceModule(name, f, n);
	
	            } finally {
	                stream.close();
	            }
	        } catch (Exception e) {
	            PydevPlugin.log(e);
	        }
        }else{ //this should be a compiled extension... we have to get completions from the python shell.
            return new CompiledModule(name);
        }
        
        //otherwise, source module does not have an ast.
        return new SourceModule(name, f, null);
    }

    
    /**
     * Creates a source file generated only from an ast.
     * @param n
     * @return
     */
    public static AbstractModule createModule(SimpleNode n) {
        return new SourceModule(null, null, n);
    }

}
