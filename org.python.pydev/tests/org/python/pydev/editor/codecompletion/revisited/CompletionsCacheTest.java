/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class CompletionsCacheTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CompletionsCacheTest.class);
    }

    public void testRebuild(){
        ASTManager cache = new ASTManager();
        
        cache.rebuildModules("C:\\bin\\Python23\\lib\\", new NullProgressMonitor());
        
        Collection imports = cache.getAllModules();
        for (Iterator iter = imports.iterator(); iter.hasNext();) {
            String e = (String) iter.next();
            
            if(e.startsWith("xml"))
                System.out.println(e);
        }

        System.out.println("----------------------");
        imports = cache.getCompletionForImport("");
        for (Iterator iter = imports.iterator(); iter.hasNext();) {
            System.out.println(((Object[])iter.next())[0]);
        }
       
        System.out.println("----------------------");
        imports = cache.getCompletionForImport("xml");
        for (Iterator iter = imports.iterator(); iter.hasNext();) {
            System.out.println(((Object[])iter.next())[0]);
        }

        System.out.println("----------------------");
        imports = cache.getCompletionForImport("xml.dom.");
        for (Iterator iter = imports.iterator(); iter.hasNext();) {
            System.out.println(((Object[])iter.next())[0]);
        }
    }
}
