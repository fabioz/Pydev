/*
 * Created on Sep 17, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.util.Iterator;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * This is not really a unit-test, still, it is here for debugging purposes.
 * @author Fabio Zadrozny
 */
public class GeneralTests extends TestCase{
    
    public void testIt() {
        String string = "from scbr import ddd #tetet";
//        System.out.println(string.replaceAll("#.*", ""));
    }
    
    /**
     * 
     */
    public void testSyst() {
        Properties properties = System.getProperties();
        for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
            Object key  = iter.next();
//            System.out.println(key+" = "+properties.get(key));
            
        }
//        String property = System.getProperty("os");
//        System.out.println(property);

    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(GeneralTests.class);
    }
}
