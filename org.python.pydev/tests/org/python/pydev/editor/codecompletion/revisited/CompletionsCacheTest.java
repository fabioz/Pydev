/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;
import java.io.File;

import junit.framework.TestCase;

import org.python.pydev.utils.PrintProgressMonitor;

/**
 * @author Fabio Zadrozny
 */
public class CompletionsCacheTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CompletionsCacheTest.class);
    }

    public void testRebuild(){
        if(false){
	        ASTManager cache = new ASTManager();
	        
	        cache.rebuildModules("C:\\bin\\Python23\\lib\\", new PrintProgressMonitor());
	        
	        String[] imports = cache.getAllModules();
	        for (int i = 0; i < imports.length; i++) {
                
	            if(imports[i].startsWith("xml"))
	                System.out.println(imports[i]);
            }
	
	        System.out.println("----------------------");
	        IToken[] completions = cache.getCompletionsForImport("");
	        for (int i = 0; i < completions.length; i++) {
                System.out.println(completions[i]);
            }
	       
	        System.out.println("----------------------");
	        completions = cache.getCompletionsForImport("xml");
	        for (int i = 0; i < completions.length; i++) {
                System.out.println(completions[i]);
            }
	
	        System.out.println("----------------------");
	        completions = cache.getCompletionsForImport("xml.dom.");
	        for (int i = 0; i < completions.length; i++) {
                System.out.println(completions[i]);
            }
        }        
    }
    
    public void testRebuild2(){
        ASTManager cache = new ASTManager();
        
//        cache.rebuildModules("x:\\scbr15\\source\\python\\", new PrintProgressMonitor());
//        
//        
//        cache.saveASTManager(new File("./temp.testcache"), new PrintProgressMonitor());
        cache = ASTManager.restoreASTManager(new File("./temp.testcache"), new PrintProgressMonitor(), null);
        
        int col = 0;
        int line = 0;
        String activationToken = "";
        String qualifier = "";
        IToken[] tokens = cache.getCompletionsForToken(new File("x:\\scbr15\\source\\python\\scbr\\app\\manager\\__init__.py"), line, col, activationToken, qualifier);
        for (int i = 0; i < tokens.length; i++) {
            System.out.println(tokens[i]);
        }
        
        
    }
}
