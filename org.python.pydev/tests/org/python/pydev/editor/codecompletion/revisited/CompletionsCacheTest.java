/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;
import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;

/**
 * @author Fabio Zadrozny
 */
public class CompletionsCacheTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CompletionsCacheTest.class);
    }

    public void testRebuild(){
        if(true){
	        ASTManager cache = new ASTManager();
	        
	        cache.changePythonPath("C:\\bin\\Python23\\lib\\", null, new NullProgressMonitor());
	        IToken[] completions = null;
	        System.out.println("import 0 ----------------------");
	        ModulesKey[] imports = cache.getAllModules();
	        for (int i = 0; i < imports.length; i++) {
                
	            if(imports[i].name.startsWith("xml"))
	                System.out.println(imports[i].name);
            }
	
//	        System.out.println("import 1 ----------------------");
//	        completions = cache.getCompletionsForImport("");
//	        for (int i = 0; i < completions.length; i++) {
//                System.out.println(completions[i]);
//            }
//	       
//	        System.out.println("import 2 ----------------------");
//	        completions = cache.getCompletionsForImport("xml");
//	        for (int i = 0; i < completions.length; i++) {
//                System.out.println(completions[i]);
//            }
//	
//	        System.out.println("import 3 ----------------------");
//	        completions = cache.getCompletionsForImport("xml.dom.");
//	        for (int i = 0; i < completions.length; i++) {
//                System.out.println(completions[i]);
//            }
	        
	        System.out.println(" token ----------------------");
	        Document doc = getTestDoc();

            completions = cache.getCompletionsForToken(doc,1,10,"","");
	        for (int i = 0; i < completions.length; i++) {
                System.out.println(completions[i]);
            }

	        System.out.println(" token 2 ----------------------");
	        completions = cache.getCompletionsForToken(doc,1,10,"xml","");
	        for (int i = 0; i < completions.length; i++) {
                System.out.println(completions[i]);
            }
	        
	        System.out.println(" token 3 ----------------------");
	        completions = cache.getCompletionsForToken(doc,1,10,"xml.dom","");
	        assertEquals(0, completions.length);
	        
	        System.out.println(" token 4 ----------------------");
	        doc = getTestDoc2();
	        completions = cache.getCompletionsForToken(doc,1,10,"xml.dom","");
	        for (int i = 0; i < completions.length; i++) {
                System.out.println(completions[i]);
            }
	        
        }        
    }

    /**
     * @return
     */
    private Document getTestDoc() {
        String str = "";
        str += "import xml\n";
        str += "from xml.dom import *\n";
        str += "print \n";
        str += "\n";
        Document doc = new Document(str);
        return doc;
    }
    
    /**
     * @return
     */
    private Document getTestDoc2() {
        String str = "";
        str += "import xml.dom \n";
        str += "print \n";
        str += "\n";
        Document doc = new Document(str);
        return doc;
    }
    
//    public void testRebuild2(){
//        IASTManager cache = new ASTManager();
//        
//        cache.rebuildModules("x:\\scbr15\\source\\python\\", new PrintProgressMonitor());
//        
//        
//        cache.saveASTManager(new File("./temp.testcache"), new PrintProgressMonitor());
//        cache = ASTManagerFactory.restoreASTManager(new File("./temp.testcache"), new PrintProgressMonitor(), null);
//        
//        int col = 0;
//        int line = 0;
//        String activationToken = "";
//        String qualifier = "";
//        IToken[] tokens = cache.getCompletionsForToken(new File("x:\\scbr15\\source\\python\\scbr\\app\\manager\\__init__.py"), line, col, activationToken, qualifier);
//        for (int i = 0; i < tokens.length; i++) {
//            System.out.println(tokens[i]);
//        }
//        
//        
//    }
}
