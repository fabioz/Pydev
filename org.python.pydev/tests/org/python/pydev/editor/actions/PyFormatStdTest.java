/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.python.pydev.editor.actions.PyFormatStd.FormatStd;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatStdTest extends TestCase {

    private PyFormatStd format;
    private FormatStd std;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyFormatStdTest.class);
    }

    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        format = new PyFormatStd();
        std = new PyFormatStd.FormatStd();
    }
    
    public void testFormatComma(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = ""+
"def a(  a,b  ):\n"+
"    pass   \n";
        
        String s1 = ""+
"def a(a, b):\n"+
"    pass   \n";
        
        assertEquals(s1, PyFormatStd.formatStr(s, std));
    
        std.spaceAfterComma = false;

        String s2 = ""+
"def a(a,b):\n"+
"    pass   \n";
        
        assertEquals(s2, PyFormatStd.formatStr(s, std));
    
    }


    public void testFormatPar(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = ""+
"def a():\n"+
"    pass   \n";
        
        String s1 = ""+
"def a( ):\n"+
"    pass   \n";
        
        assertEquals(s1, PyFormatStd.formatStr(s, std));
    }


    public void testFormatComma2(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = ""+
"def a( a,   b):\n"+
"    pass   \n";
        
        String s1 = ""+
"def a(a, b):\n"+
"    pass   \n";
        
        assertEquals(s1, PyFormatStd.formatStr(s, std));
    
        std.spaceAfterComma = false;

        String s2 = ""+
"def a(a,b):\n"+
"    pass   \n";
        
        assertEquals(s2, PyFormatStd.formatStr(s, std));
    }

    

    public void testFormatCommaParams(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = ""+
"def a(a,   b):\n"+
"    pass   \n";
        
        String s1 = ""+
"def a( a, b ):\n"+
"    pass   \n";
        
        assertEquals(s1, PyFormatStd.formatStr(s, std));
    
        std.spaceAfterComma = false;

        String s2 = ""+
"def a( a,b ):\n"+
"    pass   \n";
        
        assertEquals(s2, PyFormatStd.formatStr(s, std));
    }

    public void testFormatInnerParams(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = ""+
"def a(a,   b):\n"+
"    return ( (a+b) + ( a+b ) )   \n";
        
        String s1 = ""+
"def a(a, b):\n"+
"    return ((a+b) + (a+b))   \n";
        
        assertEquals(s1, PyFormatStd.formatStr(s, std));
    

        std.parametersWithSpace = true;
        String s2 = ""+
"def a( a, b ):\n"+
"    return ( ( a+b ) + ( a+b ) )   \n";
        
        assertEquals(s2, PyFormatStd.formatStr(s, std));
    }

    public void testFormatInnerParams2(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = ""+
"def a(a,   b):\n"+
"    return ( callA() + callB(b+b) )   \n";
        
        String s1 = ""+
"def a( a, b ):\n"+
"    return ( callA( ) + callB( b+b ) )   \n";
        
        assertEquals(s1, PyFormatStd.formatStr(s, std));
    }

    
    public void testFormatNotInsideStrings(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = ""+
"a = ''' test()\n"+
"nothing changes() ((aa) )\n"+
"'''";
        
        assertEquals(s, PyFormatStd.formatStr(s, std));

        s = ""+
"a = ''' test()\n"+
"nothing changes() ((aa) )\n"+
"";
        
        assertEquals(s, PyFormatStd.formatStr(s, std));

        s = ""+
"a = ' test()'\n"+
"'nothing changes() ((aa) )'\n"+
"";
        
        assertEquals(s, PyFormatStd.formatStr(s, std));

        s = ""+
"a = ' test()'\n"+
"'nothing changes() ((aa) )\n"+
"";
        
        assertEquals(s, PyFormatStd.formatStr(s, std));
    }

    
    public void testFormatNotInsideComments(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = ""+
"#a = ''' test()\n"+
"#nothing changes() ((aa) )\n"+
"#'''";
        
        assertEquals(s, PyFormatStd.formatStr(s, std));
    }

    public void testFormatNotInsideComments2(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "methodname.split( '(' )";
        
        assertEquals(s, PyFormatStd.formatStr(s, std));
    }

    public void testFormatNotInsideComments3(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "methodname.split( #'(' \n" +
        		" )";
        
        String formatStr = PyFormatStd.formatStr(s, std);
        System.out.println(formatStr);
        assertEquals(s, formatStr);
    }

    
    

}
