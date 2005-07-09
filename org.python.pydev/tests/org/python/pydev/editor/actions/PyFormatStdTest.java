/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.python.pydev.editor.actions.PyFormatStd.FormatStd;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatStdTest extends TestCase {

    private FormatStd std;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyFormatStdTest.class);
        
//        try {
//	        PyFormatStdTest n = new PyFormatStdTest();
//            n.setUp();
//            n.testFormatInnerParams2();
//            n.tearDown();
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
        
    }

    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
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
"def a():\n"+
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
"    return ( callA() + callB( b+b ) )   \n";
        
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

    public void testFormatNotInsideComments5(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = ""+
"''' test()\n"+
"nothing 'changes() ((aa) )\n"+
"'''\n" +
"thisChanges(a+b + (a+b))";
        
        String s2 = ""+
"''' test()\n"+
"nothing 'changes() ((aa) )\n"+
"'''\n" +
"thisChanges( a+b + ( a+b ) )";
        
        assertEquals(s2, PyFormatStd.formatStr(s, std));
        
        //unfinished comment
        s = ""+
"''' test()\n"+
"nothing 'changes() ((aa) )\n"+
"''\n" +
"thisDoesNotChange()";
        
        assertEquals(s, PyFormatStd.formatStr(s, std));

        //unfinished comment at end of string
        s = ""+
"''' test()\n"+
"nothing 'changes() ((aa) )\n"+
"''";
        
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
        assertEquals(s, formatStr);
    }

    public void testFormatNotInsideStrings2(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "r = re.compile( \"(?P<latitude>\\d*\\.\\d*)\" )";

        
        String formatStr = PyFormatStd.formatStr(s, std);
        assertEquals(s, formatStr);
    }

    public void testFormatNotLinesOnlyWithParentesis(){
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "" +
		"methodCall( a, \n"+
		"            b \n"+
		"           ) ";

        
        String formatStr = PyFormatStd.formatStr(s, std);
        assertEquals(s, formatStr);
    }

    
    

}
