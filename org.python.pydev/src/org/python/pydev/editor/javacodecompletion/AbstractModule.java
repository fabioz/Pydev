/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractModule implements Serializable{

    public abstract List getWildImportedModules();
    public abstract List getTokenImportedModules();
    public abstract List getGlobalTokens();
    public abstract String getDocString();
    
    /**
     * This method creates a source module from a file.
     * 
     * @param f
     * @return
     */
    public static AbstractModule createModule(File f, PythonPathHelper pythonPathHelper) {
        if(f.getAbsolutePath().endsWith(".py")){
	        try {
	            FileInputStream stream = new FileInputStream(f);
	            try {
	                int i = stream.available();
	                byte[] b = new byte[i];
	                stream.read(b);
	
	                Object[] obj = PyParser.reparseDocument(new Document(new String(b)), false);
	                SimpleNode n = (SimpleNode) obj[0];
	                return new SourceModule(f, n);
	
	            } finally {
	                stream.close();
	            }
	        } catch (Exception e) {
	            PydevPlugin.log(e);
	        }
        }else{ //this should be a compiled extension... we have to get completions from the python shell.
            String moduleName = pythonPathHelper.resolveModule(f.getAbsolutePath());
            return new CompiledModule(moduleName);
        }
        
        //otherwise, source module does not have an ast.
        return new SourceModule(f, null);
    }

    
    /**
     * Creates a source file generated only from an ast.
     * @param n
     * @return
     */
    public static AbstractModule createModule(SimpleNode n) {
        return new SourceModule(null, n);
    }

}
