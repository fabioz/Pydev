/*
 * Created on Sep 17, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class GeneralTests extends TestCase{
    
    public void testIt() {
        String string = "from scbr import ddd #tetet";
        System.out.println(string.replaceAll("#.*", ""));
    }
    public static void main(String[] args) {
        junit.textui.TestRunner.run(GeneralTests.class);
    }
}
